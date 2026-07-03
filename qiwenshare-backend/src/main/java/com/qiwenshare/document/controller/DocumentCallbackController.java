package com.qiwenshare.document.controller;

import com.qiwenshare.document.callback.CallbackContext;
import com.qiwenshare.document.callback.CallbackManager;
import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.dto.CallbackBodyDTO;
import com.qiwenshare.document.service.DocumentTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OnlyOffice 回调端点。
 *
 * <p>放行 Spring Security 用户认证，通过 OnlyOffice JWT 验证请求来源。</p>
 */
@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentCallbackController {

    private final CallbackManager callbackManager;
    private final DocumentTokenService documentTokenService;
    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 接收 OnlyOffice 状态回调。
     *
     * <p><b>注意：</b>此端点返回 {@code Map<String, Integer>} 而非项目统一的 {@code RestResult<T>}，
     * 因为 OnlyOffice Document Server 协议要求回调响应必须为 {@code {"error": 0|1}} 格式，
     * 属于 REST 规范的协议级豁免。</p>
     *
     * @param body    回调请求体
     * @param request HTTP 请求（用于提取 JWT header）
     * @return 处理结果 {@code {"error": 0}} 或 {@code {"error": 1}}
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Integer>> callback(
            @RequestBody CallbackBodyDTO body,
            HttpServletRequest request) {

        // 验证 OnlyOffice JWT header
        if (!verifyCallbackAuth(request)) {
            log.warn("回调鉴权失败: status={}", body.status());
            return ResponseEntity.status(403).body(Map.of("error", 1));
        }

        // 从回调 URL 的 token 参数中提取 userFileId 和 userId
        String token = request.getParameter("token");
        Long userFileId = null;
        Long userId = null;

        if (token != null && !token.isBlank()) {
            Claims claims = documentTokenService.parseCallbackToken(token);
            if (claims != null) {
                userFileId = claims.get("cb.fileId", Long.class);
                userId = Long.parseLong(claims.getSubject());
            }
        }

        if (userFileId == null) {
            log.warn("回调缺少有效 token: status={}", body.status());
            return ResponseEntity.ok(Map.of("error", 1));
        }

        CallbackContext context = new CallbackContext(body, userFileId, userId);
        int errorCode = callbackManager.dispatch(context);

        return ResponseEntity.ok(Map.of("error", errorCode));
    }

    /**
     * 验证回调请求的 OnlyOffice JWT。
     *
     * <p>使用 OnlyOffice 配置的独立 JWT secret 验证（与应用级签名密钥不同）。</p>
     */
    private boolean verifyCallbackAuth(HttpServletRequest request) {
        String jwtSecret = onlyOfficeProperties.getJwt().getSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            // 未配置 JWT secret，跳过验证（开发环境）
            log.debug("OnlyOffice JWT secret 未配置，跳过回调鉴权");
            return true;
        }

        String headerName = onlyOfficeProperties.getJwt().getHeader();
        String authHeader = request.getHeader(headerName);
        if (authHeader == null || authHeader.isBlank()) {
            return false;
        }

        // OnlyOffice 协议规定 JWT 直接传递（无 Bearer 前缀），
        // 但为兼容网关/代理可能添加的 Bearer，做防御性剥离（S7）
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        // 使用 OnlyOffice JWT secret 验证（非应用级签名密钥）
        Claims claims = documentTokenService.verifyOnlyOfficeJwt(token);
        return claims != null;
    }
}
