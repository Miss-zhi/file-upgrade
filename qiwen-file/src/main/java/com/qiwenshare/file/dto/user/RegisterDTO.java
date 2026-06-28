package com.qiwenshare.file.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册入参
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    @Schema(description = "用户名", example = "user001")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32个字符")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@qiwenshare.com")
    private String email;
}
