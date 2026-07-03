package com.qiwenshare.document.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 文档编辑请求。
 *
 * @param userFileId 用户文件 ID
 */
public record EditRequestDTO(
        @NotNull(message = "userFileId 不能为空") Long userFileId
) {
}
