package com.qiwenshare.file.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建分享请求")
public class CreateShareDTO {

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "过期天数，默认7天")
    private Integer expireDays;

    @Schema(description = "提取码，留空自动生成4位数字")
    private String code;

    @Schema(description = "文件/分享 ID（取消时使用）")
    private String fileId;
}
