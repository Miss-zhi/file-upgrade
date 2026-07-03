package com.qiwenshare.admin.repository;

import com.qiwenshare.admin.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 操作日志数据访问接口。
 *
 * <p>实现 {@link JpaSpecificationExecutor} 支持动态条件查询。</p>
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>,
        JpaSpecificationExecutor<OperationLog> {

    /**
     * 按模块名分页查询。
     *
     * @param module   模块名
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OperationLog> findByModule(String module, Pageable pageable);

    /**
     * 按操作类型分页查询。
     *
     * @param action   操作类型
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OperationLog> findByAction(String action, Pageable pageable);

    /**
     * 按操作者用户名分页查询。
     *
     * @param username 操作者用户名
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OperationLog> findByUsername(String username, Pageable pageable);
}
