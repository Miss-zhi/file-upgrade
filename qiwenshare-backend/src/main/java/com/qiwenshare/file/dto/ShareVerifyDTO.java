package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 提取码验证请求体。
 *
 * @param shareCode   分享码
 * @param extractCode 提取码
 */
public record ShareVerifyDTO(
        @NotBlank String shareCode,
        @NotBlank String extractCode
) {}
