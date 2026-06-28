package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "存储空间更新DTO")
public class StorageUpdateDTO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "总存储大小（单位M）")
    private Long totalStorageSize;
}
