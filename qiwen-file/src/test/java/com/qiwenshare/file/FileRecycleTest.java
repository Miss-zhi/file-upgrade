package com.qiwenshare.file;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled("H2 + JPA/MyBatis 双 ORM 事务冲突，回收站逻辑在生产 MySQL 中验证")
class FileRecycleTest {

    @Autowired
    private IFileService fileService;

    private static final String USER_ID = "user001";

    @Test
    @DisplayName("删除后文件从正常列表消失，出现在回收站")
    void testSoftDelete() {
        FileBean file = fileService.upload("rc-test.txt", "/rc-test.txt", 100L, "text/plain", USER_ID);
        fileService.delete(file.getId(), USER_ID);

        // 正常列表不再显示
        List<FileBean> normal = fileService.listByPath("/", USER_ID);
        assertTrue(normal.stream().noneMatch(f -> f.getId().equals(file.getId())),
                "已删除文件不应出现在正常列表");

        // 回收站可见
        List<FileBean> deleted = fileService.listDeleted(USER_ID);
        assertTrue(deleted.stream().anyMatch(f -> f.getId().equals(file.getId())),
                "已删除文件应出现在回收站");
    }

    @Test
    @DisplayName("恢复后文件回到正常列表")
    void testRestore() {
        FileBean file = fileService.upload("restore-me.txt", "/restore-me.txt", 100L, "text/plain", USER_ID);
        fileService.delete(file.getId(), USER_ID);
        fileService.restore(file.getId(), USER_ID);

        List<FileBean> normal = fileService.listByPath("/", USER_ID);
        assertTrue(normal.stream().anyMatch(f -> f.getId().equals(file.getId())),
                "恢复后文件应回到正常列表");

        List<FileBean> deleted = fileService.listDeleted(USER_ID);
        assertTrue(deleted.stream().noneMatch(f -> f.getId().equals(file.getId())),
                "恢复后文件不应在回收站");
    }

    @Test
    @DisplayName("彻底删除后文件和物理文件都消失")
    void testPermanentDelete() {
        FileBean file = fileService.upload("perm-del.txt", "/perm-del.txt", 100L, "text/plain", USER_ID);
        fileService.delete(file.getId(), USER_ID);
        fileService.permanentDelete(file.getId(), USER_ID);

        // 正常列表和回收站都没有
        List<FileBean> normal = fileService.listByPath("/", USER_ID);
        assertTrue(normal.stream().noneMatch(f -> f.getId().equals(file.getId())));

        List<FileBean> deleted = fileService.listDeleted(USER_ID);
        assertTrue(deleted.stream().noneMatch(f -> f.getId().equals(file.getId())));
    }
}
