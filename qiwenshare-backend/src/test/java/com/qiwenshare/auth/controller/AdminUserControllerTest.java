package com.qiwenshare.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.admin.common.AdminGlobalExceptionHandler;
import com.qiwenshare.auth.common.GlobalExceptionHandler;
import com.qiwenshare.auth.dto.ResetPasswordRequest;
import com.qiwenshare.auth.dto.UserListQuery;
import com.qiwenshare.auth.service.AdminUserService;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.vo.UserDetailVO;
import com.qiwenshare.auth.vo.UserListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AdminUserController adminUserController;

    @Mock
    private AuthService authService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private com.qiwenshare.auth.event.PermissionChangeEventPublisher publisher;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler(), new AdminGlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        @DisplayName("分页查询用户列表成功")
        void listUsers_success() throws Exception {
            UserListVO vo = new UserListVO("123", "testuser", "13800000000", 1, LocalDateTime.now(), List.of("USER"));
            Page<UserListVO> page = new PageImpl<>(List.of(vo));
            when(adminUserService.listUsers(any(UserListQuery.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content[0].username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUserDetail {

        @Test
        @DisplayName("查询用户详情成功")
        void getUserDetail_success() throws Exception {
            UserDetailVO vo = new UserDetailVO("123", "testuser", "13800000000", 1, LocalDateTime.now(),
                    List.of(new UserDetailVO.RoleInfo(2, "USER")), List.of("file:upload"));
            when(adminUserService.getUserDetail("123")).thenReturn(vo);

            mockMvc.perform(get("/api/v1/admin/users/123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.roles[0].roleName").value("USER"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/enable")
    class EnableUser {

        @Test
        @DisplayName("启用用户成功")
        void enableUser_success() throws Exception {
            doNothing().when(adminUserService).enableUser("123");

            mockMvc.perform(put("/api/v1/admin/users/123/enable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/disable")
    class DisableUser {

        @Test
        @DisplayName("禁用用户成功")
        void disableUser_success() throws Exception {
            doNothing().when(adminUserService).disableUser("123");

            mockMvc.perform(put("/api/v1/admin/users/123/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/password")
    class ResetPassword {

        @Test
        @DisplayName("重置密码成功")
        void resetPassword_success() throws Exception {
            doNothing().when(authService).resetPassword(anyString(), any(ResetPasswordRequest.class), anyString());

            ResetPasswordRequest request = new ResetPasswordRequest("NewPass1");

            mockMvc.perform(put("/api/v1/admin/users/123/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
