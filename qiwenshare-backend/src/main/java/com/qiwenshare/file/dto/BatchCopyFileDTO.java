package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量复制文件请求体。
 *
 * @param userFileIds    文件 ID 列表
 * @param targetFolderId 目标文件夹 ID
 */
public record BatchCopyFileDTO(
        @NotEmpty List<@NotNull Long> userFileIds,
        Long targetFolderId
) {}
