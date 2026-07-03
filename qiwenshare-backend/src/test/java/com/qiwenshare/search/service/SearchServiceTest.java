package com.qiwenshare.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.qiwenshare.search.config.SearchProperties;
import com.qiwenshare.search.dto.SearchRequestDTO;
import com.qiwenshare.search.exception.SearchModuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * SearchService 单元测试??
 *
 * <p>由于 ES Hit.source() ??final 方法无法 mock??
 * 正常解析路径通过集成测试覆盖，此处聚焦异常路径和边界条件??/p>
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private SearchProperties searchProperties;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(esClient, searchProperties);
    }

    @Nested
    @DisplayName("搜索查询")
    class SearchQuery {

        @Test
        @DisplayName("ES 异常时抛??SearchModuleException")
        void search_esThrows_throwsSearchModuleException() throws Exception {
            // Given
            when(searchProperties.getIndexName()).thenReturn("filesearch");
            when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), any(Class.class)))
                    .thenThrow(new RuntimeException("ES connection failed"));

            SearchRequestDTO dto = new SearchRequestDTO("test", 0, 20, null, null);

            // When & Then
            assertThatThrownBy(() -> searchService.search(dto, 1L))
                    .isInstanceOf(SearchModuleException.class);
        }

        @Test
        @DisplayName("空结果返回总数 0（hits 为空列表??)")

        void search_noResults_returnsZeroTotal() throws Exception {
            // Given
            when(searchProperties.getIndexName()).thenReturn("filesearch");

            // �??mock 响应：total=0, hits 为空
            @SuppressWarnings("unchecked")
            SearchResponse<Map> mockResp = mock(SearchResponse.class, withSettings().lenient());
            @SuppressWarnings("unchecked")
            HitsMetadata<Map> mockHits = mock(HitsMetadata.class, withSettings().lenient());
            TotalHits totalHits = mock(TotalHits.class, withSettings().lenient());

            when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), any(Class.class)))
                    .thenReturn((SearchResponse) mockResp);
            when(mockResp.hits()).thenReturn((HitsMetadata) mockHits);
            when(mockHits.total()).thenReturn(totalHits);
            when(totalHits.value()).thenReturn(0L);
            when(mockHits.hits()).thenReturn(java.util.List.of());

            SearchRequestDTO dto = new SearchRequestDTO("nonexistent", 0, 20, null, null);

            // When
            SearchService.SearchResult result = searchService.search(dto, 1L);

            // Then
            assertThat(result.total()).isEqualTo(0L);
            assertThat(result.items()).isEmpty();
        }

        @Test
        @DisplayName("total ??null 时返回总数 0")
        void search_nullTotal_returnsZero() throws Exception {
            // Given
            when(searchProperties.getIndexName()).thenReturn("filesearch");

            @SuppressWarnings("unchecked")
            SearchResponse<Map> mockResp = mock(SearchResponse.class, withSettings().lenient());
            @SuppressWarnings("unchecked")
            HitsMetadata<Map> mockHits = mock(HitsMetadata.class, withSettings().lenient());

            when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), any(Class.class)))
                    .thenReturn((SearchResponse) mockResp);
            when(mockResp.hits()).thenReturn((HitsMetadata) mockHits);
            when(mockHits.total()).thenReturn(null);
            when(mockHits.hits()).thenReturn(java.util.List.of());

            SearchRequestDTO dto = new SearchRequestDTO("test", 0, 20, null, null);

            // When
            SearchService.SearchResult result = searchService.search(dto, 1L);

            // Then
            assertThat(result.total()).isEqualTo(0L);
            assertThat(result.items()).isEmpty();
        }
    }
}
