package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 分片上传初始化请求体。
 *
 * @param fileName    文件名（含扩展名）
 * @param filePath    目标目录路径
 * @param fileSize    文件总大小（字节，允许 0 字节文件）
 * @param fileHash    文件 SHA-256 hash
 * @param totalChunks 总分片数
 */
public record ChunkUploadInitDTO(
        @NotBlank String fileName,
        @NotBlank String filePath,
        @NotNull @PositiveOrZero Long fileSize,
        @NotBlank String fileHash,
        @NotNull @Positive Integer totalChunks
) {}
