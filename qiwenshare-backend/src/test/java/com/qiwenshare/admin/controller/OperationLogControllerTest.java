package com.qiwenshare.admin.controller;

import com.qiwenshare.admin.common.AdminGlobalExceptionHandler;
import com.qiwenshare.admin.service.OperationLogService;
import com.qiwenshare.admin.vo.OperationLogVO;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OperationLogController 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class OperationLogControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private OperationLogController operationLogController;

    @Mock
    private OperationLogService operationLogService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(operationLogController)
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/logs")
    class ListLogs {

        @Test
        @DisplayName("分页查询操作日志成功")
        void listLogs_success() throws Exception {
            OperationLogVO vo = new OperationLogVO(1L, "admin", "admin", "user", "UPDATE",
                    "禁用用户", "PUT", "/api/v1/admin/users/123/disable", null, 200, null,
                    "127.0.0.1", "Mozilla/5.0", 50L, LocalDateTime.now());
            Page<OperationLogVO> page = new PageImpl<>(List.of(vo));
            when(operationLogService.listLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content[0].module").value("user"))
                    .andExpect(jsonPath("$.data.content[0].action").value("UPDATE"));
        }

        @Test
        @DisplayName("按模块过滤操作日志成功")

        void listLogs_filterByModule() throws Exception {
            Page<OperationLogVO> page = new PageImpl<>(List.of());
            when(operationLogService.listLogs(eq("config"), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/logs?module=config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }
}
