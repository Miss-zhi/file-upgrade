package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 修改文件内容请求体。
 *
 * @param userFileId  用户文件 ID
 * @param fileContent 文件文本内容
 */
public record UpdateFileDTO(
        @NotNull Long userFileId,
        @NotBlank String fileContent
) {}
