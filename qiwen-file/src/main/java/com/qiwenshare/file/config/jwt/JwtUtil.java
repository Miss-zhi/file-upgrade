package com.qiwenshare.file.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具类 — jjwt 0.12.x API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 生成 Token
     */
    public String generateToken(String userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role != null ? role : "USER")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 解析 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.error("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 获取用户 ID
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
