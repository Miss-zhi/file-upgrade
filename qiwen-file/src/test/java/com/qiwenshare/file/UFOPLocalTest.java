package com.qiwenshare.file;

import com.qiwenshare.ufop.UFOPFactory;
import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.operation.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UFOPLocalTest {

    @Autowired
    private UFOPFactory ufopFactory;

    @Autowired
    private UFOPConfigProperties config;

    private static final String TEST_DIR = "/test-ufop/";
    private String testPath;

    @BeforeEach
    void setUp() throws IOException {
        testPath = TEST_DIR + UUID.randomUUID().toString().substring(0, 8) + ".txt";
        Files.createDirectories(Paths.get(config.getRootPath(), TEST_DIR));
    }

    @Test
    @DisplayName("写入并读取文件")
    void testWriteAndRead() {
        String content = "Hello UFOP!";
        ufopFactory.getWriter().write(testPath, content);

        String result = ufopFactory.getReader().read(testPath);
        assertEquals(content, result);
    }

    @Test
    @DisplayName("上传（InputStream）并下载")
    void testUploadAndDownload() throws IOException {
        String content = "Upload test content";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        ufopFactory.getUploader().upload(testPath, input);

        InputStream downloaded = ufopFactory.getDownloader().download(testPath);
        byte[] bytes = downloaded.readAllBytes();
        assertEquals(content, new String(bytes));
    }

    @Test
    @DisplayName("删除文件")
    void testDelete() {
        ufopFactory.getWriter().write(testPath, "to delete");
        assertTrue(Files.exists(Paths.get(config.getRootPath(), testPath)));

        ufopFactory.getDeleter().delete(testPath);
        assertFalse(Files.exists(Paths.get(config.getRootPath(), testPath)));
    }

    @Test
    @DisplayName("重命名文件")
    void testRename() {
        String newPath = testPath.replace(".txt", "-renamed.txt");
        ufopFactory.getWriter().write(testPath, "rename me");

        ufopFactory.getRenamer().rename(testPath, newPath);
        assertFalse(Files.exists(Paths.get(config.getRootPath(), testPath)));
        assertTrue(Files.exists(Paths.get(config.getRootPath(), newPath)));

        ufopFactory.getDeleter().delete(newPath);
    }

    @Test
    @DisplayName("复制文件")
    void testCopy() {
        String copyPath = testPath.replace(".txt", "-copy.txt");
        ufopFactory.getWriter().write(testPath, "copy me");

        ufopFactory.getCopier().copy(testPath, copyPath);
        assertTrue(Files.exists(Paths.get(config.getRootPath(), testPath)));
        assertTrue(Files.exists(Paths.get(config.getRootPath(), copyPath)));

        assertEquals(
            ufopFactory.getReader().read(testPath),
            ufopFactory.getReader().read(copyPath)
        );

        ufopFactory.getDeleter().delete(testPath);
        ufopFactory.getDeleter().delete(copyPath);
    }

    @Test
    @DisplayName("下载不存在文件抛异常")
    void testDownloadNotFound() {
        assertThrows(Exception.class, () ->
            ufopFactory.getDownloader().download("/nonexistent/file.txt")
        );
    }
}
