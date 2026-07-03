package com.qiwenshare.file.vo;

/**
 * 上传文件响应。
 *
 * @param userFileId 用户文件 ID
 * @param fileName   文件名
 * @param fileSize   文件大小
 * @param fileHash   文件 hash
 * @param isSpeed    是否秒传成功
 */
public record UploadFileVO(
        Long userFileId,
        String fileName,
        Long fileSize,
        String fileHash,
        boolean isSpeed
) {}
