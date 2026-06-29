package com.qiwenshare.file;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.ufop.operation.LocalStorageWriter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "user001")
class FilePreviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IFileService fileService;

    @Autowired
    private LocalStorageWriter writer;

    private static final String USER_ID = "user001";
    private String fileId;
    private String filePath;

    @BeforeEach
    void setUp() {
        filePath = "/preview-test-" + System.currentTimeMillis() + ".txt";
        FileBean file = fileService.upload("test.txt", filePath, 100L, "text/plain", USER_ID);
        fileId = file.getId();
        writer.write(filePath, "Hello Preview Test!");
    }

    @Test
    @DisplayName("预览文本文件返回内容")
    void testPreviewText() throws Exception {
        mockMvc.perform(get("/file/preview/text/" + fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Hello Preview Test!"));
    }

    @Test
    @DisplayName("预览流接口返回文件")
    void testPreviewStream() throws Exception {
        mockMvc.perform(get("/file/preview/" + fileId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    @DisplayName("预览不存在文件返回 404")
    void testPreviewNotFound() throws Exception {
        mockMvc.perform(get("/file/preview/nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("预览文本不存在返回错误")
    void testPreviewTextNotFound() throws Exception {
        mockMvc.perform(get("/file/preview/text/nonexistent-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
