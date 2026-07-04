package com.qiwenshare.file.vo;

import java.time.LocalDateTime;

/**
 * 分享信息响应。
 *
 * @param shareId     分享 ID
 * @param userFileId  关联的用户文件 ID
 * @param shareCode   分享码
 * @param extractCode 提取码（可能为 null）
 * @param expireTime  过期时间（null 表示永久）
 * @param isExpired   是否已过期
 * @param fileName    分享的文件名
 * @param fileSize    分享的文件大小
 * @param viewCount   浏览次数
 * @param createTime  创建时间
 */
public record ShareInfoVO(
        Long shareId,
        Long userFileId,
        String shareCode,
        String extractCode,
        LocalDateTime expireTime,
        Boolean isExpired,
        String fileName,
        Long fileSize,
        Integer viewCount,
        LocalDateTime createTime
) {}
