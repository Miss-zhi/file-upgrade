package com.qiwenshare.document.service;

import com.qiwenshare.auth.service.TokenService;
import com.qiwenshare.document.config.OnlyOfficeProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 文档 Token 服务。
 *
 * <p>封装 TokenService 的文档/回调 token 生成与验证。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTokenService {

    private final TokenService tokenService;
    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 生成文档 token。
     *
     * @param userId     用户 ID
     * @param userFileId 用户文件 ID
     * @param action     操作类型（edit / view）
     * @return JWT 字符串
     */
    public String generateDocumentToken(String userId, Long userFileId, String action) {
        return tokenService.generateDocumentToken(userId, userFileId, action,
                onlyOfficeProperties.getDocumentTokenTtl());
    }

    /**
     * 生成回调 token。
     *
     * @param userId     用户 ID
     * @param userFileId 用户文件 ID
     * @param type       回调类型
     * @return JWT 字符串
     */
    public String generateCallbackToken(String userId, Long userFileId, String type) {
        return tokenService.generateCallbackToken(userId, userFileId, type,
                onlyOfficeProperties.getCallbackTokenTtl());
    }

    /**
     * 解析并验证文档 token。
     *
     * @param token JWT 字符串
     * @return Claims，无效时返回 null
     */
    public Claims parseDocumentToken(String token) {
        return tokenService.parseDocumentToken(token);
    }

    /**
     * 解析并验证回调 token。
     *
     * @param token JWT 字符串
     * @return Claims，无效时返回 null
     */
    public Claims parseCallbackToken(String token) {
        return tokenService.parseCallbackToken(token);
    }

    /**
     * 使用 OnlyOffice JWT secret 验证回调请求的 JWT。
     *
     * <p>OnlyOffice Document Server 使用独立的 JWT secret 签名回调请求，
     * 与应用级签名密钥不同，必须使用 OnlyOffice 配置的 secret 验证。</p>
     *
     * @param token OnlyOffice JWT 字符串
     * @return Claims，验证失败返回 null
     */
    public Claims verifyOnlyOfficeJwt(String token) {
        String secret = onlyOfficeProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            log.debug("OnlyOffice JWT secret 未配置，无法验证回调");
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("OnlyOffice JWT 验证失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    // ---- OnlyOffice 签名方法（用 OnlyOffice JWT secret，非应用级密钥） ----

    /**
     * 生成 OnlyOffice Editor Config token。
     *
     * <p>此 token 发送给前端编辑器，OnlyOffice Document Server 会用它验证请求来源。
     * 必须用 OnlyOffice JWT secret 签名（非应用级 jwt.secret）。</p>
     *
     * @param documentConfig 文档配置 Map（document 对象的所有字段）
     * @return JWT 字符串，若 secret 未配置则返回 null
     */
    public String generateEditorConfigToken(Map<String, Object> documentConfig) {
        SecretKey key = getOnlyOfficeKey();
        if (key == null) return null;
        return Jwts.builder()
                .claims(documentConfig)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 OnlyOffice Command API 请求体 token。
     *
     * <p>签名为 JWT，放入请求 body 的 token 字段。</p>
     *
     * @param commandBody Command 请求体 Map（如 {"c":"forcesave","key":"xxx"}）
     * @return JWT 字符串，若 secret 未配置则返回 null
     */
    public String generateCommandBodyToken(Map<String, Object> commandBody) {
        SecretKey key = getOnlyOfficeKey();
        if (key == null) return null;
        return Jwts.builder()
                .claims(commandBody)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 OnlyOffice Command API Header token。
     *
     * <p>OnlyOffice 要求 header token 的 payload 嵌套在 "payload" claim 中：
     * {@code {"payload": {实际命令参数}}}。这与 body token 的结构不同。</p>
     *
     * @param commandBody Command 请求体 Map
     * @return JWT 字符串，若 secret 未配置则返回 null
     */
    public String generateCommandHeaderToken(Map<String, Object> commandBody) {
        SecretKey key = getOnlyOfficeKey();
        if (key == null) return null;
        return Jwts.builder()
                .claim("payload", commandBody)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getOnlyOfficeKey() {
        String secret = onlyOfficeProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            log.debug("OnlyOffice JWT secret 未配置，跳过签名");
            return null;
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
