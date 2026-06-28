package com.qiwenshare.file.config.onlyoffice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {
    /** Document Server 地址 */
    private String serverUrl = "http://localhost:9980";
    /** API 路径 */
    private String apiUrl = "/web-apps/apps/api/documents/api.js";
    /** 回调密钥 */
    private String secret = "qiwen-onlyoffice-secret";
    /** 回调地址 */
    private String callbackUrl = "http://localhost:8080/onlyoffice/callback";
}
