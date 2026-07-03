package com.qiwenshare.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 用户登录请求 DTO。 */
public record LoginRequest(
        @NotBlank String telephone,
        @NotBlank String password
) {}
