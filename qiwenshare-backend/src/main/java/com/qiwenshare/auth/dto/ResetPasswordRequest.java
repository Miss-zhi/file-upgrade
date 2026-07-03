package com.qiwenshare.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员重置密码请求 DTO。 */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 30) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "密码需包含大小写字母和数字，长度8-30位") String newPassword
) {}
