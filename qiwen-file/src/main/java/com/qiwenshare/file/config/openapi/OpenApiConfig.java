package com.qiwenshare.file.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qiwenFileOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("奇文网盘 API")
                        .description("奇文网盘文件管理系统 REST API 文档")
                        .version("3.0.0")
                        .contact(new Contact().name("Qiwen").email("admin@qiwenshare.com"))
                        .license(new License().name("MIT")));
    }
}
