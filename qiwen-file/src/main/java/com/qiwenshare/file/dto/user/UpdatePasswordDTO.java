package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Schema(name = "修改密码DTO", description = "用户修改密码参数")
public class UpdatePasswordDTO {

    @Schema(description = "旧密码", required = true)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[^\u4e00-\u9fa5]{6,20}$", message = "密码长度6-20位,不允许中文")
    private String newPassword;
}
