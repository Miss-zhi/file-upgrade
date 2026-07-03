package com.qiwenshare.admin.vo;

/**
 * 管理员配额信息响应体。
 *
 * @param userId         用户业务 ID
 * @param totalQuota     总配额（字节）
 * @param usedQuota      已用空间（字节）
 * @param availableQuota 可用空间（字节）
 */
public record AdminQuotaVO(
        String userId,
        Long totalQuota,
        Long usedQuota,
        Long availableQuota
) {
}
