package com.qiwenshare.auth.filter;

import com.qiwenshare.auth.service.TokenService;
import com.qiwenshare.auth.service.UserDetailServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>在 {@code UsernamePasswordAuthenticationFilter} 之前执行。
 * 从 cookie 或 Authorization header 提取 access token，
 * 经过签名验证 → 黑名单检查 → 全局撤销检查后，
 * 加载用户权限并设置 {@code SecurityContext}。</p>
 *
 * <p>无 token 或 token 无效时不抛异常，仅不设置 SecurityContext，
 * 由后续 AuthorizationFilter 决定是否拒绝。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 1. 提取 token（cookie 优先，然后 Authorization header）
        String token = extractToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 解析并验证 JWT
        Claims claims = tokenService.parseAndValidate(token);
        if (claims == null) {
            chain.doFilter(request, response);
            return;
        }

        // 3. 检查 type 必须为 "access"
        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            chain.doFilter(request, response);
            return;
        }

        // 4. 检查黑名单
        String jti = claims.get("jti", String.class);
        if (jti != null && tokenService.isBlacklisted(jti)) {
            log.debug("Token jti={} 在黑名单中", jti);
            chain.doFilter(request, response);
            return;
        }

        // 5. 检查全局撤销
        String userId = claims.getSubject();
        long iat = claims.getIssuedAt().toInstant().getEpochSecond();
        if (tokenService.isRevoked(userId, iat)) {
            log.debug("Token userId={} 已被全局撤销", userId);
            chain.doFilter(request, response);
            return;
        }

        // 6. 加载用户权限，设置 SecurityContext
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.debug("加载用户信息失败: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    /**
     * 从请求中提取 access token。
     * 优先级：cookie access_token > Authorization: Bearer header。
     */
    private String extractToken(HttpServletRequest request) {
        // 先检查 cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 再检查 Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }
}
