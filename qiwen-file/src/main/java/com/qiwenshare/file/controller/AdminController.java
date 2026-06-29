package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.dto.user.UserQueryDTO;
import com.qiwenshare.file.dto.user.UserUpdateDTO;
import com.qiwenshare.file.service.StatsService;
import com.qiwenshare.file.service.RoleService;
import com.qiwenshare.file.service.PermissionService;
import com.qiwenshare.file.service.SysConfigService;
import com.qiwenshare.file.service.OperationLogService;
import com.qiwenshare.file.domain.log.OperationLog;
import com.qiwenshare.file.domain.user.Role;
import com.qiwenshare.file.vo.user.PermissionVO;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.user.UserAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端控制器
 */
@Tag(name = "管理端", description = "用户管理")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final IUserService userService;
    private final StatsService statsService;
    private final SysConfigService sysConfigService;
    private final OperationLogService logService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    @Operation(summary = "分页搜索用户列表")
    @PostMapping("/user/list")
    public RestResult<Map<String, Object>> listUsers(@RequestBody UserQueryDTO dto) {
        IPage<User> page = userService.listUsers(dto.getPage(), dto.getSize(), dto.getKeyword());
        List<UserAdminVO> records = page.getRecords().stream().map(UserAdminVO::fromEntity).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("size", page.getSize());
        result.put("current", page.getCurrent());
        return RestResult.success(result);
    }

    @Operation(summary = "更新用户")
    @MyLog(module = "管理端", value = "更新用户")
    @PutMapping("/user")
    public RestResult<Void> updateUser(@RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto.getId(), dto.getEmail(), dto.getPhone(), dto.getNickname(), dto.getAvatar());
        return RestResult.success();
    }

    @Operation(summary = "删除用户")
    @MyLog(module = "管理端", value = "删除用户")
    @DeleteMapping("/user/{id}")
    public RestResult<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return RestResult.success();
    }

    @Operation(summary = "切换用户状态")
    @MyLog(module = "管理端", value = "切换用户状态")
    @PutMapping("/user/{id}/status")
    public RestResult<Void> toggleStatus(@PathVariable String id, @RequestParam boolean enabled) {
        userService.toggleStatus(id, enabled);
        return RestResult.success();
    }

    @Operation(summary = "统计信息")
    @GetMapping("/stats")
    public RestResult<Map<String, Object>> getStats() {
        return RestResult.success(statsService.getStats());
    }

    @Operation(summary = "获取系统配置")
    @GetMapping("/config")
    public RestResult<Map<String, String>> getConfig() {
        return RestResult.success(sysConfigService.getAllConfig());
    }

    @Operation(summary = "保存系统配置")
    @PutMapping("/config")
    public RestResult<Void> saveConfig(@RequestBody Map<String, String> config) {
        sysConfigService.saveConfig(config);
        return RestResult.success();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/user/{id}/role")
    public RestResult<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        userService.updateRole(id, body.get("role"));
        return RestResult.success();
    }

    @Operation(summary = "操作日志")
    @GetMapping("/logs")
    public RestResult<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        com.baomidou.mybatisplus.core.metadata.IPage<OperationLog> result =
                logService.page(page, size, operation, startTime, endTime);
        return RestResult.success(Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "pages", result.getPages()
        ));
    }

    // ===== RBAC — 角色管理 =====

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public RestResult<List<Role>> listRoles() {
        return RestResult.success(roleService.listAll());
    }

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    public RestResult<Role> createRole(@RequestBody Role role) {
        return RestResult.success(roleService.create(role));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles/{id}")
    public RestResult<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setRoleId(id);
        return RestResult.success(roleService.update(role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    public RestResult<Void> deleteRole(@PathVariable Long id) {
        roleService.delete(id);
        return RestResult.success();
    }

    // ===== RBAC — 权限树 =====

    @Operation(summary = "权限树")
    @GetMapping("/permissions/tree")
    public RestResult<List<PermissionVO>> getPermissionTree() {
        return RestResult.success(permissionService.getTree());
    }

    // ===== RBAC — 用户角色分配 =====

    @GetMapping("/users/{userId}/roles")
    public RestResult<List<Role>> getUserRoles(@PathVariable String userId) {
        return RestResult.success(roleService.getUserRoles(userId));
    }

    @PutMapping("/users/{userId}/roles")
    public RestResult<Void> assignUserRoles(@PathVariable String userId,
                                            @RequestBody Map<String, List<Long>> body) {
        roleService.assignUserRoles(userId, body.get("roleIds"));
        return RestResult.success();
    }

    // ===== RBAC — 角色权限分配 =====

    @GetMapping("/roles/{roleId}/permissions")
    public RestResult<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        return RestResult.success(roleService.getRolePermissionIds(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public RestResult<Void> assignRolePermissions(@PathVariable Long roleId,
                                                  @RequestBody Map<String, List<Long>> body) {
        roleService.assignRolePermissions(roleId, body.get("permissionIds"));
        return RestResult.success();
    }
}
