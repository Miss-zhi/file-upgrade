package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建文件请求体。
 *
 * @param fileName 文件名（含扩展名）
 * @param filePath 目标目录路径
 */
public record CreateFileDTO(
        @NotBlank String fileName,
        @NotBlank String filePath
) {}
