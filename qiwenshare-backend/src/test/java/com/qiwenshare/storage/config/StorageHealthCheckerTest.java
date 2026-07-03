package com.qiwenshare.storage.config;

import com.qiwenshare.storage.factory.StorageFactory;
import com.qiwenshare.storage.interfaces.StorageBackend;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * StorageHealthChecker unit tests.
 */
@ExtendWith(MockitoExtension.class)
class StorageHealthCheckerTest {

    @Mock
    private StorageFactory storageFactory;

    @Mock
    private StorageBackend backend;

    private StorageHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        healthChecker = new StorageHealthChecker(storageFactory);
    }

    @Nested
    @DisplayName("checkOnStartup")
    class CheckOnStartup {

        @Test
        @DisplayName("executes upload, download and delete on active backend")
        void executesAllSteps() throws Exception {
            when(storageFactory.getBackend()).thenReturn(backend);
            when(backend.getStorageType()).thenReturn("local");
            when(backend.upload(any(InputStream.class), anyString(), anyLong())).thenReturn("_health_check/test.txt");
            when(backend.download("_health_check/test.txt"))
                    .thenReturn(new ByteArrayInputStream("storage health check".getBytes()));

            assertDoesNotThrow(() -> healthChecker.checkOnStartup());

            verify(backend).upload(any(InputStream.class), eq("_health_check/test.txt"), anyLong());
            verify(backend).download("_health_check/test.txt");
            verify(backend).delete("_health_check/test.txt");
        }

        @Test
        @DisplayName("throws RuntimeException when upload fails")
        void throwsWhenUploadFails() {
            when(storageFactory.getBackend()).thenReturn(backend);
            when(backend.getStorageType()).thenReturn("minio");
            when(backend.upload(any(InputStream.class), anyString(), anyLong()))
                    .thenThrow(new RuntimeException("upload failed"));

            assertThrows(RuntimeException.class, () -> healthChecker.checkOnStartup());
        }

        @Test
        @DisplayName("throws RuntimeException when downloaded content does not match")
        void throwsWhenContentMismatch() throws Exception {
            when(storageFactory.getBackend()).thenReturn(backend);
            when(backend.getStorageType()).thenReturn("aliyun");
            when(backend.upload(any(InputStream.class), anyString(), anyLong())).thenReturn("_health_check/test.txt");
            when(backend.download("_health_check/test.txt"))
                    .thenReturn(new ByteArrayInputStream("wrong content".getBytes()));

            assertThrows(RuntimeException.class, () -> healthChecker.checkOnStartup());
        }

        @Test
        @DisplayName("throws RuntimeException when download fails")
        void throwsWhenDownloadFails() {
            when(storageFactory.getBackend()).thenReturn(backend);
            when(backend.getStorageType()).thenReturn("qiniu");
            when(backend.upload(any(InputStream.class), anyString(), anyLong())).thenReturn("_health_check/test.txt");
            when(backend.download("_health_check/test.txt"))
                    .thenThrow(new RuntimeException("download failed"));

            assertThrows(RuntimeException.class, () -> healthChecker.checkOnStartup());
        }
    }
}
