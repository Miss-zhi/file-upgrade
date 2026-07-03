package com.qiwenshare.storage.impl.local;

import com.qiwenshare.storage.config.StorageProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalStorageBackend unit tests using real temp directories.
 */
class LocalStorageBackendTest {

    @TempDir
    Path tempDir;

    private LocalStorageBackend backend;

    @BeforeEach
    void setUp() {
        StorageProperties.Local localProps = new StorageProperties.Local();
        localProps.setBasePath(tempDir.toString());
        backend = new LocalStorageBackend(localProps);
    }

    @Nested
    @DisplayName("getStorageType")
    class GetStorageType {

        @Test
        @DisplayName("returns 'local'")
        void returnsLocal() {
            assertEquals("local", backend.getStorageType());
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivity {

        @Test
        @DisplayName("returns true when basePath exists and is writable")
        void returnsTrueWhenWritable() {
            assertTrue(backend.checkConnectivity());
        }
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file and returns storagePath")
        void uploadsFileSuccessfully() throws Exception {
            byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
            InputStream is = new ByteArrayInputStream(content);

            String result = backend.upload(is, "test/hello.txt", content.length);

            assertEquals("test/hello.txt", result);
            assertTrue(Files.exists(tempDir.resolve("test/hello.txt")));
            assertEquals("hello world", Files.readString(tempDir.resolve("test/hello.txt")));
        }

        @Test
        @DisplayName("overwrites existing file")
        void overwritesExistingFile() throws Exception {
            byte[] content1 = "v1".getBytes(StandardCharsets.UTF_8);
            backend.upload(new ByteArrayInputStream(content1), "file.txt", content1.length);

            byte[] content2 = "v2".getBytes(StandardCharsets.UTF_8);
            backend.upload(new ByteArrayInputStream(content2), "file.txt", content2.length);

            assertEquals("v2", Files.readString(tempDir.resolve("file.txt")));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("downloads file as InputStream")
        void downloadsFileSuccessfully() throws Exception {
            byte[] content = "download me".getBytes(StandardCharsets.UTF_8);
            Files.createDirectories(tempDir.resolve("dl"));
            Files.write(tempDir.resolve("dl/data.bin"), content);

            try (InputStream is = backend.download("dl/data.bin")) {
                byte[] read = is.readAllBytes();
                assertArrayEquals(content, read);
            }
        }

        @Test
        @DisplayName("throws UncheckedIOException for non-existent file")
        void throwsForNonExistentFile() {
            assertThrows(UncheckedIOException.class, () -> backend.download("nonexistent.txt"));
        }
    }

    @Nested
    @DisplayName("downloadRange")
    class DownloadRange {

        @Test
        @DisplayName("returns byte range from file")
        void returnsByteRange() throws Exception {
            byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);
            Files.write(tempDir.resolve("range.txt"), content);

            try (InputStream is = backend.downloadRange("range.txt", 2, 5)) {
                byte[] read = is.readAllBytes();
                assertEquals("2345", new String(read, StandardCharsets.UTF_8));
            }
        }
    }

    @Nested
    @DisplayName("getFileSize")
    class GetFileSize {

        @Test
        @DisplayName("returns correct file size")
        void returnsCorrectSize() throws Exception {
            byte[] content = "12345".getBytes(StandardCharsets.UTF_8);
            Files.write(tempDir.resolve("size.txt"), content);

            assertEquals(5, backend.getFileSize("size.txt"));
        }

        @Test
        @DisplayName("throws UncheckedIOException for non-existent file")
        void throwsForNonExistent() {
            assertThrows(UncheckedIOException.class, () -> backend.getFileSize("no.txt"));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {

        @Test
        @DisplayName("copies file from source to destination")
        void copiesFileSuccessfully() throws Exception {
            byte[] content = "copy me".getBytes(StandardCharsets.UTF_8);
            Files.write(tempDir.resolve("src.txt"), content);

            backend.copy("src.txt", "dest/copied.txt");

            assertTrue(Files.exists(tempDir.resolve("dest/copied.txt")));
            assertEquals("copy me", Files.readString(tempDir.resolve("dest/copied.txt")));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes existing file")
        void deletesExistingFile() throws Exception {
            Files.write(tempDir.resolve("del.txt"), "data".getBytes());
            assertTrue(Files.exists(tempDir.resolve("del.txt")));

            backend.delete("del.txt");

            assertFalse(Files.exists(tempDir.resolve("del.txt")));
        }

        @Test
        @DisplayName("no exception when file does not exist")
        void noExceptionWhenNotExists() {
            assertDoesNotThrow(() -> backend.delete("nonexistent.txt"));
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("returns true for existing file")
        void returnsTrueForExisting() throws Exception {
            Files.write(tempDir.resolve("ex.txt"), "data".getBytes());
            assertTrue(backend.exists("ex.txt"));
        }

        @Test
        @DisplayName("returns false for non-existent file")
        void returnsFalseForNonExistent() {
            assertFalse(backend.exists("nope.txt"));
        }
    }

    @Nested
    @DisplayName("getPreviewUrl")
    class GetPreviewUrl {

        @Test
        @DisplayName("returns null (local does not support preview URL)")
        void returnsNull() {
            assertNull(backend.getPreviewUrl("any/path"));
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("reads file content same as download")
        void readsFileContent() throws Exception {
            byte[] content = "read me".getBytes(StandardCharsets.UTF_8);
            Files.write(tempDir.resolve("read.txt"), content);

            try (InputStream is = backend.read("read.txt")) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("writes data and returns storagePath")
        void writesDataSuccessfully() throws Exception {
            byte[] content = "write me".getBytes(StandardCharsets.UTF_8);
            String result = backend.write("write.txt", new ByteArrayInputStream(content));

            assertEquals("write.txt", result);
            assertEquals("write me", Files.readString(tempDir.resolve("write.txt")));
        }
    }

    @Nested
    @DisplayName("path traversal protection")
    class PathTraversal {

        @Test
        @DisplayName("throws SecurityException for path traversal attack")
        void throwsForPathTraversal() {
            assertThrows(SecurityException.class,
                    () -> backend.upload(new ByteArrayInputStream("x".getBytes()), "../../etc/passwd", 1));
        }

        @Test
        @DisplayName("throws SecurityException for nested path traversal")
        void throwsForNestedTraversal() {
            assertThrows(SecurityException.class,
                    () -> backend.exists("sub/../../../etc/shadow"));
        }
    }
}
