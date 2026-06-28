package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "用户可用状态更新DTO")
public class UserAvailableDTO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "可用状态")
    private Integer available;
}
