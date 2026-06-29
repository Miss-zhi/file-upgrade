package com.qiwenshare.file;

import com.qiwenshare.ufop.UFOPFactory;
import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.preview.*;
import com.qiwenshare.ufop.operation.preview.product.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UfopPreviewTest {

    @Autowired
    private List<Previewer> allPreviewers;

    @Autowired
    private UFOPFactory factory;

    @Autowired(required = false)
    private AliyunOSSPreviewer aliyunPreviewer;

    @Autowired(required = false)
    private MinioPreviewer minioPreviewer;

    @Autowired(required = false)
    private FastDFSPreviewer fastDFSPreviewer;

    @Autowired(required = false)
    private QiniuyunKodoPreviewer qiniuPreviewer;

    @Test
    @DisplayName("5 个 Previewer Bean 已注入")
    void testAllPreviewersInjected() {
        assertEquals(5, allPreviewers.size());
    }

    @Test
    @DisplayName("UFOPFactory 返回 Previewer")
    void testFactoryGetPreviewer() {
        assertNotNull(factory.getPreviewer());
    }

    @Test
    @DisplayName("远程 Previewer StorageType 正确")
    void testStorageTypes() {
        assertEquals(StorageType.ALIYUN_OSS, aliyunPreviewer.getStorageType());
        assertEquals(StorageType.MINIO, minioPreviewer.getStorageType());
        assertEquals(StorageType.FAST_DFS, fastDFSPreviewer.getStorageType());
        assertEquals(StorageType.QINIU, qiniuPreviewer.getStorageType());
    }
}
