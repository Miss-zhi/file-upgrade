package com.qiwenshare.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户详情响应。
 *
 * @param userId       业务 ID
 * @param username     用户名
 * @param telephone    手机号
 * @param available    账号状态
 * @param registerTime 注册时间
 * @param roles        角色列表
 * @param permissions  权限列表
 */
public record UserDetailVO(
        String userId,
        String username,
        String telephone,
        Integer available,
        LocalDateTime registerTime,
        List<RoleInfo> roles,
        List<String> permissions
) {

    /**
     * 角色信息。
     *
     * @param roleId   角色 ID
     * @param roleName 角色名称
     */
    public record RoleInfo(Integer roleId, String roleName) {
    }
}
