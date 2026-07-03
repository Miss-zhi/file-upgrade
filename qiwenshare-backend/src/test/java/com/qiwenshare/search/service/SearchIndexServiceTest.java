package com.qiwenshare.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.qiwenshare.search.config.SearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * SearchIndexService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private SearchProperties searchProperties;

    @Mock
    private com.qiwenshare.file.repository.UserFileRepository userFileRepository;

    @Mock
    private com.qiwenshare.file.repository.FileBeanRepository fileBeanRepository;

    private SearchIndexService searchIndexService;

    @BeforeEach
    void setUp() {
        searchIndexService = new SearchIndexService(
                esClient, searchProperties, userFileRepository, fileBeanRepository);
    }

    @Test
    @DisplayName("索引已存在时跳过创建")
    void initIndex_indexExists_skipsCreation() throws Exception {
        // Given
        when(searchProperties.getIndexName()).thenReturn("filesearch");
        when(esClient.indices()).thenReturn(mockIndicesClient());
        BooleanResponse boolResp = mock(BooleanResponse.class);
        when(boolResp.value()).thenReturn(true);
        when(esClient.indices().exists(any(ExistsRequest.class)))
                .thenReturn(boolResp);

        // When
        searchIndexService.initIndex();

        // Then
        verify(esClient.indices(), never()).create(any(CreateIndexRequest.class));
    }

    @Test
    @DisplayName("ES 不可用时不阻断启??)")

    void initIndex_esUnavailable_doesNotThrow() throws Exception {
        // Given
        when(searchProperties.getIndexName()).thenReturn("filesearch");
        when(esClient.indices()).thenThrow(new RuntimeException("ES unavailable"));

        // When & Then
        searchIndexService.initIndex();
    }

    private co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient mockIndicesClient() {
        return mock(co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient.class);
    }
}
