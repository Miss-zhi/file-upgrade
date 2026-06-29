package com.qiwenshare.file;

import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.domain.task.UploadTask;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FiletransferTest {

    @Autowired
    private IFiletransferService transferService;

    private static final String USER_ID = "user001";
    private final String ID = "test-id-" + System.currentTimeMillis();

    @Test
    @DisplayName("上传分片后进度可查询")
    void testUploadChunkAndProgress() {
        String content = "Hello Chunk!";
        transferService.uploadChunk(ID, 0, 2, "test.txt", "/test.txt",
                24, USER_ID, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        UploadTask progress = transferService.getProgress(ID);
        assertNotNull(progress);
        assertEquals(1, progress.getChunkNum());
        assertEquals(2, progress.getTotalChunks());
        assertEquals(0, progress.getUploadStatus());
    }

    @Test
    @DisplayName("合并分片成功")
    void testMergeChunks() {
        // 上传 2 个分片
        transferService.uploadChunk(ID, 0, 2, "merge.txt", "/merge.txt",
                20, USER_ID, new ByteArrayInputStream("AAAA".getBytes(StandardCharsets.UTF_8)));
        transferService.uploadChunk(ID, 1, 2, "merge.txt", "/merge.txt",
                20, USER_ID, new ByteArrayInputStream("BBBB".getBytes(StandardCharsets.UTF_8)));

        assertDoesNotThrow(() -> transferService.mergeChunks(ID, "/merge.txt", USER_ID));
        UploadTask task = transferService.getProgress(ID);
        assertEquals(1, task.getUploadStatus());
    }

    @Test
    @DisplayName("不存在的任务返回 null")
    void testNotFound() {
        assertNull(transferService.getProgress("nonexistent"));
    }
}
