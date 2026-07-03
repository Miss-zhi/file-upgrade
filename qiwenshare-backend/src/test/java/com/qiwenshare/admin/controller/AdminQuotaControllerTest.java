package com.qiwenshare.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.admin.common.AdminGlobalExceptionHandler;
import com.qiwenshare.admin.dto.BatchSetQuotaDTO;
import com.qiwenshare.admin.dto.SetQuotaDTO;
import com.qiwenshare.admin.service.AdminQuotaService;
import com.qiwenshare.admin.vo.AdminQuotaVO;
import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminModuleException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminQuotaController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AdminQuotaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AdminQuotaController adminQuotaController;

    @Mock
    private AdminQuotaService adminQuotaService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminQuotaController)
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/quota/{userId}")
    class GetQuotaInfo {

        @Test
        @DisplayName("查询用户配额成功")
        void getQuotaInfo_success() throws Exception {
            AdminQuotaVO vo = new AdminQuotaVO("123", 10737418240L, 1073741824L, 9663676416L);
            when(adminQuotaService.getQuotaInfo("123")).thenReturn(vo);

            mockMvc.perform(get("/api/v1/admin/quota/123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalQuota").value(10737418240L))
                    .andExpect(jsonPath("$.data.usedQuota").value(1073741824L));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/quota/{userId}")
    class SetQuota {

        @Test
        @DisplayName("设置用户配额成功")
        void setQuota_success() throws Exception {
            doNothing().when(adminQuotaService).setQuota(eq("123"), anyLong());

            SetQuotaDTO dto = new SetQuotaDTO(21474836480L);

            mockMvc.perform(put("/api/v1/admin/quota/123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/quota/batch")
    class BatchSetQuota {

        @Test
        @DisplayName("批量设置配额成功")
        void batchSetQuota_success() throws Exception {
            when(adminQuotaService.batchSetQuota(any(BatchSetQuotaDTO.class))).thenReturn(List.of());

            BatchSetQuotaDTO dto = new BatchSetQuotaDTO(
                    List.of(new BatchSetQuotaDTO.QuotaItem("123", 10737418240L))
            );

            mockMvc.perform(put("/api/v1/admin/quota/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("设置非法配额值返??400")
        void setQuota_invalidQuota_returns400() throws Exception {
            // @Min(1) 校验??Controller 参数绑定阶段即拦截，不会到达 Service
            SetQuotaDTO dto = new SetQuotaDTO(-1L);

            mockMvc.perform(put("/api/v1/admin/quota/123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("用户不存在返??404")
        void setQuota_userNotFound_returns404() throws Exception {
            doThrow(new AdminModuleException(AdminErrorCode.USER_NOT_FOUND))
                    .when(adminQuotaService).setQuota(eq("999"), anyLong());

            SetQuotaDTO dto = new SetQuotaDTO(10737418240L);

            mockMvc.perform(put("/api/v1/admin/quota/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }
}
