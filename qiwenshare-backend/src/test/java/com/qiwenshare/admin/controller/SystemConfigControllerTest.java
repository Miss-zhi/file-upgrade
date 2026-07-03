package com.qiwenshare.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminGlobalExceptionHandler;
import com.qiwenshare.admin.common.AdminModuleException;
import com.qiwenshare.admin.dto.CreateConfigDTO;
import com.qiwenshare.admin.dto.UpdateConfigDTO;
import com.qiwenshare.admin.service.SystemConfigService;
import com.qiwenshare.admin.vo.ConfigVO;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SystemConfigController 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private SystemConfigController systemConfigController;

    @Mock
    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(systemConfigController)
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();

        var adminUser = User.withUsername("admin").password("dummy").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/config")
    class ListConfigs {

        @Test
        @DisplayName("分页查询系统参数成功")
        void listConfigs_success() throws Exception {
            ConfigVO vo = new ConfigVO(1L, "default.storage.quota", "10737418240", "默认配额", LocalDateTime.now(), LocalDateTime.now());
            Page<ConfigVO> page = new PageImpl<>(List.of(vo));
            when(systemConfigService.listConfigs(isNull(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content[0].configKey").value("default.storage.quota"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/config")
    class CreateConfig {

        @Test
        @DisplayName("新增系统参数成功")
        void createConfig_success() throws Exception {
            ConfigVO vo = new ConfigVO(1L, "new.key", "value", "desc", LocalDateTime.now(), LocalDateTime.now());
            when(systemConfigService.createConfig(any(CreateConfigDTO.class))).thenReturn(vo);

            CreateConfigDTO dto = new CreateConfigDTO("new.key", "value", "desc");

            mockMvc.perform(post("/api/v1/admin/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configKey").value("new.key"));
        }

        @Test
        @DisplayName("新增重复 key 返回 400")
        void createConfig_duplicateKey() throws Exception {
            when(systemConfigService.createConfig(any(CreateConfigDTO.class)))
                    .thenThrow(new AdminModuleException(AdminErrorCode.CONFIG_KEY_DUPLICATE));

            CreateConfigDTO dto = new CreateConfigDTO("existing.key", "value", "desc");

            mockMvc.perform(post("/api/v1/admin/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("CONFIG_KEY_DUPLICATE"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/config/{id}")
    class UpdateConfig {

        @Test
        @DisplayName("修改系统参数成功")
        void updateConfig_success() throws Exception {
            ConfigVO vo = new ConfigVO(1L, "key", "newValue", "desc", LocalDateTime.now(), LocalDateTime.now());
            when(systemConfigService.updateConfig(eq(1L), any(UpdateConfigDTO.class))).thenReturn(vo);

            UpdateConfigDTO dto = new UpdateConfigDTO("newValue", null);

            mockMvc.perform(put("/api/v1/admin/config/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configValue").value("newValue"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/config/{id}")
    class DeleteConfig {

        @Test
        @DisplayName("删除系统参数成功")
        void deleteConfig_success() throws Exception {
            doNothing().when(systemConfigService).deleteConfig(1L);

            mockMvc.perform(delete("/api/v1/admin/config/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
