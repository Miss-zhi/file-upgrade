package com.qiwenshare.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.common.GlobalExceptionHandler;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.dto.ChangePasswordRequest;
import com.qiwenshare.auth.dto.LoginRequest;
import com.qiwenshare.auth.dto.RegisterRequest;
import com.qiwenshare.auth.exception.AuthErrorCode;
import com.qiwenshare.auth.exception.AuthException;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.service.TokenService;
import com.qiwenshare.auth.service.UserDetailServiceImpl;
import com.qiwenshare.auth.vo.LoginResponse;
import com.qiwenshare.auth.vo.UserInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 单元测试（覆??Task 8.8??.5??0.4�??
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserDetailServiceImpl userDetailServiceImpl;

    @BeforeEach
    void setUp() throws Exception {
        // Mock filter 必须调用 chain.doFilter 否则请求不会到达 Controller
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any(jakarta.servlet.FilterChain.class));
    }

    // ===== 8.8 注册 + 登录 =====

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("注册成功 ??201 + userId")
        void register_success() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn("1234567890");

            RegisterRequest request = new RegisterRequest("testuser", "13800138000", "Password1");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value("1234567890"));
        }

        @Test
        @DisplayName("重复用户????400")
        void register_duplicateUsername_400() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new AuthException(AuthErrorCode.USERNAME_EXISTS));

            RegisterRequest request = new RegisterRequest("existing", "13800138000", "Password1");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(-1));
        }

        @Test
        @DisplayName("参数校验失败 ??400")
        void register_invalidParams_400() throws Exception {
            RegisterRequest request = new RegisterRequest("", "invalid", "weak");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("登录成功 ??200 + 用户信息")
        void login_success() throws Exception {
            LoginResponse response = new LoginResponse("123", "testuser", List.of("USER"), List.of("file:read"));
            when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                    .thenReturn(response);

            LoginRequest request = new LoginRequest("13800138000", "Password1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value("123"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("密码错误 ??401")
        void login_wrongPassword_401() throws Exception {
            when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                    .thenThrow(new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

            LoginRequest request = new LoginRequest("13800138000", "WrongPass1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("连续5次失????423 账户锁定")
        void login_locked_423() throws Exception {
            when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                    .thenThrow(new AuthException(AuthErrorCode.AUTH_ACCOUNT_LOCKED));

            LoginRequest request = new LoginRequest("13800138000", "WrongPass1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isLocked());
        }
    }

    // ===== 9.5 登出 + 刷新 =====

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("登出成功 ??200")
        void logout_success() throws Exception {
            doNothing().when(authService).logout(any(), any());

            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("刷新成功 ??200")
        void refresh_success() throws Exception {
            doNothing().when(authService).refresh(any(), any());

            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("refresh token 无效 ??401")
        void refresh_invalidToken_401() throws Exception {
            doThrow(new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID))
                    .when(authService).refresh(any(), any());

            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 10.4 获取用户 + 修改密码 =====

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class Me {

        @Test
        @DisplayName("返回完整用户信息")
        void me_success() throws Exception {
            UserInfoResponse response = new UserInfoResponse(
                    "123", "testuser", "138****8000", null,
                    List.of("USER"), List.of("file:read"), LocalDateTime.now());
            when(authService.getCurrentUser()).thenReturn(response);

            UserDetails userDetails = User.withUsername("testuser")
                    .password("dummy").roles("USER").build();

            mockMvc.perform(get("/api/v1/auth/me")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value("123"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/password")
    class UpdatePassword {

        @Test
        @DisplayName("修改密码成功 ??200")
        void updatePassword_success() throws Exception {
            doNothing().when(authService).updatePassword(any(ChangePasswordRequest.class));

            ChangePasswordRequest request = new ChangePasswordRequest("OldPass1", "NewPass1");

            UserDetails userDetails = User.withUsername("testuser")
                    .password("dummy").roles("USER").build();

            mockMvc.perform(put("/api/v1/auth/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("旧密码错????401")
        void updatePassword_wrongOld_401() throws Exception {
            doThrow(new AuthException(AuthErrorCode.AUTH_OLD_PASSWORD_WRONG))
                    .when(authService).updatePassword(any(ChangePasswordRequest.class));

            ChangePasswordRequest request = new ChangePasswordRequest("WrongOld1", "NewPass1");

            UserDetails userDetails = User.withUsername("testuser")
                    .password("dummy").roles("USER").build();

            mockMvc.perform(put("/api/v1/auth/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
