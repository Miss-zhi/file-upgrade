package com.qiwenshare.file.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "管理员端用户Vo")
public class UserVO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "手机号")
    private String telephone;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "可用状态")
    private Integer available;
    @Schema(description = "注册时间")
    private String registerTime;
    @Schema(description = "已用存储")
    private Long storageSize;
    @Schema(description = "总存储")
    private Long totalStorageSize;
}
