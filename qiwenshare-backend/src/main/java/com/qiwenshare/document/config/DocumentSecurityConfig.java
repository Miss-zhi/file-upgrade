package com.qiwenshare.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 文档模块安全配置。
 *
 * <p>回调端点放行用户认证（OnlyOffice 回调无用户 session），
 * 通过 OnlyOffice JWT 验证请求来源。</p>
 *
 * <p>使用独立的 SecurityFilterChain（优先级高于主链），
 * 仅匹配回调端点。</p>
 */
@Configuration
@EnableWebSecurity
public class DocumentSecurityConfig {

    /**
     * 回调端点安全过滤链。
     *
     * <p>回调端点不需要用户认证，放行所有请求。
     * 回调来源验证由 Controller 层的 OnlyOffice JWT 检查完成。</p>
     *
     * @param http HttpSecurity 构建器
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain documentCallbackFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/document/callback")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
