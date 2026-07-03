package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 重命名请求体。
 *
 * @param userFileId 文件 ID
 * @param newName    新文件名（含扩展名）
 */
public record RenameFileDTO(
        @NotNull Long userFileId,
        @NotBlank String newName
) {}
