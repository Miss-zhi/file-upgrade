package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量删除文件请求体（软删除）。
 *
 * @param userFileIds 文件 ID 列表
 */
public record BatchDeleteFileDTO(
        @NotEmpty List<Long> userFileIds
) {}
