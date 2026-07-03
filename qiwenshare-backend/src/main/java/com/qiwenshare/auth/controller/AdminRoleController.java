package com.qiwenshare.auth.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.auth.dto.UpdateRolePermissionsRequest;
import com.qiwenshare.auth.event.PermissionChangeEventPublisher;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.vo.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员角色管理 REST 端点。
 *
 * <p>所有端点在 {@code /api/v1/admin/roles} 前缀下，需要 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:role-manage')")
public class AdminRoleController {

    private final AuthService authService;
    private final PermissionChangeEventPublisher publisher;

    /**
     * 获取所有角色列表（含权限信息）。
     *
     * @return 角色列表
     */
    @GetMapping
    public ResponseEntity<RestResult<List<RoleResponse>>> listRoles() {
        List<RoleResponse> roles = authService.listRoles();
        return ResponseEntity.ok(RestResult.success(roles));
    }

    /**
     * 更新角色权限（全量替换）。
     *
     * @param roleId  角色 ID
     * @param request 更新权限请求体
     * @return 更新成功响应
     */
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<RestResult<Void>> updateRolePermissions(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        List<String> affectedUserIds = authService.updateRolePermissions(roleId, request.permissionIds());
        if (!affectedUserIds.isEmpty()) {
            publisher.publishPermissionChanged(affectedUserIds);
        }
        return ResponseEntity.ok(RestResult.success("角色权限已更新"));
    }
}
