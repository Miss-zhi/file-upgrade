package com.qiwenshare.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 搜索请求 DTO。
 *
 * @param keyword  搜索关键词
 * @param page     页码（从 0 开始）
 * @param size     每页大小
 * @param sortBy   排序字段（可选：uploadTime, fileSize）
 * @param sortOrder 排序方向（asc/desc）
 */
public record SearchRequestDTO(
        @NotBlank(message = "搜索关键词不能为空")
        @Size(max = 100, message = "搜索关键词不能超过100个字符")
        String keyword,

        int page,

        Integer size,

        String sortBy,

        String sortOrder
) {
}
