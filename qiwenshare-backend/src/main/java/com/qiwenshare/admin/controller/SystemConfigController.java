package com.qiwenshare.admin.controller;

import com.qiwenshare.admin.common.AuditLog;
import com.qiwenshare.admin.dto.CreateConfigDTO;
import com.qiwenshare.admin.dto.UpdateConfigDTO;
import com.qiwenshare.admin.service.SystemConfigService;
import com.qiwenshare.admin.vo.ConfigVO;
import com.qiwenshare.admin.vo.PageResponse;
import com.qiwenshare.auth.common.RestResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数管理 REST 端点。
 *
 * <p>所有端点在 {@code /api/v1/admin/config} 前缀下，需要 {@code admin:config-manage} 权限。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:config-manage')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 分页查询系统参数。
     *
     * @param keyword  搜索关键字（可选）
     * @param page     页码（从 0 开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<RestResult<PageResponse<ConfigVO>>> listConfigs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by("id").ascending());
        Page<ConfigVO> result = systemConfigService.listConfigs(keyword, pageRequest);
        return ResponseEntity.ok(RestResult.success(PageResponse.from(result)));
    }

    /**
     * 新增系统参数。
     *
     * @param dto 创建请求体
     * @return 创建后的参数信息
     */
    @PostMapping
    @AuditLog(module = "config", action = "CREATE", description = "新增系统参数")
    public ResponseEntity<RestResult<ConfigVO>> createConfig(@Valid @RequestBody CreateConfigDTO dto) {
        ConfigVO config = systemConfigService.createConfig(dto);
        return ResponseEntity.ok(RestResult.success(config));
    }

    /**
     * 修改系统参数。
     *
     * @param id  参数 ID
     * @param dto 更新请求体
     * @return 更新后的参数信息
     */
    @PutMapping("/{id}")
    @AuditLog(module = "config", action = "UPDATE", description = "修改系统参数")
    public ResponseEntity<RestResult<ConfigVO>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConfigDTO dto) {
        ConfigVO config = systemConfigService.updateConfig(id, dto);
        return ResponseEntity.ok(RestResult.success(config));
    }

    /**
     * 删除系统参数。
     *
     * @param id 参数 ID
     * @return 删除成功响应
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "config", action = "DELETE", description = "删除系统参数")
    public ResponseEntity<RestResult<Void>> deleteConfig(@PathVariable Long id) {
        systemConfigService.deleteConfig(id);
        return ResponseEntity.ok(RestResult.success("系统参数已删除"));
    }
}
