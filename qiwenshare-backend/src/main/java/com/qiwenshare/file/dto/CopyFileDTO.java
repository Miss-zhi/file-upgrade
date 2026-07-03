package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 复制文件请求体。
 *
 * @param userFileId     文件 ID
 * @param targetFolderId 目标文件夹 ID
 */
public record CopyFileDTO(
        @NotNull Long userFileId,
        Long targetFolderId
) {}
