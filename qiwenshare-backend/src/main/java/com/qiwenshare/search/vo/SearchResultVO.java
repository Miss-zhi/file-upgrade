package com.qiwenshare.search.vo;

import java.time.LocalDateTime;

/**
 * 搜索结果 VO。
 *
 * @param userFileId        用户文件 ID
 * @param fileName          文件名（可能包含高亮标签）
 * @param extendName        扩展名
 * @param filePath          文件路径
 * @param fileSize          文件大小
 * @param uploadTime        上传时间
 * @param modifyTime        修改时间
 * @param highlightFileName 高亮文件名片段
 */
public record SearchResultVO(
        Long userFileId,
        String fileName,
        String extendName,
        String filePath,
        Long fileSize,
        LocalDateTime uploadTime,
        LocalDateTime modifyTime,
        String highlightFileName
) {
}
