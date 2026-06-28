package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询入参（分页+搜索）
 */
@Data
@Schema(description = "用户查询请求")
public class UserQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "搜索关键词（用户名/邮箱）")
    private String keyword;
}
