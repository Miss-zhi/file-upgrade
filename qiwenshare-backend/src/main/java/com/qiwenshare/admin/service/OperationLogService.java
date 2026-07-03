package com.qiwenshare.admin.service;

import com.qiwenshare.admin.entity.OperationLog;
import com.qiwenshare.admin.repository.OperationLogRepository;
import com.qiwenshare.admin.vo.OperationLogVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志查询服务。
 *
 * <p>提供分页查询和多条件过滤功能。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 分页查询操作日志。
     *
     * @param module    模块名（可选）
     * @param action    操作类型（可选）
     * @param username  操作者用户名（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageable  分页参数
     * @return 分页结果
     */
    public Page<OperationLogVO> listLogs(String module, String action, String username,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Pageable pageable) {
        Specification<OperationLog> spec = buildSpec(module, action, username, startTime, endTime);
        Page<OperationLog> page = operationLogRepository.findAll(spec, pageable);
        return page.map(this::toVO);
    }

    private Specification<OperationLog> buildSpec(String module, String action, String username,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (module != null && !module.isBlank()) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.equal(root.get("username"), username));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private OperationLogVO toVO(OperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getModule(),
                log.getAction(),
                log.getDescription(),
                log.getRequestMethod(),
                log.getRequestUri(),
                log.getRequestParams(),
                log.getResponseCode(),
                log.getErrorMessage(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getExecutionTime(),
                log.getCreateTime()
        );
    }
}
