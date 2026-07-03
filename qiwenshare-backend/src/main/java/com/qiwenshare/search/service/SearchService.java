package com.qiwenshare.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.qiwenshare.search.config.SearchProperties;
import com.qiwenshare.search.dto.SearchRequestDTO;
import com.qiwenshare.search.exception.SearchErrorCode;
import com.qiwenshare.search.exception.SearchModuleException;
import com.qiwenshare.search.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索查询服务。
 *
 * <p>提供文件名搜索，支持分页、高亮、权限隔离。
 * 仅搜索文件元数据，内容搜索由 document 模块负责。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchClient esClient;
    private final SearchProperties searchProperties;

    /**
     * 搜索文件。
     *
     * @param dto    搜索请求
     * @param userId 当前用户 ID（权限隔离）
     * @return 搜索结果列表和总数
     */
    public SearchResult search(SearchRequestDTO dto, Long userId) {
        try {
            String indexName = searchProperties.getIndexName();
            int size = dto.size() != null ? dto.size() : searchProperties.getDefaultPageSize();

            // 构建查询
            Query query = buildQuery(dto.keyword(), userId);

            // 构建搜索请求（仅高亮 fileName）
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(indexName)
                    .query(query)
                    .from(dto.page() * size)
                    .size(size)
                    .highlight(h -> h
                            .fields("fileName", f -> f.preTags("<em>").postTags("</em>"))
                    );

            // 排序
            if (StringUtils.hasText(dto.sortBy())) {
                SortOrder order = "asc".equalsIgnoreCase(dto.sortOrder()) ? SortOrder.Asc : SortOrder.Desc;
                requestBuilder.sort(s -> s.field(f -> f.field(dto.sortBy()).order(order)));
            }

            SearchResponse<?> response = esClient.search(requestBuilder.build(), Map.class);

            // 解析结果
            TotalHits totalHits = response.hits().total();
            long total = totalHits != null ? totalHits.value() : 0;

            List<SearchResultVO> results = new ArrayList<>();
            for (Hit<?> hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) hit.source();
                if (source == null) continue;

                String highlightFileName = null;

                // 解析高亮
                if (hit.highlight() != null) {
                    List<String> fileNameHighlights = hit.highlight().get("fileName");
                    if (fileNameHighlights != null && !fileNameHighlights.isEmpty()) {
                        highlightFileName = fileNameHighlights.get(0);
                    }
                }

                SearchResultVO vo = new SearchResultVO(
                        toLong(source.get("userFileId")),
                        toString(source.get("fileName")),
                        toString(source.get("extendName")),
                        toString(source.get("filePath")),
                        toLong(source.get("fileSize")),
                        toLocalDateTime(source.get("uploadTime")),
                        toLocalDateTime(source.get("modifyTime")),
                        highlightFileName
                );
                results.add(vo);
            }

            return new SearchResult(total, results);

        } catch (Exception e) {
            log.error("搜索查询失败: keyword={}, userId={}", dto.keyword(), userId, e);
            throw new SearchModuleException(SearchErrorCode.SEARCH_UNAVAILABLE, e);
        }
    }

    /**
     * 构建查询：userId 权限隔离 + 排除文件夹 + 文件名 match。
     *
     * <p>IK 分词器已覆盖中文子串匹配，无需 wildcard 避免大索引性能隐患。</p>
     */
    private Query buildQuery(String keyword, Long userId) {
        return BoolQuery.of(b -> b
                // 权限隔离：必须匹配 userId
                .filter(f -> f.term(t -> t.field("userId").value(userId)))
                // 排除文件夹
                .filter(f -> f.term(t -> t.field("isDir").value(0)))
                // 文件名搜索（IK 分词已覆盖中文）
                .should(s -> s.match(m -> m.field("fileName").query(keyword).boost(2.0f)))
                .should(s -> s.match(m -> m.field("extendName").query(keyword).boost(1.0f)))
                .minimumShouldMatch("1")
        )._toQuery();
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String toString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        try {
            return LocalDateTime.parse(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 搜索结果封装。
     */
    public record SearchResult(long total, List<SearchResultVO> items) {
    }
}
