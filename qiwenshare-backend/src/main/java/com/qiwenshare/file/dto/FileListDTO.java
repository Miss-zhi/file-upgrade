package com.qiwenshare.file.dto;

/**
 * 文件列表查询参数。
 *
 * @param filePath 目录路径（默认 /）
 * @param fileType 文件类型筛选（可选，1-普通文件 2-文件夹）
 * @param page     页码（从 0 开始）
 * @param size     每页大小（默认 20，上限 100）
 * @param order    排序字段（fileName/fileSize/modifyTime）
 * @param sort     排序方向（asc/desc）
 */
public record FileListDTO(
        String filePath,
        Integer fileType,
        Integer page,
        Integer size,
        String order,
        String sort
) {
    public FileListDTO {
        if (filePath == null || filePath.isBlank()) filePath = "/";
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 20;
        if (size > 100) size = 100;
        if (order == null || order.isBlank()) order = "fileName";
        if (sort == null || sort.isBlank()) sort = "asc";
    }
}
