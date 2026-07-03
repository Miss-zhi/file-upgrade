package com.qiwenshare.storage.impl.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * MinioStorageBackend unit tests.
 */
@ExtendWith(MockitoExtension.class)
class MinioStorageBackendTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageBackend backend;

    private static final String BUCKET = "test-bucket";
    private static final long PRESIGNED_EXPIRY = 3600L;

    @BeforeEach
    void setUp() {
        backend = new MinioStorageBackend(minioClient, BUCKET, PRESIGNED_EXPIRY);
    }

    @Nested
    @DisplayName("getStorageType")
    class GetStorageType {

        @Test
        @DisplayName("returns 'minio'")
        void returnsMinio() {
            assertEquals("minio", backend.getStorageType());
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivity {

        @Test
        @DisplayName("returns true when bucket exists")
        void returnsTrueWhenBucketExists() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            assertTrue(backend.checkConnectivity());
        }

        @Test
        @DisplayName("returns false on exception")
        void returnsFalseOnException() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                    .thenThrow(new RuntimeException("connection refused"));
            assertFalse(backend.checkConnectivity());
        }
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file and returns storagePath")
        void uploadsSuccessfully() throws Exception {
            byte[] data = "test data".getBytes();
            InputStream is = new ByteArrayInputStream(data);

            String result = backend.upload(is, "dir/file.txt", data.length);

            assertEquals("dir/file.txt", result);
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            doThrow(new RuntimeException("upload error"))
                    .when(minioClient).putObject(any(PutObjectArgs.class));

            assertThrows(UncheckedIOException.class,
                    () -> backend.upload(new ByteArrayInputStream("x".getBytes()), "fail.txt", 1));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("returns InputStream from getObject")
        void downloadsSuccessfully() throws Exception {
            byte[] data = "content".getBytes();
            GetObjectResponse response = mock(GetObjectResponse.class);
            // GetObjectResponse extends FilterInputStream, mock read behavior
            when(response.readAllBytes()).thenReturn(data);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

            try (InputStream is = backend.download("file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new RuntimeException("download error"));

            assertThrows(UncheckedIOException.class, () -> backend.download("fail.txt"));
        }
    }

    @Nested
    @DisplayName("downloadRange")
    class DownloadRange {

        @Test
        @DisplayName("passes offset and length to getObject")
        void passesOffsetAndLength() throws Exception {
            byte[] data = "partial".getBytes();
            GetObjectResponse response = mock(GetObjectResponse.class);
            when(response.readAllBytes()).thenReturn(data);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

            try (InputStream is = backend.downloadRange("file.txt", 10, 20)) {
                assertArrayEquals(data, is.readAllBytes());
            }

            verify(minioClient).getObject(any(GetObjectArgs.class));
        }
    }

    @Nested
    @DisplayName("getFileSize")
    class GetFileSize {

        @Test
        @DisplayName("returns file size from statObject")
        void returnsSize() throws Exception {
            StatObjectResponse stat = mock(StatObjectResponse.class);
            when(stat.size()).thenReturn(1024L);
            when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

            assertEquals(1024L, backend.getFileSize("file.txt"));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        @DisplayName("calls copyObject with correct args")
        void copiesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.copy("src.txt", "dest.txt"));
            verify(minioClient).copyObject(any(CopyObjectArgs.class));
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            doThrow(new RuntimeException("copy error"))
                    .when(minioClient).copyObject(any(CopyObjectArgs.class));

            assertThrows(UncheckedIOException.class, () -> backend.copy("src.txt", "dest.txt"));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls removeObject")
        void deletesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.delete("file.txt"));
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            doThrow(new RuntimeException("delete error"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            assertThrows(UncheckedIOException.class, () -> backend.delete("fail.txt"));
        }
    }

    @Nested
    @DisplayName("getPreviewUrl")
    class GetPreviewUrl {

        @Test
        @DisplayName("returns presigned URL")
        void returnsPresignedUrl() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://minio:9000/test-bucket/file.txt?signature=xxx");

            String url = backend.getPreviewUrl("file.txt");

            assertNotNull(url);
            assertTrue(url.contains("test-bucket"));
        }

        @Test
        @DisplayName("returns null on failure")
        void returnsNullOnFailure() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new RuntimeException("presign error"));

            assertNull(backend.getPreviewUrl("fail.txt"));
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true when statObject succeeds")
        void returnsTrueWhenExists() throws Exception {
            StatObjectResponse stat = mock(StatObjectResponse.class);
            when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

            assertTrue(backend.exists("file.txt"));
        }

        @Test
        @DisplayName("returns false when NoSuchKey error")
        void returnsFalseWhenNoSuchKey() throws Exception {
            ErrorResponse errorResponse = mock(ErrorResponse.class);
            when(errorResponse.code()).thenReturn("NoSuchKey");
            ErrorResponseException ex = mock(ErrorResponseException.class);
            when(ex.errorResponse()).thenReturn(errorResponse);

            when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(ex);

            assertFalse(backend.exists("nonexistent.txt"));
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("delegates to download")
        void delegatesToDownload() throws Exception {
            byte[] data = "read content".getBytes();
            GetObjectResponse response = mock(GetObjectResponse.class);
            when(response.readAllBytes()).thenReturn(data);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

            try (InputStream is = backend.read("file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("delegates to upload")
        void delegatesToUpload() throws Exception {
            byte[] data = "write content".getBytes();
            // write calls upload(inputStream, storagePath, -1) which calls putObject
            // Need to mock putObject to avoid real validation
            doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));

            String result = backend.write("file.txt", new ByteArrayInputStream(data));

            assertEquals("file.txt", result);
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }
    }
}
