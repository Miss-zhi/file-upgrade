package com.qiwenshare.auth.dto;

/**
 * 管理员用户列表查询参数。
 *
 * @param keyword   用户名搜索关键字（可选）
 * @param available 账号状态过滤（可选，1=启用，0=禁用）
 * @param page      页码（从 0 开始）
 * @param pageSize  每页大小
 */
public record UserListQuery(
        String keyword,
        Integer available,
        int page,
        int pageSize
) {
}
