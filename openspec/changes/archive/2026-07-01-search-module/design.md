## Context

当前搜索模块处于半完成状态：文件名搜索可用但有 8 个已知 Bug（详见 proposal.md），文件内容全文搜索从未生效。搜索逻辑直接写在 FileController 中，无独立 Service 层，ES 连接地址硬编码，索引无显式 mapping。

现有架构：
- 后端采用 Spring Boot 3.2.5 + JPA (Hibernate) 6.x + MyBatis-Plus 3.5.x
- 已有 Spring ApplicationEvent 解耦模式（`UserRegisteredEvent` → `UserRegisteredListener`）
- 已有独立线程池配置 `fileTaskExecutor`（`AsyncConfig`）
- 文件模块包含 `UserFile`（用户文件维度）和 `FileBean`（物理文件元数据）两个核心实体
- `UserFile.fileId` 关联 `FileBean.fileId`，文件夹 `fileId` 为 null
- 存储层通过 UFOP `StorageFactory` 抽象，支持多种存储后端

约束：
- ES 客户端必须使用 8.x Java API Client，禁止 RestHighLevelClient
- 搜索模块作为独立包 `com.qiwenshare.search`，不侵入 file 模块业务逻辑
- file 模块通过发布事件解耦，不直接依赖 search 模块

## Goals / Non-Goals

**Goals:**
- 建立独立的 `search/` 模块，包含 config、service、controller、dto、vo、event、listener 完整分层
- ES 连接完全配置化，索引 mapping 显式定义（IK 中文分词）
- 文件操作通过 Spring Event 异步同步索引，file 模块不直接依赖 search
- 搜索 API 支持分页、高亮、权限隔离（userId term 过滤）
- ES 不可用时优雅降级，不影响文件管理主流程
- 全量重建使用 bulk API，支持管理端触发

**Non-Goals:**
- 搜索建议/自动补全
- 搜索结果缓存
- 多租户搜索（userId 隔离已足够）
- 前端搜索 UI（属于 file-module-frontend 范围）
- 搜索分析/统计

## Decisions

### 1. ES 客户端：直接使用 Elasticsearch Java API Client 8.x

**选择：** `co.elastic.clients:elasticsearch-java` + `jakarta.json:jakarta.json-api`，手动配置 `ElasticsearchClient` Bean。

**替代方案：**
- `spring-boot-starter-data-elasticsearch`：Spring Data Elasticsearch 封装了 Repository 模式，但搜索场景查询复杂度高（bool 查询、高亮、聚合），Repository 模式反而增加抽象层。且 Spring Boot 3.2.x 的 auto-config 对 ES 8.x 支持有限。
- `RestHighLevelClient`：已废弃（AGENTS.md 红线禁止）。

**理由：** 直接使用 ES Java API Client 可以完全控制查询构建、高亮配置和 bulk 操作，与 Spring Boot 3.2.x 兼容性最好。通过 `@Configuration` 手动创建 Bean，读取 `application.yml` 中的配置。

### 2. 索引同步：Spring ApplicationEvent + @Async

**选择：** file 模块发布 `FileChangedEvent`（包含操作类型和 userFileId），search 模块监听并异步更新索引。

**替代方案：**
- file 模块直接调用 search Service：违反模块解耦原则，file 会依赖 search。
- 消息队列（RabbitMQ/Kafka）：引入额外基础设施，当前规模不需要。
- 数据库变更日志（CDC/Debezium）：过于重量级。

**理由：** 复用已有的 Spring Event 模式（与 `UserRegisteredEvent` 一致），file 模块只需注入 `ApplicationEventPublisher` 发布事件，零依赖 search 模块。search 模块用 `@Async("searchIndexExecutor")` 异步处理，不阻塞文件操作主流程。

**事件定义：**
```java
// file 模块定义事件（search 模块不依赖此事件类，只依赖事件数据）
public class FileChangedEvent extends ApplicationEvent {
    public enum ChangeType { CREATED, UPDATED, DELETED }
    private final Long userFileId;
    private final ChangeType changeType;
}
```

**异步线程池：** 独立 `searchIndexExecutor`，与 `fileTaskExecutor` 分离，避免索引 IO 影响文件任务。

### 3. 索引 Mapping：JSON 文件 + 启动时自动创建

**选择：** 在 `resources/elasticsearch/` 下放置 `file-index-mapping.json`，应用启动时检查索引是否存在，不存在则创建。

**Mapping 设计：**
```json
{
  "mappings": {
    "properties": {
      "userFileId": { "type": "long" },
      "userId":     { "type": "long" },
      "fileName":   { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "extendName": { "type": "keyword" },
      "filePath":   { "type": "keyword" },
      "fileSize":   { "type": "long" },
      "isDir":      { "type": "integer" },
      "content":    { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "uploadTime": { "type": "date" },
      "modifyTime": { "type": "date" }
    }
  }
}
```

