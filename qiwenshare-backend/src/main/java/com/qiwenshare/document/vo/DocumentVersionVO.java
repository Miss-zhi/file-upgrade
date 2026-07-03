package com.qiwenshare.document.vo;

import java.time.LocalDateTime;

/**
 * 文档版本信息 VO。
 *
 * @param versionNumber 版本号
 * @param editorId      编辑者用户 ID
 * @param fileSize      文件大小（字节）
 * @param createTime    创建时间
 */
public record DocumentVersionVO(
        int versionNumber,
        Long editorId,
        long fileSize,
        LocalDateTime createTime
) {
}
