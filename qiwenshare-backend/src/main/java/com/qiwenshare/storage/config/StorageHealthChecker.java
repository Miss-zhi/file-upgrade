package com.qiwenshare.storage.config;

import com.qiwenshare.storage.factory.StorageFactory;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 存储后端启动时连通性验证。
 *
 * <p>在应用启动完成后对当前激活的存储后端执行：
 * 写入测试（1KB）→ 读取测试 → 删除测试。
 * 验证失败时阻止应用继续。</p>
 */
@Component
@Slf4j
public class StorageHealthChecker {

    private final StorageFactory storageFactory;

    public StorageHealthChecker(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    /**
     * 应用启动后执行存储后端连通性验证。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        StorageBackend backend = storageFactory.getBackend();
        String type = backend.getStorageType();
        log.info("开始存储后端连通性验证: {}", type);

        String testPath = "_health_check/test.txt";
        String testContent = "storage health check";
        byte[] testBytes = testContent.getBytes(StandardCharsets.UTF_8);

        try {
            // 写入测试
            backend.upload(new ByteArrayInputStream(testBytes), testPath, testBytes.length);
            log.debug("写入测试通过");

            // 读取测试
            InputStream readStream = backend.download(testPath);
            byte[] readBytes = readStream.readAllBytes();
            readStream.close();
            if (!testContent.equals(new String(readBytes, StandardCharsets.UTF_8))) {
                throw new RuntimeException("读取内容校验失败");
            }
            log.debug("读取测试通过");

            // 删除测试
            backend.delete(testPath);
            log.debug("删除测试通过");

            log.info("存储后端 [{}] 连通性验证全部通过", type);
        } catch (Exception e) {
            log.error("存储后端 [{}] 连通性验证失败，应用启动中止", type, e);
            throw new RuntimeException("存储后端连通性验证失败: " + type, e);
        }
    }
}
