# search-module — 全文搜索模块升级提案

## 背景

旧项目 search 模块处于半完成状态：文件名搜索可用，但存在 8 个已知 Bug，文件内容全文搜索被注释掉从未生效。搜索逻辑直接写在 FileController 中无 Service 层，ES 连接地址硬编码，索引无显式 mapping，全量重建无 bulk 优化。

### 旧系统已知问题清单

| # | 问题 | 严重性 |
|---|------|--------|
| 1 | ES 地址 `localhost:9200` 硬编码在 Java 代码中，无法通过配置覆盖 | 部署阻断 |
| 2 | 搜索结果总数传 `list.size()`（当前页条数）而非 `totalHits.value()` | 分页 Bug |
| 3 | IOException 后 `search` 变量为 null，后续 `search.hits()` 抛 NPE | 运行时崩溃 |
| 4 | `content` 字段写入逻辑被注释，内容全文搜索从未生效 | 功能缺失 |
| 5 | 索引 `filesearch` 无显式 mapping，中文分词效果差 | 搜索质量 |
| 6 | 全量重建逐条写入，无 bulk API，大文件量性能极差 | 性能 |
| 7 | ES 异常用 `log.debug` 静默吞掉，生产环境完全不可见 | 可观测性 |
| 8 | 搜索逻辑直接写在 FileController，无 Service 层 | 架构 |

## 升级目标

将搜索模块从"能用但有坑"升级为生产级全文搜索：独立的 Service 层、配置化 ES 连接、显式索引 mapping（IK 中文分词）、正确的分页和错误处理、bulk 批量索引、ES 不可用时优雅降级。

## Capabilities

### 1. search-index-management（搜索索引管理）

管理 ES 索引的生命周期：创建/更新 mapping、全量重建、增量同步。

**范围：**
- ES 连接配置化（host/port/auth/timeout 从 application.yml 读取）
- 显式定义索引 mapping：`fileName` 使用 IK 分词器、`userId` 为 keyword、`fileSize` 为 long、`isDir` 为 integer、`content` 使用 IK 分词器
- 索引初始化：应用启动时自动创建索引（如不存在）
- 增量同步：文件上传/创建/重命名/删除时异步更新索引（使用 Spring @Async + 独立线程池）
- 全量重建：管理端 API 触发，使用 bulk API 批量写入（每批 500 条）
- 文件内容索引：支持文本类文件（.txt, .md, .csv, .log）的内容提取和索引，大文件限制 5MB
- ES 不可用时索引操作静默失败（log.warn），不影响主流程
- 索引文档与 FileBean/UserFile 的数据映射

**约束：**
- 索引名称通过配置管理，不硬编码
- ES 写入失败必须 log.warn（不是 debug），包含 userFileId 和错误原因
- 全量重建必须使用 bulk API，禁止逐条写入
- 内容提取限制文件大小（可配置，默认 5MB），超时 10 秒
- 异步索引操作使用 BACKEND.md 中定义的独立 TaskExecutor

### 2. search-query（搜索查询服务）

提供文件名和内容搜索 API，支持分页、高亮、权限隔离。

**范围：**
- 独立 SearchService（不再写在 Controller 中）
- 搜索查询构造：bool 查询，文件名 match + wildcard，内容 match，userId term 过滤
- 高亮：fileName 和 content 字段均开启高亮，使用 `<em>` 标签
- 分页：正确使用 `from/size`，返回 `totalHits.value()` 作为总数
- 排序：默认相关性评分，支持按时间/大小排序
- 搜索结果 DTO：包含高亮片段、文件基本信息、匹配字段标识
- 错误处理：ES 查询异常时返回友好提示（不抛 NPE），log.error 记录详情
- ES 健康检查：提供搜索服务可用性端点

**约束：**
- 搜索逻辑必须在 SearchService 中，Controller 只做参数校验和响应包装
- 分页总数必须使用 `hits.totalHits.value()`，禁止使用当前页条数
- ES 查询异常必须 catch 并返回 RestResult.fail()，禁止吞异常或 NPE
- 用户只能搜索自己的文件（userId term 过滤，从 SecurityContext 获取）
- 搜索关键词最大长度 100 字符，防止正则 DoS

