package com.qiwenshare.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.file.common.FileGlobalExceptionHandler;
import com.qiwenshare.file.dto.ChunkUploadInitDTO;
import com.qiwenshare.file.dto.SpeedUploadDTO;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.service.FileDownloadService;
import com.qiwenshare.file.service.FileUploadService;
import com.qiwenshare.file.vo.UploadFileVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileTransferController 集成测试??
 *
 * <p>使用 MockMvc 验证上传/下载 API 端点??/p>
 */
@WebMvcTest(FileTransferController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class, FileGlobalExceptionHandler.class})
class FileTransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private FileUploadService fileUploadService;
    @MockBean private FileDownloadService fileDownloadService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any(jakarta.servlet.FilterChain.class));
    }

    @Nested
    @DisplayName("普通上??)")

    class Upload {

        @Test
        @DisplayName("未认证时返回 401")
        void upload_unauthenticated_returns401() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                    "text/plain", "hello".getBytes());

            mockMvc.perform(multipart("/api/v1/filetransfer/upload")
                            .file(file)
                            .param("filePath", "/"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("上传成功返回 UploadFileVO")
        @WithMockUser(username = "1")
        void upload_success_returnsUploadResult() throws Exception {
            UploadFileVO vo = new UploadFileVO(100L, "test.txt", 5L, "hash123", false);
            when(fileUploadService.uploadFile(any(), eq("/"), eq(1L))).thenReturn(vo);

            MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                    "text/plain", "hello".getBytes());

            mockMvc.perform(multipart("/api/v1/filetransfer/upload")
                            .file(file)
                            .param("filePath", "/")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userFileId").value(100))
                    .andExpect(jsonPath("$.data.fileName").value("test.txt"));
        }
    }

    @Nested
    @DisplayName("秒传")
    class SpeedUpload {

        @Test
        @DisplayName("秒传成功返回结果")
        @WithMockUser(username = "1")
        void speedUpload_success() throws Exception {
            UploadFileVO vo = new UploadFileVO(200L, "existing.txt", 1024L, "existingHash", true);
            when(fileUploadService.speedUpload(any(SpeedUploadDTO.class), eq(1L))).thenReturn(vo);

            SpeedUploadDTO dto = new SpeedUploadDTO("existing.txt", "/", 1024L, "existingHash");

            mockMvc.perform(post("/api/v1/filetransfer/upload/speed")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.isSpeed").value(true));
        }
    }

    @Nested
    @DisplayName("分片上传初始??)")

    class ChunkUploadInit {

        @Test
        @DisplayName("初始化分片上传返??taskId")
        @WithMockUser(username = "1")
        void initChunkUpload_returnsTaskId() throws Exception {
            when(fileUploadService.initChunkUpload(any(ChunkUploadInitDTO.class), eq(1L)))
                    .thenReturn("task-abc-123");

            ChunkUploadInitDTO dto = new ChunkUploadInitDTO("big.zip", "/", 15728640L, "hashXYZ", 4);

            mockMvc.perform(post("/api/v1/filetransfer/upload/chunk/init")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.message").value("task-abc-123"));
        }
    }
}
