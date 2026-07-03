package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建文件夹请求体。
 *
 * @param folderName 文件夹名称
 * @param filePath   父目录路径
 */
public record CreateFoldDTO(
        @NotBlank String folderName,
        @NotBlank String filePath
) {}
