package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.vo.DocumentHealthVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 文档管理服务（管理员操作）。
 */
@Service
@Slf4j
public class DocumentAdminService {

    private final OnlyOfficeProperties onlyOfficeProperties;
    private final HttpClient httpClient;

    /**
     * 构造方法，注入 HttpClient（复用连接池）。
     *
     * @param onlyOfficeProperties OnlyOffice 配置
     * @param httpClient           复用的 HTTP 客户端
     */
    public DocumentAdminService(OnlyOfficeProperties onlyOfficeProperties, HttpClient httpClient) {
        this.onlyOfficeProperties = onlyOfficeProperties;
        this.httpClient = httpClient;
    }

    /**
     * Spring 自动注入构造方法：使用默认 HttpClient。
     *
     * @param onlyOfficeProperties OnlyOffice 配置
     */
    @org.springframework.beans.factory.annotation.Autowired
    public DocumentAdminService(OnlyOfficeProperties onlyOfficeProperties) {
        this(onlyOfficeProperties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    /**
     * 检查 OnlyOffice Document Server 健康状态。
     *
     * @return 健康状态 VO
     */
    public DocumentHealthVO checkHealth() {
        String serverUrl = onlyOfficeProperties.getServerUrl();
        // 防止双斜杠：去除末尾斜杠后再拼接（S2）
        String normalizedUrl = serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl + "/healthcheck"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new DocumentHealthVO("UP", serverUrl, null);
            } else {
                return new DocumentHealthVO("DOWN", serverUrl, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.warn("OnlyOffice 健康检查失败: {}", e.getMessage());
            return new DocumentHealthVO("DOWN", serverUrl, e.getMessage());
        }
    }
}
