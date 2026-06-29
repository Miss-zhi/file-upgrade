package com.qiwenshare.file;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IFileVersionService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.file.FileVersion;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileVersionTest {

    @Autowired
    private IFileService fileService;

    @Autowired
    private IFileVersionService versionService;

    private static final String USER_ID = "user001";

    @Test
    @DisplayName("首次上传创建版本1")
    void testInitialVersion() {
        FileBean file = fileService.upload("v1.txt", "/v1.txt", 100L, "text/plain", USER_ID);
        List<FileVersion> versions = versionService.listVersions(file.getId());
        assertEquals(1, versions.size());
        assertEquals(1, versions.get(0).getVersion());
    }

    @Test
    @DisplayName("覆盖上传生成版本2")
    void testOverwriteVersion() {
        FileBean file = fileService.upload("same.txt", "/same.txt", 100L, "text/plain", USER_ID);
        fileService.upload("same-new.txt", "/same.txt", 200L, "text/plain", USER_ID);

        List<FileVersion> versions = versionService.listVersions(file.getId());
        assertTrue(versions.size() >= 2);
        assertTrue(versions.stream().anyMatch(v -> v.getVersion() == 1));
        assertTrue(versions.stream().anyMatch(v -> v.getVersion() == 2));
    }

    @Test
    @DisplayName("回滚到指定版本")
    void testRestoreVersion() {
        FileBean file = fileService.upload("rollback.txt", "/rollback.txt", 100L, "text/plain", USER_ID);
        FileVersion v1 = versionService.listVersions(file.getId()).get(0);

        fileService.upload("rollback-new.txt", "/rollback.txt", 200L, "text/plain", USER_ID);

        FileBean restored = versionService.restoreVersion(file.getId(), v1.getId(), USER_ID);
        assertEquals("rollback.txt", restored.getFileName());
        assertEquals(100L, restored.getFileSize());
    }
}
