package com.qiwenshare.file.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件列表请求
 */
@Data
@Schema(description = "文件列表请求")
public class ListFileDTO {

    @NotBlank
    @Schema(description = "目录路径", example = "/")
    private String path;
}
