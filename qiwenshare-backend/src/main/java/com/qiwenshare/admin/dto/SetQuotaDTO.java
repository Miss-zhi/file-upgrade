package com.qiwenshare.admin.dto;

import jakarta.validation.constraints.Min;

/**
 * 设置用户配额请求体。
 *
 * @param totalQuota 新配额值（字节）
 */
public record SetQuotaDTO(
        @Min(value = 1, message = "配额值必须大于 0") long totalQuota
) {
}
