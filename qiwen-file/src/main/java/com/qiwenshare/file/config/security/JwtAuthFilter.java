package com.qiwenshare.file.config.security;

import com.qiwenshare.file.config.jwt.JwtProperties;
import com.qiwenshare.file.config.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 — 每个请求拦截验证 Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Claims claims = jwtUtil.parseToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            if (role == null) role = "USER";

            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix())) {
            return bearerToken.substring(jwtProperties.getTokenPrefix().length());
        }
        return null;
    }
}
