package com.qiwenshare.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.document.callback.CallbackContext;
import com.qiwenshare.document.callback.CallbackManager;
import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.dto.CallbackBodyDTO;
import com.qiwenshare.document.service.DocumentTokenService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentCallbackController 单元测试??
 */
@WebMvcTest(DocumentCallbackController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class})
@WithMockUser
class DocumentCallbackControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CallbackManager callbackManager;
    @MockBean private DocumentTokenService documentTokenService;
    @MockBean private OnlyOfficeProperties onlyOfficeProperties;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any(jakarta.servlet.FilterChain.class));
    }

    @Nested
    @DisplayName("POST /api/v1/document/callback")
    class CallbackEndpoint {

        @Test
        @DisplayName("有效回调返回 error=0")
        void validCallback_returnsError0() throws Exception {
            // 模拟未配??JWT secret（开发环境）
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret(null);
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            // 模拟 token 解析
            Claims claims = createMockClaims(10L, 1L);
            when(documentTokenService.parseCallbackToken(anyString())).thenReturn(claims);

            // 模拟回调处理
            when(callbackManager.dispatch(any(CallbackContext.class))).thenReturn(0);

            CallbackBodyDTO body = new CallbackBodyDTO(4, "http://example.com/file.docx", "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .param("token", "mock-callback-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));
        }

        @Test
        @DisplayName("缺少 token 时返??error=1")
        void missingToken_returnsError1() throws Exception {
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret(null);
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            CallbackBodyDTO body = new CallbackBodyDTO(4, null, "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(1));
        }

        @Test
        @DisplayName("无效 token 时返??error=1")
        void invalidToken_returnsError1() throws Exception {
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret(null);
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            when(documentTokenService.parseCallbackToken("invalid-token")).thenReturn(null);

            CallbackBodyDTO body = new CallbackBodyDTO(4, null, "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .param("token", "invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(1));
        }

        @Test
        @DisplayName("JWT 鉴权失败时返??403")
        void jwtAuthFails_returns403() throws Exception {
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret("test-secret");
            jwt.setHeader("Authorization");
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            // 模拟 OnlyOffice JWT 验证失败
            when(documentTokenService.verifyOnlyOfficeJwt(anyString())).thenReturn(null);

            CallbackBodyDTO body = new CallbackBodyDTO(4, null, "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "invalid-jwt"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value(1));
        }

        @Test
        @DisplayName("JWT 鉴权成功时继续处??)")

        void jwtAuthSuccess_continuesProcessing() throws Exception {
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret("test-secret");
            jwt.setHeader("Authorization");
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            // 模拟 OnlyOffice JWT 验证成功
            Claims ooClaims = org.mockito.Mockito.mock(Claims.class);
            when(documentTokenService.verifyOnlyOfficeJwt(anyString())).thenReturn(ooClaims);

            // 模拟回调 token 解析（从 query parameter??
            Claims claims = createMockClaims(10L, 1L);
            when(documentTokenService.parseCallbackToken(anyString())).thenReturn(claims);
            when(callbackManager.dispatch(any(CallbackContext.class))).thenReturn(0);

            CallbackBodyDTO body = new CallbackBodyDTO(4, null, "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "valid-jwt")
                            .param("token", "mock-callback-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));
        }

        @Test
        @DisplayName("回调处理失败时返??error=1")
        void callbackFails_returnsError1() throws Exception {
            OnlyOfficeProperties.Jwt jwt = new OnlyOfficeProperties.Jwt();
            jwt.setSecret(null);
            when(onlyOfficeProperties.getJwt()).thenReturn(jwt);

            Claims claims = createMockClaims(10L, 1L);
            when(documentTokenService.parseCallbackToken(anyString())).thenReturn(claims);

            // 模拟回调处理返回错误??1
            when(callbackManager.dispatch(any(CallbackContext.class))).thenReturn(1);

            CallbackBodyDTO body = new CallbackBodyDTO(2, null, "key", List.of("user1"), List.of(), null, null, null, null);

            mockMvc.perform(post("/api/v1/document/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .param("token", "mock-callback-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(1));
        }
    }

    private Claims createMockClaims(Long userFileId, Long userId) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        org.mockito.Mockito.doReturn(userFileId).when(claims).get("cb.fileId", Long.class);
        org.mockito.Mockito.doReturn(userId.toString()).when(claims).getSubject();
        return claims;
    }
}
