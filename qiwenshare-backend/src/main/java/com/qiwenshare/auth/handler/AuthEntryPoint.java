package com.qiwenshare.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.common.RestResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 未认证响应处理器。
 *
 * <p>当未认证用户访问受保护端点时，返回 {@link RestResult} 格式的 JSON 响应。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        RestResult<Void> result = new RestResult<>(-1, "AUTH_NOT_AUTHENTICATED", "请先登录", null);
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
