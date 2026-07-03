package com.qiwenshare.auth.vo;

import java.util.List;

/**
 * 角色信息响应体。
 *
 * @param roleId      角色 ID
 * @param roleName    角色名称
 * @param roleDesc    角色描述
 * @param available   是否可用（1-启用，0-禁用）
 * @param permissions 角色关联的权限 ID 列表
 */
public record RoleResponse(
        Integer roleId,
        String roleName,
        String roleDesc,
        Integer available,
        List<Integer> permissions
) {}
