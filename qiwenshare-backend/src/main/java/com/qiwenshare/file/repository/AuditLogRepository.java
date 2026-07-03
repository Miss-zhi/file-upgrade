package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审计日志 Repository。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
