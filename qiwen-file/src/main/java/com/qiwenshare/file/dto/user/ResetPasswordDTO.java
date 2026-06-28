package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Schema(name = "重置密码DTO", description = "管理员重置用户密码参数")
public class ResetPasswordDTO {

    @Schema(description = "用户ID", required = true)
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[^\u4e00-\u9fa5]{6,20}$", message = "密码长度6-20位,不允许中文")
    private String password;
}
