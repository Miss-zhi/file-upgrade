package com.qiwenshare.document.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 文档预览请求。
 *
 * @param userFileId 用户文件 ID
 */
public record PreviewRequestDTO(
        @NotNull(message = "userFileId 不能为空") Long userFileId
) {
}
