package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 恢复文件请求体。
 *
 * @param userFileIds 文件 ID 列表
 */
public record RestoreFileDTO(
        @NotEmpty List<Long> userFileIds
) {}
