package com.qiwenshare.file.vo.file;

import com.qiwenshare.file.domain.file.FileBean;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 文件信息出参
 */
@Data
@Builder
@Schema(description = "文件信息")
public class FileVO {

    @Schema(description = "文件 ID")
    private String id;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "是否为文件夹")
    private Boolean isFolder;

    @Schema(description = "创建时间")
    private String createTime;

    public static FileVO fromEntity(FileBean file) {
        return FileVO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .isFolder(file.getIsFolder())
                .createTime(file.getCreateTime() != null ? file.getCreateTime().toString() : "")
                .build();
    }
}
