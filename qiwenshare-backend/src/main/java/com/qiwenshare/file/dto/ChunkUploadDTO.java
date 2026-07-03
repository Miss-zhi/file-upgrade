package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分片上传请求体（chunkIndex + chunkData 通过 multipart 传递）。
 *
 * @param taskId     上传任务 ID
 * @param chunkIndex 分片序号（从 0 开始）
 */
public record ChunkUploadDTO(
        @NotBlank String taskId,
        @NotNull Integer chunkIndex
) {}
