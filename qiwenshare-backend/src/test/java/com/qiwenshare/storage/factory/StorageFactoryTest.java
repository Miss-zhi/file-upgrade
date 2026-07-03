package com.qiwenshare.storage.factory;

import com.qiwenshare.storage.config.StorageProperties;
import com.qiwenshare.storage.interfaces.StorageBackend;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * StorageFactory unit tests.
 */
@ExtendWith(MockitoExtension.class)
class StorageFactoryTest {

    private StorageFactory factory;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setType("local");
    }

    private StorageBackend createMockBackend(String type) {
        StorageBackend backend = mock(StorageBackend.class);
        when(backend.getStorageType()).thenReturn(type);
        return backend;
    }

    @Nested
    @DisplayName("getBackend() - active backend")
    class GetActiveBackend {

        @Test
        @DisplayName("returns the active backend matching storage.type")
        void returnsActiveBackend() {
            StorageBackend localBackend = createMockBackend("local");
            StorageBackend minioBackend = createMockBackend("minio");
            factory = new StorageFactory(List.of(localBackend, minioBackend), properties);

            StorageBackend result = factory.getBackend();

            assertSame(localBackend, result);
        }

        @Test
        @DisplayName("throws IllegalStateException when active type not registered")
        void throwsWhenTypeNotRegistered() {
            StorageBackend localBackend = createMockBackend("local");
            properties.setType("nonexistent");
            factory = new StorageFactory(List.of(localBackend), properties);

            assertThrows(IllegalStateException.class, () -> factory.getBackend());
        }
    }

    @Nested
    @DisplayName("getBackend(type) - named backend")
    class GetNamedBackend {

        @Test
        @DisplayName("returns backend by type")
        void returnsBackendByType() {
            StorageBackend localBackend = createMockBackend("local");
            StorageBackend minioBackend = createMockBackend("minio");
            factory = new StorageFactory(List.of(localBackend, minioBackend), properties);

            assertSame(minioBackend, factory.getBackend("minio"));
        }

        @Test
        @DisplayName("throws IllegalStateException for unknown type")
        void throwsForUnknownType() {
            StorageBackend localBackend = createMockBackend("local");
            factory = new StorageFactory(List.of(localBackend), properties);

            assertThrows(IllegalStateException.class, () -> factory.getBackend("unknown"));
        }
    }

    @Nested
    @DisplayName("getActiveType")
    class GetActiveType {

        @Test
        @DisplayName("returns configured storage type")
        void returnsConfiguredType() {
            StorageBackend localBackend = createMockBackend("local");
            factory = new StorageFactory(List.of(localBackend), properties);

            assertEquals("local", factory.getActiveType());
        }
    }

    @Nested
    @DisplayName("getBackendForStorageType(storageType) - file-level routing")
    class GetBackendForStorageType {

        @Test
        @DisplayName("returns active backend when storageType is null")
        void returnsActiveWhenNull() {
            StorageBackend localBackend = createMockBackend("local");
            StorageBackend minioBackend = createMockBackend("minio");
            factory = new StorageFactory(List.of(localBackend, minioBackend), properties);

            assertSame(localBackend, factory.getBackendForStorageType(null));
        }

        @Test
        @DisplayName("returns matching backend when storageType is registered")
        void returnsMatchingBackend() {
            StorageBackend localBackend = createMockBackend("local");
            StorageBackend minioBackend = createMockBackend("minio");
            factory = new StorageFactory(List.of(localBackend, minioBackend), properties);

            assertSame(minioBackend, factory.getBackendForStorageType("minio"));
        }

        @Test
        @DisplayName("falls back to active backend when storageType not registered")
        void fallsBackToActive() {
            StorageBackend localBackend = createMockBackend("local");
            StorageBackend minioBackend = createMockBackend("minio");
            factory = new StorageFactory(List.of(localBackend, minioBackend), properties);

            // "qiniu" not registered, should fallback to active "local"
            assertSame(localBackend, factory.getBackendForStorageType("qiniu"));
        }
    }
}
