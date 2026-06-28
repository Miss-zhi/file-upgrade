package com.qiwenshare.file.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSearchService {

    private static final String INDEX_NAME = "qiwen_file";

    private final ElasticsearchClient esClient;

    public void createIndex(String fileId, String fileName, String filePath, String fileType, Long fileSize, String userId) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("fileId", fileId);
            doc.put("fileName", fileName);
            doc.put("filePath", filePath);
            doc.put("fileType", fileType);
            doc.put("fileSize", fileSize);
            doc.put("userId", userId);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i ->
                    i.index(INDEX_NAME).id(fileId).document(doc));
            esClient.index(request);
        } catch (IOException e) {
            log.error("ES 索引创建失败: {}", e.getMessage());
        }
    }

    public void deleteIndex(String fileId) {
        try {
            esClient.delete(d -> d.index(INDEX_NAME).id(fileId));
        } catch (IOException e) {
            log.error("ES 索引删除失败: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> search(String keyword, String userId) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.term(t -> t.field("userId").value(userId)))
                                    .must(m -> m.multiMatch(mm -> mm
                                            .fields("fileName", "fileType")
                                            .query(keyword)))
                            ))
                    .highlight(h -> h
                            .fields("fileName", f -> f.preTags("<em>").postTags("</em>"))
                            .fields("fileType", f -> f.preTags("<em>").postTags("</em>"))),
                    Map.class);

            return response.hits().hits().stream().map(hit -> {
                Map<String, Object> result = new HashMap<>(hit.source());
                result.put("_score", hit.score());
                if (hit.highlight() != null) {
                    hit.highlight().forEach((k, v) ->
                            result.put(k + "Highlight", String.join("", v)));
                }
                return result;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("ES 搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
