package com.qiwenshare.file.vo;

/**
 * 配额信息响应。
 *
 * @param totalQuota     总配额（字节）
 * @param usedSize       已用空间（字节）
 * @param availableQuota 可用空间（字节）
 */
public record QuotaInfoVO(
        Long totalQuota,
        Long usedSize,
        Long availableQuota
) {}
