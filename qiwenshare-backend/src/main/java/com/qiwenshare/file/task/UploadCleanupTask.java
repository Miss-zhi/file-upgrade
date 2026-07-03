package com.qiwenshare.file.task;

import com.qiwenshare.file.entity.UploadTask;
import com.qiwenshare.file.repository.UploadTaskDetailRepository;
import com.qiwenshare.file.repository.UploadTaskRepository;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传任务清理定时任务。
 *
 * <p>清理超时未完成的上传任务（超过 24 小时）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupTask {

    private final UploadTaskRepository uploadTaskRepository;
    private final UploadTaskDetailRepository uploadTaskDetailRepository;
    private final StorageFactory storageFactory;

    /**
     * 每小时执行一次，清理超时的上传任务。
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredUploadTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<UploadTask> expiredTasks = uploadTaskRepository.findByStatusAndCreateTimeBefore(0, threshold);
        log.info("清理超时上传任务: {} 个", expiredTasks.size());

        for (UploadTask task : expiredTasks) {
            try {
                // 清理分片临时文件
                uploadTaskDetailRepository.findByTaskId(task.getTaskId()).forEach(detail -> {
                    if (detail.getStoragePath() != null) {
                        try {
                            storageFactory.getBackend().delete(detail.getStoragePath());
                        } catch (Exception e) {
                            log.warn("清理分片临时文件失败: {}", detail.getStoragePath(), e);
                        }
                    }
                });
                uploadTaskDetailRepository.deleteByTaskId(task.getTaskId());
                uploadTaskRepository.delete(task);
                log.info("已清理超时上传任务: taskId={}", task.getTaskId());
            } catch (Exception e) {
                log.error("清理上传任务失败: taskId={}", task.getTaskId(), e);
            }
        }
    }
}