**理由：** JSON 文件便于版本管理和 review，启动时自动创建避免手动操作。`ik_max_word` 用于索引（最大粒度分词），`ik_smart` 用于搜索（智能分词），这是 IK 分词器的最佳实践。

### 4. 搜索查询：bool 查询 + 高亮

**选择：** 使用 `bool` 查询组合 `fileName`（match + wildcard）和 `content`（match），`userId` 作为 `term` 过滤，`from/size` 分页。

**查询结构：**
```
bool:
  must:
    - userId: term（权限隔离，从 SecurityContext 获取）
  should:
    - fileName: match（IK 分词匹配）
    - fileName: wildcard（前缀/后缀匹配，boost 较低）
    - content: match（内容全文匹配）
  filter:
    - isDir: term(0)（排除文件夹）
```

**高亮：** `fileName` 和 `content` 字段开启高亮，使用 `<em>` 标签，`fragment_size` 对 content 设为 150。

**理由：** bool 查询提供灵活的匹配策略，wildcard 补充 IK 分词无法覆盖的场景（如精确前缀搜索）。userId 放在 filter 上下文（不计算评分），提升性能。

### 5. 降级策略：ES 不可用时搜索返回空 + 提示

**选择：**
- 搜索查询：catch ES 异常 → 返回 `RestResult.fail("SEARCH_UNAVAILABLE", "搜索服务暂不可用")`
- 索引操作：catch ES 异常 → `log.warn` 记录，静默跳过
- 健康检查：提供 `/api/v1/search/health` 端点，检查 ES 连通性

**理由：** 搜索是辅助功能，ES 故障不应影响文件管理核心流程。索引操作的静默失败可能导致索引与数据库不一致，后续可通过全量重建修复。

### 6. 模块包结构

```
com.qiwenshare.search/
├── config/
│   ├── ElasticsearchConfig.java      // ES 客户端 Bean 配置
│   ├── SearchProperties.java         // @ConfigurationProperties 搜索配置
│   └── SearchAsyncConfig.java        // searchIndexExecutor 线程池
├── controller/
│   ├── SearchController.java         // 搜索 API
│   └── SearchAdminController.java    // 管理端全量重建 API
├── service/
│   ├── SearchService.java            // 搜索查询服务
│   └── SearchIndexService.java       // 索引管理服务
├── listener/
│   └── FileChangedListener.java      // 文件变更事件监听
├── dto/
│   ├── SearchRequestDTO.java         // 搜索请求参数
│   └── SearchRebuildDTO.java         // 全量重建请求
├── vo/
│   ├── SearchResultVO.java           // 搜索结果
│   └── SearchHealthVO.java           // 健康检查结果
├── event/
│   └── FileChangedEvent.java         // 文件变更事件（放在 file 模块）
└── exception/
    ├── SearchErrorCode.java          // 搜索错误码枚举
    └── SearchModuleException.java    // 搜索模块异常
```

**注意：** `FileChangedEvent` 定义在 `file/` 模块（与 `UserRegisteredEvent` 在 auth 模块定义同理），search 模块监听它。

### 7. 内容索引策略

**选择：** 仅索引文本类文件（.txt, .md, .csv, .log），大小限制可配置（默认 5MB），超时 10 秒。

**实现：** 通过 UFOP `StorageFactory` 获取文件输入流，读取为文本写入 ES `content` 字段。非文本文件（图片、视频、Office 文档等）不索引内容。

**理由：** 文本文件内容索引成本低且价值高。Office 文档内容提取需要 Apache POI/Tika，复杂度高，作为后续迭代。5MB 限制防止大文件拖慢索引速度。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| ES 索引与 MySQL 数据不一致（异步写入失败） | 提供管理端全量重建 API，定期或按需修复。索引操作 log.warn 记录失败详情，便于排查。 |
| IK 分词器插件未安装导致 ES 启动失败 | 索引创建时 catch 异常并 log.error 提示检查 IK 插件。配置中提供 fallback analyzer（standard），ES 连接配置中可选择是否启用 IK。 |
| 全量重建期间 ES 负载过高 | bulk API 每批 500 条，批次间 sleep 100ms。管理端 API 需要 ADMIN 权限。 |
| 文件内容读取超时阻塞索引线程 | 内容提取设置 10 秒超时（可配置），超时则跳过内容索引，仅索引文件元数据。 |
| ES 集群不可用导致搜索完全失效 | 搜索端点返回友好提示，不影响文件浏览/上传/下载等核心功能。健康检查端点供监控告警。 |
| `FileChangedEvent` 发布在事务内，事务回滚后事件已发出 | 事件发布放在事务提交后（使用 `TransactionSynchronizationManager.registerSynchronization`），或在事务方法外发布。 |
