## 1. 依赖与配置基础

- [x] 1.1 在 pom.xml 添加 ES 依赖：`co.elastic.clients:elasticsearch-java` + `jakarta.json:jakarta.json-api`
- [x] 1.2 在 application.yml 添加 ES 连接配置（`spring.elasticsearch.uris/username/password/connection-timeout/socket-timeout`）和搜索行为配置（`search.index-name/max-content-size/content-timeout/default-page-size`）
- [x] 1.3 创建 `SearchProperties`（`@ConfigurationProperties(prefix = "search")`），绑定搜索行为配置
- [x] 1.4 创建 `ElasticsearchConfig`，读取 `spring.elasticsearch.*` 配置创建 `ElasticsearchClient` Bean（使用 `RestClient` 传输层）
- [x] 1.5 创建 `SearchAsyncConfig`，配置 `searchIndexExecutor` 线程池（参数从 `async.search.*` 读取）
- [x] 1.6 在 application.yml 添加 `async.search` 线程池配置

## 2. 索引 Mapping 与初始化

- [x] 2.1 创建 `resources/elasticsearch/file-index-mapping.json`，定义索引 mapping（fileName/content 使用 IK 分词器）
- [x] 2.2 创建 `SearchIndexService`，实现索引自动初始化逻辑（启动时检查索引是否存在，不存在则创建）
- [x] 2.3 实现 `SearchIndexService.initIndex()` 方法：读取 mapping JSON、检查索引存在性、创建索引，ES 不可用时 log.error 不阻断启动

## 3. 异常与错误码

- [x] 3.1 创建 `SearchErrorCode` 枚举（SEARCH_UNAVAILABLE、INDEX_FAILED、INVALID_KEYWORD 等）
- [x] 3.2 创建 `SearchModuleException`（继承 `BusinessException`）

## 4. 文件变更事件（file 模块侧）

- [x] 4.1 在 `file/event/` 下创建 `FileChangedEvent`（继承 `ApplicationEvent`，包含 `userFileId` 和 `ChangeType` 枚举：CREATED/UPDATED/DELETED）
- [x] 4.2 在 `FileUploadService` 中注入 `ApplicationEventPublisher`，文件上传成功后发布 `FileChangedEvent(CREATED, userFileId)`（事务提交后发布）
- [x] 4.3 在 `FileOperationService` 中发布 `FileChangedEvent(UPDATED, userFileId)`（重命名/移动操作后）
- [x] 4.4 在 `FileOperationService` 中发布 `FileChangedEvent(DELETED, userFileId)`（删除操作后）

## 5. 索引同步监听

- [x] 5.1 创建 `FileChangedListener`（`@Component`），使用 `@EventListener` + `@Async("searchIndexExecutor")` 监听 `FileChangedEvent`
- [x] 5.2 实现 CREATED 事件处理：查询 `UserFile` + 关联 `FileBean`，构建索引文档，写入 ES
- [x] 5.3 实现 UPDATED 事件处理：更新 ES 中对应文档的 fileName、modifyTime
- [x] 5.4 实现 DELETED 事件处理：从 ES 中删除对应文档
- [x] 5.5 实现内容索引逻辑：判断文件类型（.txt/.md/.csv/.log）和大小限制，通过 UFOP StorageFactory 读取内容，超时控制
- [x] 5.6 所有 ES 写入操作 catch 异常后 log.warn（包含 userFileId 和错误原因），不抛出

## 6. 搜索查询服务

- [x] 6.1 创建 `SearchRequestDTO`（record：keyword、page、size、sortBy、sortOrder）
- [x] 6.2 创建 `SearchResultVO`（record：userFileId、fileName、extendName、filePath、fileSize、uploadTime、highlightFileName、highlightContent）
- [x] 6.3 创建 `SearchService`，实现搜索查询方法：构建 bool 查询（fileName match + wildcard、content match、userId term 过滤、isDir filter）
- [x] 6.4 实现分页逻辑：使用 `from/size`，返回 `totalHits.value()` 作为总数
- [x] 6.5 实现高亮：fileName 和 content 字段开启高亮，使用 `<em>` 标签
- [x] 6.6 实现排序：默认 `_score`，支持 `uploadTime` 和 `fileSize` 排序
- [x] 6.7 实现错误处理：catch ES 异常返回 `RestResult.fail("SEARCH_UNAVAILABLE", ...)`，不抛 NPE
- [x] 6.8 实现关键词校验：最大 100 字符，非空

## 7. 全量重建

- [x] 7.1 在 `SearchIndexService` 中实现 `rebuildAll()` 方法：分页查询所有 `deleteStatus=0` 的 UserFile，使用 bulk API 批量写入（每批 500 条）
- [x] 7.2 全量重建包含文本文件内容索引（复用内容索引逻辑）
- [x] 7.3 批次间 sleep 100ms 控制 ES 负载

## 8. Controller 层

- [x] 8.1 创建 `SearchController`（`/api/v1/search`），提供搜索端点 `GET /search`（参数校验 + 调用 SearchService + 响应包装）
- [x] 8.2 创建 `SearchAdminController`（`/api/v1/search/admin`），提供全量重建端点 `POST /rebuild`（需要 ADMIN 权限）
- [x] 8.3 实现健康检查端点 `GET /api/v1/search/health`，检查 ES 连通性

## 9. 测试

- [x] 9.1 编写 `SearchServiceTest`：测试搜索查询构建、分页、高亮、权限隔离、错误处理
- [x] 9.2 编写 `SearchIndexServiceTest`：测试索引初始化、增量同步、全量重建逻辑
- [x] 9.3 编写 `SearchControllerTest`：测试搜索 API 参数校验、正常响应、ES 不可用降级
- [x] 9.4 编写 `FileChangedListenerTest`：测试事件监听和索引同步

## 10. 配置验证与集成

- [x] 10.1 在 `application-test.yml` 添加 ES 测试配置
- [x] 10.2 验证所有配置项从 application.yml 正确读取，无硬编码
- [x] 10.3 验证 ES 不可用时应用正常启动，搜索返回友好提示，索引操作静默跳过
