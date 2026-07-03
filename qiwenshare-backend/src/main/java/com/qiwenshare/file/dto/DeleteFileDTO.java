package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 删除文件请求体（软删除）。
 *
 * @param userFileId 文件 ID
 */
public record DeleteFileDTO(
        @NotNull Long userFileId
) {}
