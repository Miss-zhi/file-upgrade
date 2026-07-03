package com.qiwenshare.admin.common;

import com.qiwenshare.admin.entity.OperationLog;
import com.qiwenshare.admin.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步写入器。
 *
 * <p>独立组件，确保 {@link Async @Async} 代理生效。
 * 避免 AuditLogAspect 自调用导致 @Async 失效的问题。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAsyncWriter {

    private final OperationLogRepository operationLogRepository;

    /**
     * 异步保存操作日志。
     *
     * @param opLog 操作日志实体
     */
    @Async("fileTaskExecutor")
    public void save(OperationLog opLog) {
        try {
            operationLogRepository.save(opLog);
        } catch (Exception e) {
            log.error("审计日志写入失败: module={}, action={}, userId={}",
                    opLog.getModule(), opLog.getAction(), opLog.getUserId(), e);
        }
    }
}
