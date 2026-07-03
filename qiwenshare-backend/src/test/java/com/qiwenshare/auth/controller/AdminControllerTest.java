package com.qiwenshare.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.auth.common.GlobalExceptionHandler;
import com.qiwenshare.auth.dto.ResetPasswordRequest;
import com.qiwenshare.auth.dto.UpdateRolePermissionsRequest;
import com.qiwenshare.auth.event.PermissionChangeEventPublisher;
import com.qiwenshare.auth.service.AdminUserService;
import com.qiwenshare.auth.service.AuthService;
import com.qiwenshare.auth.vo.RoleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin controller unit tests.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AdminUserController adminUserController;

    @InjectMocks
    private AdminRoleController adminRoleController;

    @Mock
    private AuthService authService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private PermissionChangeEventPublisher publisher;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminUserController, adminRoleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/password")
    class ResetPassword {

        @Test
        @DisplayName("reset password success - 200 + publish permission change event")
        void resetPassword_success() throws Exception {
            doNothing().when(authService).resetPassword(anyString(), any(ResetPasswordRequest.class), anyString());

            ResetPasswordRequest request = new ResetPasswordRequest("NewPass1");

            mockMvc.perform(put("/api/v1/admin/users/123/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(publisher).publishPermissionChanged(List.of("123"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/roles")
    class ListRoles {

        @Test
        @DisplayName("list roles success")
        void listRoles_success() throws Exception {
            List<RoleResponse> roles = List.of(
                    new RoleResponse(1, "ADMIN", "admin", 1, List.of(1, 2, 3)),
                    new RoleResponse(2, "USER", "user", 1, List.of(1))
            );
            when(authService.listRoles()).thenReturn(roles);

            mockMvc.perform(get("/api/v1/admin/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].roleName").value("ADMIN"))
                    .andExpect(jsonPath("$.data[0].permissions").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/roles/{roleId}/permissions")
    class UpdateRolePermissions {

        @Test
        @DisplayName("update role permissions success + publish event")
        void updatePermissions_success() throws Exception {
            when(authService.updateRolePermissions(eq(1), anyList()))
                    .thenReturn(List.of("user1", "user2"));

            UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(List.of(1, 2, 3));

            mockMvc.perform(put("/api/v1/admin/roles/1/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(publisher).publishPermissionChanged(List.of("user1", "user2"));
        }

        @Test
        @DisplayName("cache invalidated after permission change")
        void updatePermissions_cacheInvalidated() throws Exception {
            when(authService.updateRolePermissions(eq(2), anyList()))
                    .thenReturn(List.of("user3"));

            UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(List.of(1));

            mockMvc.perform(put("/api/v1/admin/roles/2/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(publisher).publishPermissionChanged(List.of("user3"));
        }
    }
}
