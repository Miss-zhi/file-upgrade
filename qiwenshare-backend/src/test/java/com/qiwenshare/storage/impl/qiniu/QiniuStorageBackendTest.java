package com.qiwenshare.storage.impl.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.util.Auth;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.StringMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * QiniuStorageBackend unit tests.
 */
@ExtendWith(MockitoExtension.class)
class QiniuStorageBackendTest {

    @Mock private Auth auth;
    @Mock private UploadManager uploadManager;
    @Mock private BucketManager bucketManager;

    private QiniuStorageBackend backend;

    private static final String BUCKET = "test-bucket";
    private static final String DOMAIN = "http://cdn.example.com";
    private static final long EXPIRE_SECONDS = 3600L;

    @BeforeEach
    void setUp() {
        backend = new QiniuStorageBackend(auth, uploadManager, bucketManager, BUCKET, DOMAIN, EXPIRE_SECONDS);
    }

    @Nested
    @DisplayName("getStorageType")
    class GetStorageType {

        @Test
        @DisplayName("returns 'qiniu'")
        void returnsQiniu() {
            assertEquals("qiniu", backend.getStorageType());
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivity {

        @Test
        @DisplayName("returns true when buckets() succeeds")
        void returnsTrueWhenConnected() throws Exception {
            when(bucketManager.buckets()).thenReturn(new String[]{"test-bucket"});
            assertTrue(backend.checkConnectivity());
        }

        @Test
        @DisplayName("returns false on QiniuException")
        void returnsFalseOnQiniuException() throws Exception {
            when(bucketManager.buckets()).thenThrow(new QiniuException(new IOException("network error")));
            assertFalse(backend.checkConnectivity());
        }
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file and returns storagePath")
        void uploadsSuccessfully() throws Exception {
            when(auth.uploadToken(BUCKET)).thenReturn("fake-token");
            Response okResponse = mock(Response.class);
            when(okResponse.isOK()).thenReturn(true);
            when(uploadManager.put(any(InputStream.class), anyString(), anyString(),
                    isNull(StringMap.class), isNull(String.class))).thenReturn(okResponse);

            byte[] data = "test data".getBytes();
            String result = backend.upload(new ByteArrayInputStream(data), "file.txt", data.length);

            assertEquals("file.txt", result);
            verify(uploadManager).put(any(InputStream.class), eq("file.txt"), eq("fake-token"),
                    isNull(StringMap.class), isNull(String.class));
        }

        @Test
        @DisplayName("throws UncheckedIOException on non-OK response")
        void throwsOnNonOkResponse() throws Exception {
            when(auth.uploadToken(BUCKET)).thenReturn("fake-token");
            Response failResponse = mock(Response.class);
            when(failResponse.isOK()).thenReturn(false);
            // error is a public final field, will be null on mock
            when(uploadManager.put(any(InputStream.class), anyString(), anyString(),
                    isNull(StringMap.class), isNull(String.class))).thenReturn(failResponse);

            assertThrows(UncheckedIOException.class,
                    () -> backend.upload(new ByteArrayInputStream("x".getBytes()), "fail.txt", 1));
        }

        @Test
        @DisplayName("throws UncheckedIOException on QiniuException")
        void throwsOnQiniuException() throws Exception {
            when(auth.uploadToken(BUCKET)).thenReturn("fake-token");
            when(uploadManager.put(any(InputStream.class), anyString(), anyString(),
                    isNull(StringMap.class), isNull(String.class)))
                    .thenThrow(new QiniuException(new IOException("qiniu error")));

            assertThrows(UncheckedIOException.class,
                    () -> backend.upload(new ByteArrayInputStream("x".getBytes()), "fail.txt", 1));
        }
    }

    @Nested
    @DisplayName("getFileSize")
    class GetFileSize {

        @Test
        @DisplayName("returns file size from FileInfo.fsize")
        void returnsSize() throws Exception {
            FileInfo fileInfo = new FileInfo();
            fileInfo.fsize = 4096L;
            when(bucketManager.stat(BUCKET, "file.txt")).thenReturn(fileInfo);

            assertEquals(4096L, backend.getFileSize("file.txt"));
        }

        @Test
        @DisplayName("throws UncheckedIOException on QiniuException")
        void throwsOnQiniuException() throws Exception {
            when(bucketManager.stat(BUCKET, "fail.txt")).thenThrow(new QiniuException(new IOException("error")));

            assertThrows(UncheckedIOException.class, () -> backend.getFileSize("fail.txt"));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        @DisplayName("calls bucketManager.copy with force=true")
        void copiesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.copy("src.txt", "dest.txt"));
            verify(bucketManager).copy(BUCKET, "src.txt", BUCKET, "dest.txt", true);
        }

        @Test
        @DisplayName("throws UncheckedIOException on QiniuException")
        void throwsOnQiniuException() throws Exception {
            doThrow(new QiniuException(new IOException("copy error")))
                    .when(bucketManager).copy(BUCKET, "src.txt", BUCKET, "dest.txt", true);

            assertThrows(UncheckedIOException.class, () -> backend.copy("src.txt", "dest.txt"));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls bucketManager.delete")
        void deletesSuccessfully() throws Exception {
            assertDoesNotThrow(() -> backend.delete("file.txt"));
            verify(bucketManager).delete(BUCKET, "file.txt");
        }

        @Test
        @DisplayName("throws UncheckedIOException on QiniuException")
        void throwsOnQiniuException() throws Exception {
            doThrow(new QiniuException(new IOException("delete error")))
                    .when(bucketManager).delete(BUCKET, "fail.txt");

            assertThrows(UncheckedIOException.class, () -> backend.delete("fail.txt"));
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true when stat succeeds")
        void returnsTrueWhenExists() throws Exception {
            FileInfo fileInfo = new FileInfo();
            fileInfo.fsize = 100L;
            when(bucketManager.stat(BUCKET, "file.txt")).thenReturn(fileInfo);

            assertTrue(backend.exists("file.txt"));
        }

        @Test
        @DisplayName("returns false when code is 612 (file not found)")
        void returnsFalseWhenCode612() throws Exception {
            QiniuException ex = mock(QiniuException.class);
            when(ex.code()).thenReturn(612);
            when(bucketManager.stat(BUCKET, "nonexistent.txt")).thenThrow(ex);

            assertFalse(backend.exists("nonexistent.txt"));
        }

        @Test
        @DisplayName("throws UncheckedIOException on other QiniuException")
        void throwsOnOtherQiniuException() throws Exception {
            QiniuException ex = mock(QiniuException.class);
            when(ex.code()).thenReturn(500);
            when(bucketManager.stat(BUCKET, "fail.txt")).thenThrow(ex);

            assertThrows(UncheckedIOException.class, () -> backend.exists("fail.txt"));
        }
    }

    @Nested
    @DisplayName("getPreviewUrl")
    class GetPreviewUrl {

        @Test
        @DisplayName("returns signed URL from auth.privateDownloadUrl")
        void returnsPreviewUrl() {
            when(auth.privateDownloadUrl(eq(DOMAIN + "/file.txt"), eq(EXPIRE_SECONDS)))
                    .thenReturn("http://cdn.example.com/file.txt?sign=xxx");

            String url = backend.getPreviewUrl("file.txt");

            assertNotNull(url);
            assertTrue(url.contains("cdn.example.com"));
        }

        @Test
        @DisplayName("returns null on exception")
        void returnsNullOnException() {
            when(auth.privateDownloadUrl(anyString(), anyLong()))
                    .thenThrow(new RuntimeException("auth error"));

            assertNull(backend.getPreviewUrl("fail.txt"));
        }
    }

    @Nested
    @DisplayName("getStorageType")
    class ReadWriteDelegation {

        @Test
        @DisplayName("write delegates to upload")
        void writeDelegatesToUpload() throws Exception {
            when(auth.uploadToken(BUCKET)).thenReturn("fake-token");
            Response okResponse = mock(Response.class);
            when(okResponse.isOK()).thenReturn(true);
            when(uploadManager.put(any(InputStream.class), anyString(), anyString(),
                    isNull(StringMap.class), isNull(String.class))).thenReturn(okResponse);

            byte[] data = "write".getBytes();
            String result = backend.write("file.txt", new ByteArrayInputStream(data));

            assertEquals("file.txt", result);
        }
    }
}
