package com.qiwenshare.search.listener;

import com.qiwenshare.file.event.FileChangedEvent;
import com.qiwenshare.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 文件变更事件监听器。
 *
 * <p>监听 file 模块发布的 {@link FileChangedEvent}，委托给
 * {@link SearchIndexService} 的 {@code @Async} 方法异步更新 ES 索引。
 * 监听器本身不标注 {@code @Async}，避免双重异步调度浪费线程。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileChangedListener {

    private final SearchIndexService searchIndexService;

    /**
     * 处理文件变更事件。
     *
     * <p>同步分发，由 Service 方法的 {@code @Async("searchIndexExecutor")} 负责异步执行。</p>
     *
     * @param event 文件变更事件
     */
    @EventListener
    public void onFileChanged(FileChangedEvent event) {
        Long userFileId = event.getUserFileId();
        FileChangedEvent.ChangeType changeType = event.getChangeType();

        log.debug("收到文件变更事件: userFileId={}, type={}", userFileId, changeType);

        try {
            switch (changeType) {
                case CREATED -> searchIndexService.indexAsync(userFileId);
                case UPDATED -> searchIndexService.updateAsync(userFileId);
                case DELETED -> searchIndexService.deleteAsync(userFileId);
                default -> log.warn("未知的变更类型: {}", changeType);
            }
        } catch (Exception e) {
            log.warn("处理文件变更事件失败: userFileId={}, type={}, 原因={}", userFileId, changeType, e.getMessage());
        }
    }
}
