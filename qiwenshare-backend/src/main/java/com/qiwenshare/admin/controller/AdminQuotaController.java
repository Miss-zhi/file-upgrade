package com.qiwenshare.admin.controller;

import com.qiwenshare.admin.common.AuditLog;
import com.qiwenshare.admin.dto.BatchSetQuotaDTO;
import com.qiwenshare.admin.dto.SetQuotaDTO;
import com.qiwenshare.admin.service.AdminQuotaService;
import com.qiwenshare.admin.vo.AdminQuotaVO;
import com.qiwenshare.auth.common.RestResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员配额管理 REST 端点。
 *
 * <p>所有端点在 {@code /api/v1/admin/quota} 前缀下，需要 {@code admin:quota-manage} 权限。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/quota")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:quota-manage')")
public class AdminQuotaController {

    private final AdminQuotaService adminQuotaService;

    /**
     * 查询用户配额信息。
     *
     * @param userId 用户业务 ID
     * @return 配额信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<RestResult<AdminQuotaVO>> getQuotaInfo(@PathVariable String userId) {
        AdminQuotaVO quotaInfo = adminQuotaService.getQuotaInfo(userId);
        return ResponseEntity.ok(RestResult.success(quotaInfo));
    }

    /**
     * 设置用户配额。
     *
     * @param userId 用户业务 ID
     * @param dto    设置配额请求体
     * @return 操作结果
     */
    @PutMapping("/{userId}")
    @AuditLog(module = "quota", action = "UPDATE", description = "设置用户配额")
    public ResponseEntity<RestResult<Void>> setQuota(
            @PathVariable String userId,
            @Valid @RequestBody SetQuotaDTO dto) {
        adminQuotaService.setQuota(userId, dto.totalQuota());
        return ResponseEntity.ok(RestResult.success("配额设置成功"));
    }

    /**
     * 批量设置用户配额。
     *
     * @param dto 批量设置请求体
     * @return 操作结果（含跳过的 userId 列表）
     */
    @PutMapping("/batch")
    @AuditLog(module = "quota", action = "UPDATE", description = "批量设置用户配额")
    public ResponseEntity<RestResult<List<String>>> batchSetQuota(@Valid @RequestBody BatchSetQuotaDTO dto) {
        List<String> skipped = adminQuotaService.batchSetQuota(dto);
        return ResponseEntity.ok(RestResult.success("配额批量设置完成", skipped));
    }
}
