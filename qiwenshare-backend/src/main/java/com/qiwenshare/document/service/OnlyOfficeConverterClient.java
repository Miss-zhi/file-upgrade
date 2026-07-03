package com.qiwenshare.document.service;

import com.fasterxml.jackson.databind.JsonNode;
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
 * OnlyOffice Conversion Service 客户端。
 *
 * <p>调用 Converter API 将文件从一种格式转换为另一种（如 doc → docx）。
 * 主要用于保存回调时：OnlyOffice 返回的文件格式可能与原始格式不同，
 * 需要转换后再存储。</p>
 *
 * <p>请求使用 OnlyOffice JWT secret 签名（header + body token），
 * 与旧项目 {@code DefaultServiceConverter} 行为一致。</p>
 */
@Service
@Slf4j
public class OnlyOfficeConverterClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final OnlyOfficeProperties onlyOfficeProperties;
    private final DocumentTokenService documentTokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OnlyOfficeConverterClient(OnlyOfficeProperties onlyOfficeProperties,
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
     * 转换文件格式。
     *
     * @param documentUrl   源文件的下载 URL（OnlyOffice 能访问到的地址）
     * @param fromExtension 源文件扩展名（不含点号，如 "doc"）
     * @param toExtension   目标扩展名（不含点号，如 "docx"）
     * @param documentKey   文档 key（用于去重和缓存）
     * @param title         文件标题（可选）
     * @return 转换后文件的下载 URL，转换失败或未完成时返回 null
     */
    public String convert(String documentUrl, String fromExtension,
                          String toExtension, String documentKey, String title) {
        String converterUrl = onlyOfficeProperties.getConverterUrl();
        if (converterUrl == null || converterUrl.isBlank()) {
            log.warn("OnlyOffice converterUrl 未配置，无法执行格式转换");
            return null;
        }

        try {
            // 构建转换请求参数（原始参数，不含 token）
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("url", documentUrl);
            params.put("outputtype", toExtension);
            params.put("filetype", fromExtension);
            params.put("key", documentKey);
            if (title != null && !title.isBlank()) {
                params.put("title", title);
            }

            // 先签名 header token（payload 嵌套原始参数，不含 body token）
            String headerToken = documentTokenService.generateCommandHeaderToken(params);

            // 再签名 body token 并加入 params
            String bodyToken = documentTokenService.generateCommandBodyToken(params);
            if (bodyToken != null) {
                params.put("token", bodyToken);
            }

            // 使用 Jackson 序列化请求体
            String jsonBody = objectMapper.writeValueAsString(params);

            // 构建 HTTP 请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(converterUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (headerToken != null) {
                String headerName = onlyOfficeProperties.getJwt().getHeader();
                requestBuilder.header(headerName, "Bearer " + headerToken);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Converter API 请求失败: status={}, body={}",
                        response.statusCode(), response.body());
                return null;
            }

            return parseConvertResponse(response.body());

        } catch (Exception e) {
            log.error("Converter API 调用异常: url={}, from={}, to={}",
                    documentUrl, fromExtension, toExtension, e);
            return null;
        }
    }

    /**
     * 生成文档 revision ID（用于 Converter API 的 key 参数）。
     *
     * <p>与旧项目 {@code DefaultServiceConverter.generateRevisionId()} 逻辑一致：
     * 超过 20 字符时 hashCode，最终截取前 20 位。</p>
     */
    public String generateRevisionId(String input) {
        if (input == null || input.isBlank()) {
            return String.valueOf(System.currentTimeMillis());
        }
        String key = input.length() > 20
                ? String.valueOf(input.hashCode())
                : input;
        key = key.replaceAll("[^0-9\\-.a-zA-Z_=]", "_");
        return key.substring(0, Math.min(key.length(), 20));
    }

    /**
     * 解析 Converter API 响应。
     *
     * <p>响应格式：{@code {"fileUrl":"...", "endConvert":true/false, "percent":N}}。
     * 转换完成返回 fileUrl，未完成返回 null。</p>
     */
    private String parseConvertResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // 检查 error 字段
            if (root.has("error") && !root.get("error").asText().equals("0")) {
                log.error("Converter API 返回错误: error={}", root.get("error"));
                return null;
            }

            // 检查 endConvert
            if (!root.path("endConvert").asBoolean(false)) {
                log.debug("转换未完成，返回 null");
                return null;
            }

            // 提取 fileUrl
            JsonNode fileUrlNode = root.get("fileUrl");
            return fileUrlNode != null ? fileUrlNode.asText() : null;

        } catch (Exception e) {
            log.error("解析 Converter 响应失败: {}", json, e);
            return null;
        }
    }
}
