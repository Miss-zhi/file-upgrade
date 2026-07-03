package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 保存分享文件请求体。
 *
 * @param shareCode   分享码
 * @param targetNodeId 目标文件夹 userFileId（保存到该目录下）
 */
public record SaveShareFileDTO(
        @NotNull String shareCode,
        Long targetNodeId
) {}
