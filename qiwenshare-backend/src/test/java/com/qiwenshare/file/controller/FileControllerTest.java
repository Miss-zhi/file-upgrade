package com.qiwenshare.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.file.dto.*;
import com.qiwenshare.file.service.FileOperationService;
import com.qiwenshare.file.vo.BatchOperationResultVO;
import com.qiwenshare.file.vo.FileDetailVO;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.file.vo.TreeNodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController 单元测试。
 */
@WebMvcTest(FileController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private FileOperationService fileOperationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any(jakarta.servlet.FilterChain.class));
    }

    @Nested
    @DisplayName("GET /api/v1/file/getfilelist")
    class ListFiles {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns file list page")
        void returnsFileList() throws Exception {
            FileListVO vo = new FileListVO(1L, "test.txt", "/", 1, 1024L, "txt",
                    LocalDateTime.now(), LocalDateTime.now(), 0);
            Page<FileListVO> page = new PageImpl<>(List.of(vo));
            when(fileOperationService.listFiles(eq(100L), any(FileListDTO.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/file/getfilelist")
                            .param("filePath", "/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.content[0].fileName").value("test.txt"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/file/getfilelist/bycategory")
    class ListFilesByCategory {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns files by category")
        void returnsByCategory() throws Exception {
            Page<FileListVO> page = new PageImpl<>(List.of());
            when(fileOperationService.listFilesByCategory(100L, "image", 0, 20)).thenReturn(page);

            mockMvc.perform(get("/api/v1/file/getfilelist/bycategory")
                            .param("category", "image"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/renamefile")
    class RenameFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("rename success")
        void renameSuccess() throws Exception {
            doNothing().when(fileOperationService).renameFile(any(RenameFileDTO.class), eq(100L));

            mockMvc.perform(post("/api/v1/file/renamefile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userFileId\":1,\"newName\":\"new-name.txt\"}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/movefile")
    class MoveFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("move success")
        void moveSuccess() throws Exception {
            doNothing().when(fileOperationService).moveFile(any(MoveFileDTO.class), eq(100L));

            mockMvc.perform(post("/api/v1/file/movefile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userFileId\":1,\"targetFolderId\":10}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/batchmovefile")
    class BatchMoveFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("batch move returns result")
        void batchMoveSuccess() throws Exception {
            BatchOperationResultVO result = new BatchOperationResultVO(3, List.of());
            when(fileOperationService.batchMoveFile(any(BatchMoveFileDTO.class), eq(100L)))
                    .thenReturn(result);

            mockMvc.perform(post("/api/v1/file/batchmovefile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userFileIds\":[1,2,3],\"targetFolderId\":10}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.successCount").value(3));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/copyfile")
    class CopyFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("copy success")
        void copySuccess() throws Exception {
            doNothing().when(fileOperationService).copyFile(any(CopyFileDTO.class), eq(100L));

            mockMvc.perform(post("/api/v1/file/copyfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userFileId\":1,\"targetFolderId\":20}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/batchcopyfile")
    class BatchCopyFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("batch copy returns result")
        void batchCopySuccess() throws Exception {
            BatchOperationResultVO result = new BatchOperationResultVO(2, List.of());
            when(fileOperationService.batchCopyFile(any(BatchCopyFileDTO.class), eq(100L)))
                    .thenReturn(result);

            mockMvc.perform(post("/api/v1/file/batchcopyfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userFileIds\":[1,2],\"targetFolderId\":20}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.successCount").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/createfold")
    class CreateFolder {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("create folder returns folder id")
        void createFolderSuccess() throws Exception {
            when(fileOperationService.createFolder(any(CreateFoldDTO.class), eq(100L)))
                    .thenReturn(500L);

            mockMvc.perform(post("/api/v1/file/createfold")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"folderName\":\"new-folder\",\"filePath\":\"/\"}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(500));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/file/createfile")
    class CreateFile {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("create file returns file id")
        void createFileSuccess() throws Exception {
            when(fileOperationService.createFile(any(CreateFileDTO.class), eq(100L)))
                    .thenReturn(600L);

            mockMvc.perform(post("/api/v1/file/createfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fileName\":\"readme.txt\",\"filePath\":\"/\"}")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(600));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/file/getfiledetail/{userFileId}")
    class GetFileDetail {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns file detail")
        void returnsDetail() throws Exception {
            FileDetailVO vo = new FileDetailVO(1L, "test.txt", "/test.txt", 1, 2048L,
                    "txt", "abc123", "local", LocalDateTime.now(), LocalDateTime.now());
            when(fileOperationService.getFileDetail(1L, 100L)).thenReturn(vo);

            mockMvc.perform(get("/api/v1/file/getfiledetail/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                    .andExpect(jsonPath("$.data.fileSize").value(2048));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/file/getfiletree")
    class GetFileTree {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns tree structure")
        void returnsTree() throws Exception {
            TreeNodeVO child = new TreeNodeVO(3L, "sub", "/parent/sub", List.of());
            TreeNodeVO root = new TreeNodeVO(2L, "parent", "/parent", List.of(child));
            when(fileOperationService.getFileTree(100L)).thenReturn(List.of(root));

            mockMvc.perform(get("/api/v1/file/getfiletree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].fileName").value("parent"))
                    .andExpect(jsonPath("$.data[0].children[0].fileName").value("sub"));
        }
    }
}
