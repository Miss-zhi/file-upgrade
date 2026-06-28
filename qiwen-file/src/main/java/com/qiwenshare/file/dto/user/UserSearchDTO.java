package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "用户搜索DTO")
public class UserSearchDTO {
    @Schema(description = "页码")
    private Long currentPage;
    @Schema(description = "页大小")
    private Long pageCount;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "手机号")
    private String telephone;
}
