package com.qiwenshare.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 更新角色权限请求体。
 *
 * @param permissionIds 新的权限 ID 列表（全量替换）
 */
public record UpdateRolePermissionsRequest(
        @NotNull @NotEmpty(message = "权限列表不能为空") List<Integer> permissionIds
) {}
