package com.qiwenshare.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.document.config.OnlyOfficeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OnlyOffice Command Service 客户端。
 *
 * <p>封装对 OnlyOffice Command API 的调用（forcesave、drop 等）。
 * 所有外部调用设置超时，避免无限阻塞（红线 #12）。
 * 请求使用 OnlyOffice JWT secret 签名（header + body），与 Document Server 的 JWT 验证对齐。</p>
 */
@Service
@Slf4j
public class OnlyOfficeCommandClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final OnlyOfficeProperties onlyOfficeProperties;
    private final DocumentTokenService documentTokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OnlyOfficeCommandClient(OnlyOfficeProperties onlyOfficeProperties,
                                   DocumentTokenService documentTokenService,
                                   ObjectMapper objectMapper) {
        this.onlyOfficeProperties = onlyOfficeProperties;
        this.documentTokenService = documentTokenService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * 触发 OnlyOffice 强制保存。
     *
     * <p>调用 Command API 的 forcesave 命令，让 Document Server 立即保存文档。
     * 请求同时签名 Authorization header（嵌套 payload）和 body token 字段，
     * 与 OnlyOffice Document Server 的 JWT 验证协议一致。</p>
     *
     * @param documentKey 文档 key（与 OnlyOffice 配置中的 key 一致）
     * @return 是否调用成功
     */
    public boolean forcesave(String documentKey) {
        String commandUrl = onlyOfficeProperties.getCommandUrl();
        if (commandUrl == null || commandUrl.isBlank()) {
            log.warn("OnlyOffice commandUrl 未配置，无法触发 forcesave");
            return false;
        }

        try {
            // 构建命令参数（原始参数，不含 token）
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("c", "forcesave");
            params.put("key", documentKey);

            // 先签名 header token（payload 嵌套原始参数）
            String headerToken = documentTokenService.generateCommandHeaderToken(
                    Map.of("c", "forcesave", "key", documentKey));

            // 再签名 body token 并加入 params
            String bodyToken = documentTokenService.generateCommandBodyToken(params);
            if (bodyToken != null) {
                params.put("token", bodyToken);
            }

            // 使用 Jackson 序列化请求体
            String jsonBody = objectMapper.writeValueAsString(params);

            // 构建 HTTP 请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(commandUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (headerToken != null) {
                String headerName = onlyOfficeProperties.getJwt().getHeader();
                requestBuilder.header(headerName, "Bearer " + headerToken);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("forcesave 命令发送成功: key={}", documentKey);
                return true;
            } else {
                log.warn("forcesave 命令失败: status={}, body={}, key={}",
                        response.statusCode(), response.body(), documentKey);
                return false;
            }
        } catch (Exception e) {
            log.error("forcesave 命令异常: key={}", documentKey, e);
            return false;
        }
    }
}
