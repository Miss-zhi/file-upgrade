package com.qiwenshare.auth.controller;

import com.qiwenshare.admin.common.AuditLog;
import com.qiwenshare.admin.vo.PageResponse;
import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.auth.dto.ResetPasswordRequest;
import com.qiwenshare.auth.dto.UserListQuery;
import com.qiwenshare.auth.event.PermissionChangeEventPublisher;
import com.qiwenshare.auth.service.AdminUserService;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.vo.UserDetailVO;
import com.qiwenshare.auth.vo.UserListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理 REST 端点。
 *
 * <p>所有端点在 {@code /api/v1/admin/users} 前缀下，需要 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final AdminUserService adminUserService;
    private final PermissionChangeEventPublisher publisher;

    /**
     * 管理员重置用户密码。
     *
     * @param userId       目标用户 ID
     * @param request      重置密码请求体
     * @param userDetails  当前操作人
     * @return 重置成功响应
     */
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('admin:user-manage')")
    @AuditLog(module = "user", action = "UPDATE", description = "重置用户密码")
    public ResponseEntity<RestResult<Void>> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.resetPassword(userId, request, userDetails.getUsername());
        // 发布权限变更事件，清除目标用户的权限缓存
        publisher.publishPermissionChanged(List.of(userId));
        return ResponseEntity.ok(RestResult.success("密码已重置"));
    }

    /**
     * 分页查询用户列表。
     *
     * @param keyword   用户名搜索关键字（可选）
     * @param available 账号状态过滤（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 分页用户列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('admin:user-manage')")
    public ResponseEntity<RestResult<PageResponse<UserListVO>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer available,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        UserListQuery query = new UserListQuery(keyword, available, page, pageSize);
        Page<UserListVO> result = adminUserService.listUsers(query);
        return ResponseEntity.ok(RestResult.success(PageResponse.from(result)));
    }

    /**
     * 查询用户详情（含角色和权限列表）。
     *
     * @param userId 用户业务 ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin:user-manage')")
    public ResponseEntity<RestResult<UserDetailVO>> getUserDetail(@PathVariable String userId) {
        UserDetailVO detail = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(RestResult.success(detail));
    }

    /**
     * 启用用户。
     *
     * @param userId 用户业务 ID
     * @return 操作结果
     */
    @PutMapping("/{userId}/enable")
    @PreAuthorize("hasAuthority('admin:user-manage')")
    @AuditLog(module = "user", action = "UPDATE", description = "启用用户")
    public ResponseEntity<RestResult<Void>> enableUser(@PathVariable String userId) {
        adminUserService.enableUser(userId);
        return ResponseEntity.ok(RestResult.success("用户已启用"));
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户业务 ID
     * @return 操作结果
     */
    @PutMapping("/{userId}/disable")
    @PreAuthorize("hasAuthority('admin:user-manage')")
    @AuditLog(module = "user", action = "UPDATE", description = "禁用用户")
    public ResponseEntity<RestResult<Void>> disableUser(@PathVariable String userId) {
        adminUserService.disableUser(userId);
        return ResponseEntity.ok(RestResult.success("用户已禁用"));
    }
}
