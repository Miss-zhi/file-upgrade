package com.qiwenshare.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户列表响应项。
 *
 * @param userId       业务 ID
 * @param username     用户名
 * @param telephone    手机号
 * @param available    账号状态（1=启用，0=禁用）
 * @param registerTime 注册时间
 * @param roles        关联角色名称列表
 */
public record UserListVO(
        String userId,
        String username,
        String telephone,
        Integer available,
        LocalDateTime registerTime,
        List<String> roles
) {
}
