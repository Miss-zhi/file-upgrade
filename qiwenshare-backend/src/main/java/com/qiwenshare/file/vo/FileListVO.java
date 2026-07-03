package com.qiwenshare.file.vo;

import java.time.LocalDateTime;

/**
 * 文件列表响应项。
 *
 * @param userFileId   用户文件 ID
 * @param fileName     文件名（含扩展名）
 * @param filePath     目录路径
 * @param fileType     文件类型（1-普通文件 2-文件夹）
 * @param fileSize     文件大小（字节），文件夹为 0
 * @param extendName   扩展名
 * @param uploadTime   上传时间
 * @param modifyTime   修改时间
 * @param deleteStatus 删除状态
 */
public record FileListVO(
        Long userFileId,
        String fileName,
        String filePath,
        Integer fileType,
        Long fileSize,
        String extendName,
        LocalDateTime uploadTime,
        LocalDateTime modifyTime,
        Integer deleteStatus
) {}
