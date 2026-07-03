package com.qiwenshare.search.controller;

import com.qiwenshare.auth.common.GlobalExceptionHandler;
import com.qiwenshare.search.service.SearchIndexService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchAdminController 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SearchAdminControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private SearchAdminController searchAdminController;

    @Mock
    private SearchIndexService searchIndexService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(searchAdminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/search/admin/rebuild")
    class Rebuild {

        @Test
        @DisplayName("rebuild index success")
        void rebuildSuccess() throws Exception {
            doNothing().when(searchIndexService).rebuildAll();

            mockMvc.perform(post("/api/v1/search/admin/rebuild"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(searchIndexService).rebuildAll();
        }
    }
}
