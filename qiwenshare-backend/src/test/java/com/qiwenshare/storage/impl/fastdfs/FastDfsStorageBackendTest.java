package com.qiwenshare.storage.impl.fastdfs;

import com.github.tobato.fastdfs.domain.FileInfo;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.exception.FdfsServerException;
import com.github.tobato.fastdfs.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * FastDfsStorageBackend unit tests.
 */
@ExtendWith(MockitoExtension.class)
class FastDfsStorageBackendTest {

    @Mock
    private FastFileStorageClient storageClient;

    private FastDfsStorageBackend backend;

    private static final String DEFAULT_GROUP = "group1";

    @BeforeEach
    void setUp() {
        backend = new FastDfsStorageBackend(storageClient, DEFAULT_GROUP);
    }

    @Nested
    @DisplayName("getStorageType")
    class GetStorageType {

        @Test
        @DisplayName("returns 'fastdfs'")
        void returnsFastdfs() {
            assertEquals("fastdfs", backend.getStorageType());
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivity {

        @Test
        @DisplayName("returns true when queryFileInfo succeeds")
        void returnsTrueWhenConnected() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(storageClient.queryFileInfo(eq(DEFAULT_GROUP), anyString())).thenReturn(fileInfo);

            assertTrue(backend.checkConnectivity());
        }

        @Test
        @DisplayName("returns true when FdfsServerException (file not found but server reachable)")
        void returnsTrueOnFdfsServerException() {
            when(storageClient.queryFileInfo(eq(DEFAULT_GROUP), anyString()))
                    .thenThrow(FdfsServerException.byCode(2));

            assertTrue(backend.checkConnectivity());
        }

        @Test
        @DisplayName("returns false on other exception")
        void returnsFalseOnOtherException() {
            when(storageClient.queryFileInfo(eq(DEFAULT_GROUP), anyString()))
                    .thenThrow(new RuntimeException("connection refused"));

            assertFalse(backend.checkConnectivity());
        }
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file and returns full path")
        void uploadsSuccessfully() {
            StorePath storePath = new StorePath("group1", "M00/00/01/test.txt");
            when(storageClient.uploadFile(any(InputStream.class), anyLong(), anyString(), isNull(Set.class)))
                    .thenReturn(storePath);

            byte[] data = "test data".getBytes();
            String result = backend.upload(new ByteArrayInputStream(data), "test.txt", data.length);

            assertEquals("group1/M00/00/01/test.txt", result);
        }

        @Test
        @DisplayName("throws UncheckedIOException on IOException")
        void throwsOnIOException() throws Exception {
            // Use a PipedInputStream without connected source to simulate read failure
            java.io.PipedInputStream pis = new java.io.PipedInputStream();
            pis.close(); // closing causes read() to throw IOException

            assertThrows(UncheckedIOException.class,
                    () -> backend.upload(pis, "fail.txt", 1));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("downloads file and returns ByteArrayInputStream")
        void downloadsSuccessfully() throws Exception {
            byte[] data = "download content".getBytes();
            when(storageClient.downloadFile(eq("group1"), eq("M00/00/01/file.txt"), any(DownloadCallback.class)))
                    .thenReturn(data);

            try (InputStream is = backend.download("group1/M00/00/01/file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }

        @Test
        @DisplayName("uses default group when path has no slash")
        void usesDefaultGroup() throws Exception {
            byte[] data = "data".getBytes();
            when(storageClient.downloadFile(eq(DEFAULT_GROUP), eq("file.txt"), any(DownloadCallback.class)))
                    .thenReturn(data);

            try (InputStream is = backend.download("file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }
    }

    @Nested
    @DisplayName("getFileSize")
    class GetFileSize {

        @Test
        @DisplayName("returns file size from queryFileInfo")
        void returnsSize() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(fileInfo.getFileSize()).thenReturn(8192L);
            when(storageClient.queryFileInfo("group1", "M00/00/01/file.txt")).thenReturn(fileInfo);

            assertEquals(8192L, backend.getFileSize("group1/M00/00/01/file.txt"));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        @DisplayName("downloads source and re-uploads to destination")
        void copiesSuccessfully() {
            byte[] data = "copy data".getBytes();
            when(storageClient.downloadFile(eq("group1"), eq("src.txt"), any(DownloadCallback.class)))
                    .thenReturn(data);
            StorePath storePath = new StorePath("group1", "M00/00/01/dest.txt");
            when(storageClient.uploadFile(any(InputStream.class), anyLong(), anyString(), isNull(Set.class)))
                    .thenReturn(storePath);

            assertDoesNotThrow(() -> backend.copy("group1/src.txt", "group1/dest.txt"));

            verify(storageClient).downloadFile(eq("group1"), eq("src.txt"), any(DownloadCallback.class));
            verify(storageClient).uploadFile(any(InputStream.class), anyLong(), anyString(), isNull(Set.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls deleteFile with parsed group and path")
        void deletesSuccessfully() {
            assertDoesNotThrow(() -> backend.delete("group1/M00/00/01/file.txt"));
            verify(storageClient).deleteFile("group1", "M00/00/01/file.txt");
        }

        @Test
        @DisplayName("uses default group when no slash in path")
        void usesDefaultGroup() {
            assertDoesNotThrow(() -> backend.delete("file.txt"));
            verify(storageClient).deleteFile(DEFAULT_GROUP, "file.txt");
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true when queryFileInfo succeeds")
        void returnsTrueWhenExists() {
            FileInfo fileInfo = mock(FileInfo.class);
            when(storageClient.queryFileInfo("group1", "M00/00/01/file.txt")).thenReturn(fileInfo);

            assertTrue(backend.exists("group1/M00/00/01/file.txt"));
        }

        @Test
        @DisplayName("returns false when FdfsServerException")
        void returnsFalseOnFdfsServerException() {
            when(storageClient.queryFileInfo("group1", "M00/00/01/missing.txt"))
                    .thenThrow(FdfsServerException.byCode(2));

            assertFalse(backend.exists("group1/M00/00/01/missing.txt"));
        }
    }

    @Nested
    @DisplayName("getPreviewUrl")
    class GetPreviewUrl {

        @Test
        @DisplayName("returns null (FastDFS does not support preview URL)")
        void returnsNull() {
            assertNull(backend.getPreviewUrl("group1/file.txt"));
        }
    }

    @Nested
    @DisplayName("parseStoragePath")
    class ParseStoragePath {

        @Test
        @DisplayName("splits group/path correctly")
        void splitsGroupAndPath() {
            String[] result = backend.parseStoragePath("group2/M00/01/02/file.jpg");
            assertEquals("group2", result[0]);
            assertEquals("M00/01/02/file.jpg", result[1]);
        }

        @Test
        @DisplayName("uses default group when no slash")
        void usesDefaultGroupWhenNoSlash() {
            String[] result = backend.parseStoragePath("file.txt");
            assertEquals(DEFAULT_GROUP, result[0]);
            assertEquals("file.txt", result[1]);
        }
    }

    @Nested
    @DisplayName("read and write delegation")
    class ReadWriteDelegation {

        @Test
        @DisplayName("read delegates to download")
        void readDelegatesToDownload() throws Exception {
            byte[] data = "read data".getBytes();
            when(storageClient.downloadFile(eq(DEFAULT_GROUP), eq("file.txt"), any(DownloadCallback.class)))
                    .thenReturn(data);

            try (InputStream is = backend.read("file.txt")) {
                assertArrayEquals(data, is.readAllBytes());
            }
        }

        @Test
        @DisplayName("write delegates to upload")
        void writeDelegatesToUpload() {
            StorePath storePath = new StorePath("group1", "M00/00/01/write.txt");
            when(storageClient.uploadFile(any(InputStream.class), anyLong(), anyString(), isNull(Set.class)))
                    .thenReturn(storePath);

            byte[] data = "write data".getBytes();
            String result = backend.write("write.txt", new ByteArrayInputStream(data));

            assertEquals("group1/M00/00/01/write.txt", result);
        }
    }
}
