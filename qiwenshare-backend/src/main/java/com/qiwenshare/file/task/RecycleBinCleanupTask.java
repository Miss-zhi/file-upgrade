package com.qiwenshare.file.task;

import com.qiwenshare.file.service.FileRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 回收站自动清理定时任务。
 *
 * <p>自动清理超过 30 天的回收站文件。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecycleBinCleanupTask {

    private final FileRecoveryService fileRecoveryService;

    /**
     * 每天凌晨 2 点执行，清理超过 30 天的回收站文件。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredRecycleBin() {
        log.info("开始清理过期回收站文件...");
        try {
            fileRecoveryService.cleanupExpiredRecycleBin(30);
            log.info("回收站清理完成");
        } catch (Exception e) {
            log.error("回收站清理失败", e);
        }
    }
}
