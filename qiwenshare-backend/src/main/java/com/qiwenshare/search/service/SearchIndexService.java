package com.qiwenshare.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.search.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搜索索引管理服务。
 *
 * <p>负责 ES 索引的生命周期管理：初始化、增量同步、全量重建。
 * 仅索引文件元数据（文件名、大小、路径等），不索引文件内容。
 * 内容搜索由 document 模块负责。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private static final int BULK_SIZE = 500;
    private static final int BULK_SLEEP_MS = 100;

    private final ElasticsearchClient esClient;
    private final SearchProperties searchProperties;
    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;

    /**
     * 应用启动后初始化索引。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            String indexName = searchProperties.getIndexName();
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("ES 索引已存在: {}", indexName);
                return;
            }

            // 读取 mapping JSON（TypeMapping.of(m -> m.withJson(...)) 期望 {"properties": {...}} 层级）
            String mappingJson = loadMappingJson();
            TypeMapping mapping = TypeMapping.of(m -> m.withJson(new java.io.StringReader(mappingJson)));

            esClient.indices().create(CreateIndexRequest.of(c -> c.index(indexName).mappings(mapping)));
            log.info("ES 索引创建成功: {}", indexName);
        } catch (Exception e) {
            log.error("ES 索引初始化失败，搜索功能可能不可用", e);
        }
    }

    /**
     * 索引单个文件（增量同步）。
     *
     * @param userFileId 用户文件 ID
     */
    @Async("searchIndexExecutor")
    public void indexAsync(Long userFileId) {
        try {
            UserFile userFile = userFileRepository.findById(userFileId).orElse(null);
            if (userFile == null || userFile.getDeleteStatus() != 0) {
                return;
            }

            Map<String, Object> doc = buildDocument(userFile, null);
            esClient.index(i -> i
                    .index(searchProperties.getIndexName())
                    .id(String.valueOf(userFileId))
                    .document(doc));
            log.debug("索引文档成功: userFileId={}", userFileId);
        } catch (Exception e) {
            log.warn("索引文档失败: userFileId={}, 原因={}", userFileId, e.getMessage());
        }
    }

    /**
     * 更新索引文档（文件名/路径变更）。
     *
     * @param userFileId 用户文件 ID
     */
    @Async("searchIndexExecutor")
    public void updateAsync(Long userFileId) {
        try {
            UserFile userFile = userFileRepository.findById(userFileId).orElse(null);
            if (userFile == null || userFile.getDeleteStatus() != 0) {
                deleteAsync(userFileId);
                return;
            }

            Map<String, Object> doc = buildDocument(userFile, null);
            esClient.index(i -> i
                    .index(searchProperties.getIndexName())
                    .id(String.valueOf(userFileId))
                    .document(doc));
            log.debug("更新索引文档成功: userFileId={}", userFileId);
        } catch (Exception e) {
            log.warn("更新索引文档失败: userFileId={}, 原因={}", userFileId, e.getMessage());
        }
    }

    /**
     * 删除索引文档。
     *
     * @param userFileId 用户文件 ID
     */
    @Async("searchIndexExecutor")
    public void deleteAsync(Long userFileId) {
        try {
            esClient.delete(d -> d
                    .index(searchProperties.getIndexName())
                    .id(String.valueOf(userFileId)));
            log.debug("删除索引文档成功: userFileId={}", userFileId);
        } catch (Exception e) {
            log.warn("删除索引文档失败: userFileId={}, 原因={}", userFileId, e.getMessage());
        }
    }

    /**
     * 全量重建索引。
     *
     * <p>批量查询 FileBean 获取 fileSize，避免 N+1 查询。</p>
     */
    public void rebuildAll() {
        String indexName = searchProperties.getIndexName();
        int page = 0;
        int totalIndexed = 0;

        log.info("开始全量重建索引...");

        while (true) {
            Page<UserFile> userFiles = userFileRepository.findByDeleteStatus(0, PageRequest.of(page, BULK_SIZE));
            if (userFiles.isEmpty()) {
                break;
            }

            List<UserFile> batch = userFiles.getContent();

            // 批量查询 FileBean，避免 N+1
            Set<Long> fileIds = batch.stream()
                    .map(UserFile::getFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, FileBean> fileBeanMap = fileIds.isEmpty()
                    ? Map.of()
                    : fileBeanRepository.findAllById(fileIds).stream()
                            .collect(Collectors.toMap(FileBean::getFileId, Function.identity()));

            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (UserFile userFile : batch) {
                try {
                    Map<String, Object> doc = buildDocument(userFile, fileBeanMap);
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(indexName)
                                    .id(String.valueOf(userFile.getUserFileId()))
                                    .document(doc)));
                } catch (Exception e) {
                    log.warn("构建索引文档失败: userFileId={}, 原因={}", userFile.getUserFileId(), e.getMessage());
                }
            }

            try {
                BulkResponse response = esClient.bulk(bulkBuilder.build());
                if (response.errors()) {
                    for (BulkResponseItem item : response.items()) {
                        if (item.error() != null) {
                            log.warn("Bulk 索引失败: id={}, error={}", item.id(), item.error().reason());
                        }
                    }
                }
                totalIndexed += batch.size();
            } catch (Exception e) {
                log.error("Bulk 索引请求失败: page={}", page, e);
            }

            try {
                Thread.sleep(BULK_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            page++;
        }

        log.info("全量重建索引完成: 共索引 {} 个文档", totalIndexed);
    }

    /**
     * 检查 ES 健康状态。
     *
     * @return true 如果 ES 可用
     */
    public boolean isHealthy() {
        try {
            return esClient.ping().value();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建 ES 索引文档（仅元数据，不含文件内容）。
     *
     * @param userFile     用户文件实体
     * @param fileBeanMap  FileBean 映射（全量重建时传入，增量同步时可传 null）
     * @return 文档 Map
     */
    private Map<String, Object> buildDocument(UserFile userFile, Map<Long, FileBean> fileBeanMap) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("userFileId", userFile.getUserFileId());
        doc.put("userId", userFile.getUserId());
        doc.put("fileName", userFile.getFileName());
        doc.put("extendName", userFile.getExtendName());
        doc.put("filePath", userFile.getFilePath());
        doc.put("isDir", userFile.getFileType());
        doc.put("uploadTime", userFile.getUploadTime() != null ? userFile.getUploadTime().toString() : null);
        doc.put("modifyTime", userFile.getModifyTime() != null ? userFile.getModifyTime().toString() : null);

        long fileSize = 0L;
        if (userFile.getFileId() != null && userFile.getFileType() == 1) {
            if (fileBeanMap != null) {
                FileBean fileBean = fileBeanMap.get(userFile.getFileId());
                if (fileBean != null) {
                    fileSize = fileBean.getFileSize();
                }
            } else {
                fileBeanRepository.findById(userFile.getFileId())
                        .ifPresent(fb -> doc.put("fileSize", fb.getFileSize()));
                if (!doc.containsKey("fileSize")) {
                    doc.put("fileSize", 0L);
                }
                return doc;
            }
        }
        doc.put("fileSize", fileSize);

        return doc;
    }

    private String loadMappingJson() {
        try (InputStream is = getClass().getResourceAsStream("/elasticsearch/file-index-mapping.json")) {
            if (is == null) {
                throw new IllegalStateException("Mapping 文件不存在: /elasticsearch/file-index-mapping.json");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取 mapping 文件失败", e);
        }
    }
}
