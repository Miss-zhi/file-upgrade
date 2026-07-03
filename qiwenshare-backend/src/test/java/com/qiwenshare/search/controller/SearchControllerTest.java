package com.qiwenshare.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiwenshare.auth.config.SecurityConfig;
import com.qiwenshare.auth.filter.JwtAuthenticationFilter;
import com.qiwenshare.auth.handler.AccessDeniedHandlerImpl;
import com.qiwenshare.auth.handler.AuthEntryPoint;
import com.qiwenshare.search.dto.SearchRequestDTO;
import com.qiwenshare.search.service.SearchIndexService;
import com.qiwenshare.search.service.SearchService;
import com.qiwenshare.search.vo.SearchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchController 单元测试。
 */
@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class, AccessDeniedHandlerImpl.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private SearchIndexService searchIndexService;

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
    @DisplayName("GET /api/v1/search")
    class Search {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns search results")
        void returnsResults() throws Exception {
            SearchResultVO vo = new SearchResultVO(1L, "test.txt", "txt", "/test.txt",
                    1024L, LocalDateTime.now(), LocalDateTime.now(), "<em>test</em>.txt");
            SearchService.SearchResult result = new SearchService.SearchResult(1, List.of(vo));
            when(searchService.search(any(SearchRequestDTO.class), eq(100L))).thenReturn(result);

            mockMvc.perform(get("/api/v1/search")
                            .param("keyword", "test")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.items[0].fileName").value("test.txt"));
        }

        @Test
        @WithMockUser(username = "100")
        @DisplayName("returns empty results")
        void returnsEmptyResults() throws Exception {
            SearchService.SearchResult result = new SearchService.SearchResult(0, List.of());
            when(searchService.search(any(SearchRequestDTO.class), eq(100L))).thenReturn(result);

            mockMvc.perform(get("/api/v1/search")
                            .param("keyword", "nonexistent")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/health")
    class Health {

        @Test
        @WithMockUser(username = "admin")
        @DisplayName("returns healthy when ES is available")
        void returnsHealthy() throws Exception {
            when(searchIndexService.isHealthy()).thenReturn(true);

            mockMvc.perform(get("/api/v1/search/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.available").value(true))
                    .andExpect(jsonPath("$.data.status").value("ES 可用"));
        }

        @Test
        @WithMockUser(username = "admin")
        @DisplayName("returns error code when ES is unavailable")
        void returnsError() throws Exception {
            when(searchIndexService.isHealthy()).thenReturn(false);

            mockMvc.perform(get("/api/v1/search/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(-1))
                    .andExpect(jsonPath("$.message").value("SEARCH_UNAVAILABLE"));
        }
    }
}
