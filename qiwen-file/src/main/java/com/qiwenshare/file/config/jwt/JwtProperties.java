package com.qiwenshare.file.config.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 密钥（Base64 编码） */
    private String secret = "cWl3ZW5maWxlLWp3dC1zZWNyZXQta2V5LWZvci10b2tlbi1zaWduaW5n";

    /** Token 有效期（秒），默认 7 天 */
    private long expiration = 604800;

    /** Token 前缀 */
    private String tokenPrefix = "Bearer ";

    /** Header 名称 */
    private String header = "Authorization";
}
