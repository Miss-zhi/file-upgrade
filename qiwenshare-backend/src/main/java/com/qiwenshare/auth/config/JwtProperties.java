package com.qiwenshare.auth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

/**
 * JWT 配置属性。
 *
 * <p>从 {@code jwt.*} 配置项绑定。启动时校验签名密钥长度 ≥ 256 bit（32 字节）。</p>
 *
 * @param secret           Base64 编码的签名密钥
 * @param accessTokenTtl   Access token 有效期（秒），默认 900
 * @param refreshTokenTtl  Refresh token 有效期（秒），默认 604800
 * @param clockSkewSeconds 时钟偏移容忍度（秒），默认 30
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtl,
        long refreshTokenTtl,
        int clockSkewSeconds
) {

    public JwtProperties {
        if (accessTokenTtl <= 0) {
            accessTokenTtl = 900L;
        }
        if (refreshTokenTtl <= 0) {
            refreshTokenTtl = 604800L;
        }
        if (clockSkewSeconds <= 0) {
            clockSkewSeconds = 30;
        }
    }

    /**
     * 启动时校验密钥长度。
     */
    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must not be blank");
        }
        byte[] key = Base64.getDecoder().decode(secret);
        if (key.length < 32) {
            throw new IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits)");
        }
    }
}
