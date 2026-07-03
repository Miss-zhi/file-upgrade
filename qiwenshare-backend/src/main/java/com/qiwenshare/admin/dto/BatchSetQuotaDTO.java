package com.qiwenshare.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量设置用户配额请求体。
 *
 * @param items 配额设置列表
 */
public record BatchSetQuotaDTO(
        @NotEmpty @Valid List<QuotaItem> items
) {

    /**
     * 单个用户的配额设置。
     *
     * @param userId     用户业务 ID
     * @param totalQuota 新配额值（字节）
     */
    public record QuotaItem(
            @NotNull String userId,
            @Min(value = 1, message = "配额值必须大于 0") long totalQuota
    ) {
    }
}
