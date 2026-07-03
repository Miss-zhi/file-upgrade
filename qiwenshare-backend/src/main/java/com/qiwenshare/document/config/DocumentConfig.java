package com.qiwenshare.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文档模块配置。
 *
 * <p>注册 {@link OnlyOfficeProperties} 为 Spring Bean。</p>
 */
@Configuration
@EnableConfigurationProperties(OnlyOfficeProperties.class)
public class DocumentConfig {
}
