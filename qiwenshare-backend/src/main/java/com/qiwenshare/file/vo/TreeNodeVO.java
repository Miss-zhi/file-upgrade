package com.qiwenshare.file.vo;

import java.util.List;

/**
 * 文件树节点响应。
 *
 * @param userFileId 文件夹 ID
 * @param fileName   文件夹名称
 * @param filePath   文件夹路径
 * @param children   子文件夹列表
 */
public record TreeNodeVO(
        Long userFileId,
        String fileName,
        String filePath,
        List<TreeNodeVO> children
) {}
