package com.qiwenshare.admin.controller;

import com.qiwenshare.admin.service.OperationLogService;
import com.qiwenshare.admin.vo.OperationLogVO;
import com.qiwenshare.admin.vo.PageResponse;
import com.qiwenshare.auth.common.RestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志查询 REST 端点。
 *
 * <p>所有端点在 {@code /api/v1/admin/logs} 前缀下，需要 {@code admin:log-view} 权限。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:log-view')")
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志。
     *
     * @param module    模块名（可选）
     * @param action    操作类型（可选）
     * @param username  操作者用户名（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param page      页码（从 0 开始）
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<RestResult<PageResponse<OperationLogVO>>> listLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by("createTime").descending());
        Page<OperationLogVO> result = operationLogService.listLogs(module, action, username, startTime, endTime, pageRequest);
        return ResponseEntity.ok(RestResult.success(PageResponse.from(result)));
    }
}
