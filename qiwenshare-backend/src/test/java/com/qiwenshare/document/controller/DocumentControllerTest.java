package com.qiwenshare.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.document.exception.DocumentGlobalExceptionHandler;
import com.qiwenshare.document.dto.EditRequestDTO;
import com.qiwenshare.document.dto.PreviewRequestDTO;
import com.qiwenshare.document.service.DocumentEditService;
import com.qiwenshare.document.service.DocumentHistoryService;
import com.qiwenshare.document.service.DocumentPreviewService;
import com.qiwenshare.document.vo.DocumentVersionVO;
import com.qiwenshare.document.vo.EditConfigVO;
import com.qiwenshare.document.vo.PreviewConfigVO;
import com.qiwenshare.file.service.FilePermissionService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentController 单元测试??
 */
@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class, DocumentGlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DocumentPreviewService documentPreviewService;
    @MockBean private DocumentEditService documentEditService;
    @MockBean private DocumentHistoryService documentHistoryService;
    @MockBean private FilePermissionService filePermissionService;
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
    @DisplayName("POST /api/v1/document/preview")
    class PreviewEndpoint {

        @Test
        @WithMockUser(username = "1")
        @DisplayName("有效请求返回预览配置")
        void validRequest_returnsPreviewConfig() throws Exception {
            PreviewConfigVO config = createMockPreviewConfig();
            when(documentPreviewService.buildPreviewConfig(eq(10L), eq(1L))).thenReturn(config);

            PreviewRequestDTO dto = new PreviewRequestDTO(10L);

            mockMvc.perform(post("/api/v1/document/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.document.docType").value("word"))
                    .andExpect(jsonPath("$.data.editorConfig.mode").value("edit"));
        }

        @Test
        @DisplayName("未认证时返回 401")
        void unauthenticated_returns401() throws Exception {
            PreviewRequestDTO dto = new PreviewRequestDTO(10L);

            mockMvc.perform(post("/api/v1/document/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "1")
        @DisplayName("userFileId 为空时返??400")
        void missingUserFileId_returns400() throws Exception {
            String invalidJson = "{}";

            mockMvc.perform(post("/api/v1/document/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson)
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/document/edit")
    class EditEndpoint {

        @Test
        @WithMockUser(username = "1")
        @DisplayName("有效请求返回编辑配置")
        void validRequest_returnsEditConfig() throws Exception {
            EditConfigVO config = createMockEditConfig();
            when(documentEditService.buildEditConfig(eq(10L), eq(1L))).thenReturn(config);

            EditRequestDTO dto = new EditRequestDTO(10L);

            mockMvc.perform(post("/api/v1/document/edit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.cowApplied").value(false));
        }

        @Test
        @DisplayName("未认证时返回 401")
        void unauthenticated_returns401() throws Exception {
            EditRequestDTO dto = new EditRequestDTO(10L);

            mockMvc.perform(post("/api/v1/document/edit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/document/{userFileId}/history")
    class HistoryEndpoint {

        @Test
        @WithMockUser(username = "1")
        @DisplayName("有效请求返回版本列表")
        void validRequest_returnsVersionList() throws Exception {
            DocumentVersionVO v1 = new DocumentVersionVO(1, 1L, 1024L, LocalDateTime.now());
            DocumentVersionVO v2 = new DocumentVersionVO(2, 1L, 2048L, LocalDateTime.now());
            when(filePermissionService.canView(1L, 10L)).thenReturn(true);
            when(documentHistoryService.listVersions(10L)).thenReturn(List.of(v2, v1));

            mockMvc.perform(get("/api/v1/document/10/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].versionNumber").value(2))
                    .andExpect(jsonPath("$.data[1].versionNumber").value(1));
        }

        @Test
        @DisplayName("未认证时返回 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/document/10/history"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "1")
        @DisplayName("无版本时返回空数??)")

        void noVersions_returnsEmptyArray() throws Exception {
            when(filePermissionService.canView(1L, 10L)).thenReturn(true);
            when(documentHistoryService.listVersions(10L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/document/10/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    private PreviewConfigVO createMockPreviewConfig() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setDocserviceApiUrl("http://localhost:8090/api.js");
        config.setToken("mock-token");
        
        PreviewConfigVO.DocumentConfig doc = new PreviewConfigVO.DocumentConfig();
        doc.setFileType("docx");
        doc.setDocType("word");
        doc.setKey("doc-key");
        doc.setTitle("test.docx");
        doc.setUrl("http://download/test.docx");
        config.setDocument(doc);
        
        PreviewConfigVO.EditorConfig editorConfig = new PreviewConfigVO.EditorConfig();
        editorConfig.setMode("edit");
        config.setEditorConfig(editorConfig);
        
        return config;
    }

    private EditConfigVO createMockEditConfig() {
        EditConfigVO config = new EditConfigVO();
        config.setDocserviceApiUrl("http://localhost:8090/api.js");
        config.setToken("mock-token");
        config.setCowApplied(false);
        
        PreviewConfigVO.DocumentConfig doc = new PreviewConfigVO.DocumentConfig();
        doc.setFileType("docx");
        doc.setDocType("word");
        doc.setKey("doc-key");
        doc.setTitle("test.docx");
        doc.setUrl("http://download/test.docx");
        config.setDocument(doc);
        
        PreviewConfigVO.EditorConfig editorConfig = new PreviewConfigVO.EditorConfig();
        editorConfig.setMode("edit");
        config.setEditorConfig(editorConfig);
        
        return config;
    }
}
