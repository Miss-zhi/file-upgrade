package com.qiwenshare.storage.impl.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * AliyunOssStorageBackend unit tests.
 */
@ExtendWith(MockitoExtension.class)
class AliyunOssStorageBackendTest {

    @Mock
    private OSS ossClient;

    private AliyunOssStorageBackend backend;

    private static final String BUCKET = "test-bucket";
    private static final long PRESIGNED_EXPIRY = 3600L;

    @BeforeEach
    void setUp() {
        backend = new AliyunOssStorageBackend(ossClient, BUCKET, PRESIGNED_EXPIRY);
    }

    @Nested
    @DisplayName("getStorageType")
    class GetStorageType {

        @Test
        @DisplayName("returns 'aliyun'")
        void returnsAliyun() {
            assertEquals("aliyun", backend.getStorageType());
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivity {

        @Test
        @DisplayName("returns true when bucket exists")
        void returnsTrueWhenBucketExists() {
            when(ossClient.doesBucketExist(BUCKET)).thenReturn(true);
            assertTrue(backend.checkConnectivity());
        }

        @Test
        @DisplayName("returns false on exception")
        void returnsFalseOnException() {
            when(ossClient.doesBucketExist(BUCKET)).thenThrow(new RuntimeException("connection error"));
            assertFalse(backend.checkConnectivity());
        }
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file with positive fileSize")
        void uploadsSuccessfully() {
            byte[] data = "test data".getBytes();
            InputStream is = new ByteArrayInputStream(data);

            String result = backend.upload(is, "dir/file.txt", data.length);

            assertEquals("dir/file.txt", result);
            verify(ossClient).putObject(eq(BUCKET), eq("dir/file.txt"), any(InputStream.class), any(ObjectMetadata.class));
        }

        @Test
        @DisplayName("uploads file without content-length when fileSize <= 0")
        void uploadsWithoutContentLength() {
            byte[] data = "test".getBytes();
            String result = backend.upload(new ByteArrayInputStream(data), "file.txt", -1);

            assertEquals("file.txt", result);
            verify(ossClient).putObject(eq(BUCKET), eq("file.txt"), any(InputStream.class), any(ObjectMetadata.class));
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() {
            doThrow(new RuntimeException("upload error"))
                    .when(ossClient).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));

            assertThrows(UncheckedIOException.class,
                    () -> backend.upload(new ByteArrayInputStream("x".getBytes()), "fail.txt", 1));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("returns object content stream")
        void downloadsSuccessfully() throws Exception {
            byte[] data = "content".getBytes();
            OSSObject ossObject = new OSSObject();
            ossObject.setObjectContent(new ByteArrayInputStream(data));
            when(ossClient.getObject(BUCKET, "file.txt")).thenReturn(ossObject);

            try (InputStream is = backend.download("file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() {
            when(ossClient.getObject(BUCKET, "fail.txt")).thenThrow(new RuntimeException("download error"));

            assertThrows(UncheckedIOException.class, () -> backend.download("fail.txt"));
        }
    }

    @Nested
    @DisplayName("downloadRange")
    class DownloadRange {

        @Test
        @DisplayName("returns range content stream")
        void downloadsRangeSuccessfully() throws Exception {
            byte[] data = "partial".getBytes();
            OSSObject ossObject = new OSSObject();
            ossObject.setObjectContent(new ByteArrayInputStream(data));
            when(ossClient.getObject(any(com.aliyun.oss.model.GetObjectRequest.class))).thenReturn(ossObject);

            try (InputStream is = backend.downloadRange("file.txt", 0, 10)) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }
    }

    @Nested
    @DisplayName("getFileSize")
    class GetFileSize {

        @Test
        @DisplayName("returns content length from metadata")
        void returnsSize() throws Exception {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(2048L);
            when(ossClient.getObjectMetadata(BUCKET, "file.txt")).thenReturn(metadata);

            assertEquals(2048L, backend.getFileSize("file.txt"));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        @DisplayName("calls copyObject with correct bucket and paths")
        void copiesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.copy("src.txt", "dest.txt"));
            verify(ossClient).copyObject(BUCKET, "src.txt", BUCKET, "dest.txt");
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            doThrow(new RuntimeException("copy error"))
                    .when(ossClient).copyObject(BUCKET, "src.txt", BUCKET, "dest.txt");

            assertThrows(UncheckedIOException.class, () -> backend.copy("src.txt", "dest.txt"));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls deleteObject with correct bucket and path")
        void deletesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.delete("file.txt"));
            verify(ossClient).deleteObject(BUCKET, "file.txt");
        }

        @Test
        @DisplayName("throws UncheckedIOException on failure")
        void throwsOnFailure() throws Exception {
            doThrow(new RuntimeException("delete error"))
                    .when(ossClient).deleteObject(BUCKET, "fail.txt");

            assertThrows(UncheckedIOException.class, () -> backend.delete("fail.txt"));
        }
    }

    @Nested
    @DisplayName("getPreviewUrl")
    class GetPreviewUrl {

        @Test
        @DisplayName("returns presigned URL string")
        void returnsPresignedUrl() throws Exception {
            URL url = new URL("https://test-bucket.oss.aliyuncs.com/file.txt?signature=xxx");
            when(ossClient.generatePresignedUrl(eq(BUCKET), eq("file.txt"), any())).thenReturn(url);

            String result = backend.getPreviewUrl("file.txt");

            assertNotNull(result);
            assertTrue(result.contains("test-bucket"));
        }

        @Test
        @DisplayName("returns null on failure")
        void returnsNullOnFailure() {
            when(ossClient.generatePresignedUrl(eq(BUCKET), eq("fail.txt"), any()))
                    .thenThrow(new RuntimeException("presign error"));

            assertNull(backend.getPreviewUrl("fail.txt"));
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true when object exists")
        void returnsTrueWhenExists() {
            when(ossClient.doesObjectExist(BUCKET, "file.txt")).thenReturn(true);
            assertTrue(backend.exists("file.txt"));
        }

        @Test
        @DisplayName("returns false when object does not exist")
        void returnsFalseWhenNotExists() {
            when(ossClient.doesObjectExist(BUCKET, "nonexistent.txt")).thenReturn(false);
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
            OSSObject ossObject = new OSSObject();
            ossObject.setObjectContent(new ByteArrayInputStream(data));
            when(ossClient.getObject(BUCKET, "file.txt")).thenReturn(ossObject);

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
        void delegatesToUpload() {
            byte[] data = "write content".getBytes();
            String result = backend.write("file.txt", new ByteArrayInputStream(data));

            assertEquals("file.txt", result);
            verify(ossClient).putObject(eq(BUCKET), eq("file.txt"), any(InputStream.class), any(ObjectMetadata.class));
        }
    }
}
