package com.qiwenshare.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO。
 */
public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String telephone,
        @NotBlank @Size(min = 8, max = 30) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "密码需包含大小写字母和数字，长度8-30位") String password
) {}
