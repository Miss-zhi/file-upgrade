package com.qiwenshare.file.task;

import com.qiwenshare.file.service.FileShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 过期分享清理定时任务。
 *
 * <p>清理已过期的分享记录。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShareCleanupTask {

    private final FileShareService fileShareService;

    /**
     * 每天凌晨 3 点执行，清理过期分享记录。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredShares() {
        log.info("开始清理过期分享记录...");
        try {
            fileShareService.cleanupExpiredShares();
            log.info("分享记录清理完成");
        } catch (Exception e) {
            log.error("分享记录清理失败", e);
        }
    }
}
