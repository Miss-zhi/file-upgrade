package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.AuditLog;
import com.qiwenshare.file.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务。
 *
 * <p>通过 {@code @Async} 异步写入审计日志到 audit_log 表。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 异步记录审计日志。
     *
     * @param userId     用户 ID
     * @param userFileId 文件 ID
     * @param action     操作类型（download / share_download）
     * @param ipAddress  客户端 IP
     * @param userAgent  客户端 UA
     */
    @Async("fileTaskExecutor")
    public void recordAudit(Long userId, Long userFileId, String action, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUserFileId(userFileId);
            auditLog.setAction(action);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLogRepository.save(auditLog);
            log.debug("审计日志已记录: userId={}, action={}", userId, action);
        } catch (Exception e) {
            log.error("审计日志写入失败: userId={}, action={}", userId, action, e);
        }
    }
}