### 3. search-config（搜索配置）

ES 连接和搜索行为的可配置化。

**范围：**
- ES 连接配置：`spring.elasticsearch.uris`、`spring.elasticsearch.username`、`spring.elasticsearch.password`、`spring.elasticsearch.connection-timeout`、`spring.elasticsearch.socket-timeout`
- 搜索行为配置：`search.index-name`、`search.max-content-size`、`search.content-timeout`、`search.default-page-size`
- ES 客户端 Bean：手动配置 `ElasticsearchClient` Bean（使用 `co.elastic.clients:elasticsearch-java` + `RestClient` + `JacksonJsonpMapper`），不使用 `spring-boot-starter-data-elasticsearch`（功能受限）
- 索引 mapping 配置文件（JSON 格式放在 `resources/search/` 下）
- 独立 `searchTaskExecutor` Bean（与 `fileTaskExecutor` 分离，IO 密集型任务使用不同线程池，配置项 `async.search.*`）

**约束：**
- 所有 ES 连接参数从 application.yml 读取，禁止硬编码
- ES 客户端使用 Elasticsearch Java API Client 8.x（`co.elastic.clients:elasticsearch-java`），禁止 RestHighLevelClient
- 配置变更不需要重新编译
- `FileChangedEvent` 定义在 `file/event/` 包下（遵循现有约定：事件类在发布方模块），监听器 `SearchIndexListener` 在 `search/listener/` 包下

## 与现有模块的关系

| 模块 | 关系 | 说明 |
|------|------|------|
| file/ | 依赖 | 文件上传/删除/重命名后触发索引同步事件 |
| auth/ | 依赖 | 搜索时使用 SecurityContext 获取当前用户 userId |
| storage/ | 依赖 | 内容索引时通过 UFOP Reader 读取文件内容 |
| admin/ | 被依赖 | 管理端提供全量重建触发 API |

## 技术方案要点

### ES 客户端选型

使用 **Elasticsearch Java API Client 8.x**（`co.elastic.clients:elasticsearch-java`），手动配置 `ElasticsearchClient` Bean。不使用已废弃的 `RestHighLevelClient`（AGENTS.md 红线禁止），不使用 `spring-boot-starter-data-elasticsearch`（Spring Data ES Repository 抽象层功能受限且不灵活）。

### 索引同步策略

采用 Spring ApplicationEvent 解耦：文件操作发布 `FileChangedEvent`，搜索模块监听并异步更新索引。这样 file 模块不直接依赖 search 模块。

```
FileUploadService.upload() --publish--> FileChangedEvent(CREATED, userFileId)
                                              |
SearchIndexListener.onFileChanged() <--subscribe--|
    --> searchIndexService.indexAsync(userFileId)
```

### 降级策略

ES 不可用时，搜索端点返回 `RestResult.fail("SEARCH_UNAVAILABLE", "搜索服务暂不可用，请稍后再试")`，不影响文件管理的其他功能。索引操作静默跳过，log.warn 记录。

## 不在范围内

- 搜索建议/自动补全（可作为后续迭代）
- 搜索结果缓存（热点查询缓存，后续迭代）
- 多租户搜索（当前按 userId 隔离已足够）
- 搜索分析/统计（搜索热词、零结果关键词等）
- 前端搜索 UI（属于 file-module-frontend 或独立 search-frontend）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~15 个 Java 文件（config, service, listener, dto, vo, event） |
| 修改文件 | file 模块添加 FileChangedEvent 发布（~3 处） |
| 新增依赖 | `co.elastic.clients:elasticsearch-java` + `jakarta.json:jakarta.json-api` |
| Flyway | V7 无需新表（ES 索引不占 MySQL 空间） |
| 外部依赖 | 需要 Elasticsearch 8.x 实例 |
