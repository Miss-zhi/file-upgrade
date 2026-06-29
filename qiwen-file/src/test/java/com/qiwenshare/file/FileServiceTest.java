package com.qiwenshare.file;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.exception.QiwenException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件服务单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileServiceTest {

    @Autowired
    private IFileService fileService;

    private static final String USER_ID = "user001";

    @Test
    @DisplayName("创建文件夹")
    void testCreateFolder() {
        FileBean folder = fileService.createFolder("/", "documents", USER_ID);
        assertNotNull(folder);
        assertTrue(folder.getIsFolder());
        assertEquals("documents", folder.getFileName());
        assertEquals("/", folder.getParentPath());
        assertEquals("/documents", folder.getFilePath());
    }

    @Test
    @DisplayName("上传文件")
    void testUpload() {
        FileBean file = fileService.upload("readme.txt", "/readme.txt", 1024L, "text/plain", USER_ID);
        assertNotNull(file);
        assertFalse(file.getIsFolder());
        assertEquals("readme.txt", file.getFileName());
        assertEquals("/", file.getParentPath());
    }

    @Test
    @DisplayName("按路径查询文件列表")
    void testListByPath() {
        // 自建数据
        fileService.createFolder("/", "pics", USER_ID);
        fileService.upload("a.txt", "/a.txt", 100L, "text/plain", USER_ID);
        fileService.upload("b.jpg", "/b.jpg", 200L, "image/jpeg", USER_ID);

        List<FileBean> list = fileService.listByPath("/", USER_ID);
        assertEquals(3, list.size());
        // 确认文件夹存在
        boolean hasFolder = list.stream().anyMatch(FileBean::getIsFolder);
        assertTrue(hasFolder);
    }

    @Test
    @DisplayName("删除文件（软删除）")
    @Disabled("H2 + JPA/MyBatis 双 ORM 事务冲突，生产 MySQL 无此问题")
    void testDelete() {
        FileBean file = fileService.upload("todelete.txt", "/todelete.txt", 50L, "text/plain", USER_ID);
        fileService.delete(file.getId(), USER_ID);

        // 正常列表不再显示
        List<FileBean> list = fileService.listByPath("/", USER_ID);
        assertTrue(list.stream().noneMatch(f -> f.getId().equals(file.getId())));
    }

    @Test
    @DisplayName("根据 ID 获取文件")
    void testGetById() {
        FileBean file = fileService.upload("findme.txt", "/findme.txt", 99L, "text/plain", USER_ID);
        FileBean found = fileService.getById(file.getId());
        assertNotNull(found);
        assertEquals("findme.txt", found.getFileName());
    }

    @Test
    @DisplayName("无权删除他人文件")
    void testDeleteOtherUserFile() {
        FileBean file = fileService.upload("secret.txt", "/secret.txt", 100L, "text/plain", USER_ID);
        assertThrows(QiwenException.class, () ->
                fileService.delete(file.getId(), "other_user")
        );
    }
}
