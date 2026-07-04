package com.qiwenshare.auth.config;

import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 6 配置。
 *
 * <p>使用 {@code SecurityFilterChain} Bean 方式（替代已移除的 {@code WebSecurityConfigurerAdapter}）。
 * 启用 {@code @EnableMethodSecurity} 支持 {@code @PreAuthorize} 方法级权限控制。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPoint authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${onlyoffice.server-url:}")
    private String onlyOfficeServerUrl;

    /**
     * 配置 SecurityFilterChain。
     *
     * @param http HttpSecurity 构建器
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSP 中的 OnlyOffice 地址从配置读取，未配置时留空（仅允许 self）
        String oo = onlyOfficeServerUrl != null && !onlyOfficeServerUrl.isBlank() ? onlyOfficeServerUrl : "";
        String csp = "default-src 'self'; "
                + "script-src 'self' 'unsafe-eval' 'unsafe-inline'" + (oo.isEmpty() ? "" : " " + oo) + "; "
                + "style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data:; "
                + "font-src 'self' data:; "
                + "connect-src 'self' ws: wss:" + (oo.isEmpty() ? "" : " " + oo) + "; "
                + "frame-src 'self'" + (oo.isEmpty() ? "" : " " + oo);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .headers(headers -> headers
                .contentSecurityPolicy(cspCfg -> cspCfg.policyDirectives(csp))
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout",
                    "/api/v1/share/info/**",
                    "/api/v1/share/verifyshare",
                    "/api/v1/share/download/**",
                    "/api/v1/document/download/**",
                    "/api/v1/document/callback",
                    "/api/v1/filetransfer/preview/**",
                    "/api/v1/param/**",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }

    /**
     * 密码编码器。
     *
     * @return BCryptPasswordEncoder（strength=10）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
