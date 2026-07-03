package com.qiwenshare.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.common.RestResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 访问拒绝响应处理器。
 *
 * <p>当已认证用户无权限访问端点时，返回 {@link RestResult} 格式的 JSON 响应。</p>
 */
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        RestResult<Void> result = new RestResult<>(-1, "ACCESS_DENIED", "权限不足", null);
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
