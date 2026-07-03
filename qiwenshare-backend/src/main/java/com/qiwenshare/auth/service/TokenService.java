package com.qiwenshare.auth.service;

import com.qiwenshare.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT Token 服务。
 *
 * <p>负责 token 的生成、解析、黑名单管理、全局撤销和 refresh token rotation。
 * 使用 jjwt 0.12.x 新 API（{@code Jwts.builder().signWith(key, Jwts.SIG.HS256)}）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String REVOKE_ALL_PREFIX = "revoke:all:";
    private static final String REVOKE_REFRESH_ALL_PREFIX = "revoke:refresh:all:";

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    private SecretKey signingKey;

    /**
     * 获取签名密钥（懒加载，首次调用时初始化）。
     */
    private SecretKey getSigningKey() {
        if (signingKey == null) {
            byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return signingKey;
    }

    /**
     * 生成 access token。
     *
     * @param userId 用户业务 ID
     * @param roles  角色列表
     * @return JWT 字符串
     */
    public String generateAccessToken(String userId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "access")
                .claim("jti", UUID.randomUUID().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.accessTokenTtl())))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 refresh token 并注册到 Redis。
     *
     * @param userId 用户业务 ID
     * @return JWT 字符串
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .claim("jti", jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.refreshTokenTtl())))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
        registerRefreshToken(jti, userId, jwtProperties.refreshTokenTtl());
        return token;
    }

    /**
     * 解析并验证 JWT。
     *
     * @param token JWT 字符串
     * @return Claims，签名无效或已过期时返回 null
     */
    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .clockSkewSeconds(jwtProperties.clockSkewSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("Token 验证失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 token 加入黑名单。
     *
     * @param jti             token 唯一标识
     * @param remainingSeconds 剩余有效期（秒）
     */
    public void blacklist(String jti, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti, "1",
                    Duration.ofSeconds(remainingSeconds));
        }
    }

    /**
     * 检查 jti 是否在黑名单中。
     *
     * @param jti token 唯一标识
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    /**
     * 全局撤销：记录撤销时间戳，该用户此后签发的 token 全部失效。
     *
     * @param userId 用户业务 ID
     */
    public void revokeAllTokens(String userId) {
        redisTemplate.opsForValue().set(
                REVOKE_ALL_PREFIX + userId,
                String.valueOf(Instant.now().getEpochSecond()),
                Duration.ofSeconds(jwtProperties.refreshTokenTtl()));
    }

    /**
     * 检查 token 的 iat 是否早于全局撤销时间戳。
     *
     * @param userId   用户业务 ID
     * @param tokenIat token 签发时间（Unix 秒）
     * @return 是否已撤销
     */
    public boolean isRevoked(String userId, long tokenIat) {
        String revokeTimestamp = redisTemplate.opsForValue().get(REVOKE_ALL_PREFIX + userId);
        if (revokeTimestamp == null) {
            return false;
        }
        return tokenIat < Long.parseLong(revokeTimestamp);
    }

    /**
     * 注册 refresh token 的 jti 到 Redis。
     *
     * @param jti        token 唯一标识
     * @param userId     用户业务 ID
     * @param ttlSeconds 有效期（秒）
     */
    public void registerRefreshToken(String jti, String userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + jti, userId,
                Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 验证并消费 refresh token 的 jti（rotation 策略）。
     * 若 jti 不在 Redis 中，返回 null（触发重用检测）。
     *
     * @param jti token 唯一标识
     * @return userId，若 jti 不存在则返回 null
     */
    public String consumeRefreshToken(String jti) {
        String key = REFRESH_PREFIX + jti;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return null;
        }
        redisTemplate.delete(key);
        return userId;
    }

    /**
     * 删除该用户所有 refresh token（重用检测触发时调用）。
     * 同时记录全局撤销时间戳。
     *
     * @param userId 用户业务 ID
     */
    public void revokeAllRefreshTokens(String userId) {
        // 记录撤销时间戳
        redisTemplate.opsForValue().set(
                REVOKE_REFRESH_ALL_PREFIX + userId,
                String.valueOf(Instant.now().getEpochSecond()),
                Duration.ofSeconds(jwtProperties.refreshTokenTtl()));

        // 扫描并删除该用户所有 refresh token
        var keys = redisTemplate.keys(REFRESH_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (userId.equals(value)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    /**
     * 生成文档 token（用于 OnlyOffice 文档会话）。
     *
     * <p>claims: type=doc, doc.fileId, doc.action, exp=4小时</p>
     *
     * @param userId     用户业务 ID
     * @param userFileId 用户文件 ID
     * @param action     操作类型（edit / view）
     * @param ttlSeconds 有效期（秒）
     * @return JWT 字符串
     */
    public String generateDocumentToken(String userId, Long userFileId, String action, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "doc")
                .claim("doc.fileId", userFileId)
                .claim("doc.action", action)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成回调 token（用于 OnlyOffice 回调 URL 鉴权）。
     *
     * <p>claims: type=cb, cb.fileId, cb.type, exp=30分钟</p>
     *
     * @param userId     用户业务 ID
     * @param userFileId 用户文件 ID
     * @param type       回调类型（edit）
     * @param ttlSeconds 有效期（秒）
     * @return JWT 字符串
     */
    public String generateCallbackToken(String userId, Long userFileId, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "cb")
                .claim("cb.fileId", userFileId)
                .claim("cb.type", type)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并验证文档 token。
     *
     * @param token JWT 字符串
     * @return Claims，无效时返回 null
     * @throws IllegalArgumentException 若 type 不是 doc
     */
    public Claims parseDocumentToken(String token) {
        Claims claims = parseAndValidate(token);
        if (claims == null) {
            return null;
        }
        if (!"doc".equals(claims.get("type", String.class))) {
            log.debug("Token type 不是 doc: {}", claims.get("type"));
            return null;
        }
        return claims;
    }

    /**
     * 解析并验证回调 token。
     *
     * @param token JWT 字符串
     * @return Claims，无效时返回 null
     * @throws IllegalArgumentException 若 type 不是 cb
     */
    public Claims parseCallbackToken(String token) {
        Claims claims = parseAndValidate(token);
        if (claims == null) {
            return null;
        }
        if (!"cb".equals(claims.get("type", String.class))) {
            log.debug("Token type 不是 cb: {}", claims.get("type"));
            return null;
        }
        return claims;
    }

    /**
     * 计算 token 剩余有效期（秒）。
     *
     * @param exp token 过期时间
     * @return 剩余秒数，已过期返回 0
     */
    public long remainingSeconds(Instant exp) {
        long remaining = exp.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0);
    }
}
