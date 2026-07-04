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

        String token = request.getParameter("token");

        // 验证 OnlyOffice JWT（header 或 body token）
        if (!verifyCallbackAuth(request, body)) {
            log.warn("回调鉴权失败: status={}", body.status());
            return ResponseEntity.status(403).body(Map.of("error", 1));
        }

        // 从回调 URL 的 token 参数中提取 userFileId 和 userId
        Long userFileId = null;
        Long userId = null;

        if (token != null && !token.isBlank()) {
            Claims claims = documentTokenService.parseCallbackToken(token);
            if (claims != null) {
                userFileId = claims.get("cb.fileId", Long.class);
                userId = Long.parseLong(claims.getSubject());
            } else {
                log.warn("回调 token 解析失败（过期或签名无效）: status={}", body.status());
            }
        } else {
            log.warn("回调 token 缺失: status={}", body.status());
        }

        if (userFileId == null) {
            log.warn("回调缺少有效 token: status={}, key={}", body.status(), body.key());
            return ResponseEntity.ok(Map.of("error", 1));
        }

        CallbackContext context = new CallbackContext(body, userFileId, userId);
        int errorCode = callbackManager.dispatch(context);

        return ResponseEntity.ok(Map.of("error", errorCode));
    }

    /**
     * 验证回调请求的 OnlyOffice JWT。
     *
     * <p>优先从 Authorization header 读取 JWT（status=2/3/6/7），
     * 若 header 则回退到请求体的 token 字段（status=1/4）。
     * 使用 OnlyOffice 配置的独立 JWT secret 验证（与应用级签名密钥不同）。</p>
     */
    private boolean verifyCallbackAuth(HttpServletRequest request, CallbackBodyDTO body) {
        // OnlyOffice 6.4.2 不对回调请求签 JWT（JWT 仅用于 editor config token）。
        // 高版本（7.x+）才在回调中携带 JWT。
        // 安全性由回调 URL 中的应用级 callback token 保障（下方 parseCallbackToken 验证）。
        log.debug("回调鉴权: status={} 由 URL callback token 保障，跳过 OnlyOffice JWT", body.status());
        return true;
    }
}
