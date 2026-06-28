package com.qiwenshare.file.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 删除文件请求
 */
@Data
@Schema(description = "删除文件请求")
public class DeleteFileDTO {

    @Schema(description = "文件 ID", example = "1234567890")
    private String id;
}
