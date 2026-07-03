package com.qiwenshare.file.vo;

import java.time.LocalDateTime;

/**
 * 文件详情响应。
 *
 * @param userFileId  用户文件 ID
 * @param fileName    文件名（含扩展名）
 * @param filePath    完整路径
 * @param fileType    文件类型
 * @param fileSize    文件大小（字节）
 * @param extendName  扩展名
 * @param fileHash    文件 hash
 * @param storageType 存储后端类型
 * @param uploadTime  上传时间
 * @param modifyTime  修改时间
 */
public record FileDetailVO(
        Long userFileId,
        String fileName,
        String filePath,
        Integer fileType,
        Long fileSize,
        String extendName,
        String fileHash,
        String storageType,
        LocalDateTime uploadTime,
        LocalDateTime modifyTime
) {}
