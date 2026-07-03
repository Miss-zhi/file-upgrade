## ADDED Requirements

### Requirement: ES 连接配置化
系统 SHALL 从 `application.yml` 读取所有 ES 连接参数（uris、username、password、connection-timeout、socket-timeout），禁止在 Java 代码中硬编码任何 ES 地址或端口。

#### Scenario: 从配置文件加载 ES 连接参数
- **WHEN** 应用启动时读取 `spring.elasticsearch.uris`、`spring.elasticsearch.username`、`spring.elasticsearch.password`、`spring.elasticsearch.connection-timeout`、`spring.elasticsearch.socket-timeout`
- **THEN** 系统使用这些配置创建 `ElasticsearchClient` Bean，不依赖任何硬编码的默认值

#### Scenario: 缺少 ES 认证配置时使用无认证连接
- **WHEN** `spring.elasticsearch.username` 和 `spring.elasticsearch.password` 未配置
- **THEN** 系统创建无认证的 `ElasticsearchClient`，仅使用 `uris` 连接

### Requirement: 索引 Mapping 显式定义
系统 SHALL 在 `resources/elasticsearch/` 下维护 JSON 格式的索引 mapping 文件，定义所有字段的类型和分词器。

#### Scenario: mapping 文件包含所有必需字段
- **WHEN** 检查索引 mapping 定义
- **THEN** mapping MUST 包含以下字段：`userFileId`(long)、`userId`(long)、`fileName`(text + IK)、`extendName`(keyword)、`filePath`(keyword)、`fileSize`(long)、`isDir`(integer)、`uploadTime`(date)、`modifyTime`(date)
- **NOTE** mapping 不包含 `content` 字段，内容索引由 document 模块负责

#### Scenario: fileName 使用 IK 分词器
- **WHEN** 索引 mapping 中定义了 `fileName` 字段
- **THEN** 该字段 MUST 使用 `ik_max_word` 作为索引分析器，`ik_smart` 作为搜索分析器

#### Scenario: mapping JSON 使用正确层级
- **WHEN** 检查 `file-index-mapping.json` 文件结构
- **THEN** 顶层为 `{"properties": {...}}`，不包含外层 `mappings` 包装

### Requirement: 索引自动初始化
系统 SHALL 在应用启动时检查 ES 索引是否存在，如不存在则自动创建。

#### Scenario: 索引不存在时自动创建
- **WHEN** 应用启动且 ES 索引不存在
- **THEN** 系统读取 mapping JSON 文件并创建索引，log.info 记录创建成功

#### Scenario: 索引已存在时跳过创建
- **WHEN** 应用启动且 ES 索引已存在
- **THEN** 系统跳过索引创建，log.info 记录索引已存在

#### Scenario: ES 不可用时启动不阻断
- **WHEN** 应用启动时 ES 不可达
- **THEN** 系统 log.error 记录连接失败，应用正常启动不阻断

### Requirement: 增量索引同步
系统 SHALL 监听文件变更事件（`FileChangedEvent`），异步更新 ES 索引。

#### Scenario: 文件创建后索引新增文档
- **WHEN** file 模块发布 `FileChangedEvent(CREATED, userFileId)`
- **THEN** search 模块异步查询 `UserFile` 和关联的 `FileBean`，构建索引文档并写入 ES

#### Scenario: 文件变更后索引更新
- **WHEN** file 模块发布 `FileChangedEvent(UPDATED, userFileId)`
- **THEN** search 模块异步更新 ES 中对应文档

#### Scenario: 文件删除后索引移除
- **WHEN** file 模块发布 `FileChangedEvent(DELETED, userFileId)`
- **THEN** search 模块异步从 ES 中删除对应文档

#### Scenario: ES 写入失败时静默处理
- **WHEN** 索引同步过程中 ES 写入抛出异常
- **THEN** 系统 log.warn 记录失败（包含 userFileId 和错误原因），不抛出异常，不影响主流程

#### Scenario: Listener 不标注 @Async
- **WHEN** `FileChangedListener` 接收文件变更事件
- **THEN** Listener 本身不标注 `@Async`，由 Service 方法的 `@Async` 独自负责异步调度，避免双重线程池提交

### Requirement: 全量重建索引
系统 SHALL 提供管理端 API 触发全量索引重建，使用 bulk API 批量写入。

#### Scenario: 触发全量重建
- **WHEN** 管理员调用全量重建 API
- **THEN** 系统分页查询所有 `deleteStatus=0` 的 `UserFile` 记录，使用 bulk API 批量写入 ES，每批 500 条

#### Scenario: 全量重建使用 bulk API
- **WHEN** 全量重建执行批量写入
- **THEN** 系统 MUST 使用 ES bulk API，禁止逐条写入

#### Scenario: 全量重建批量查询 FileBean 避免 N+1
- **WHEN** 全量重建处理一批 UserFile
- **THEN** 系统先收集所有 fileId，使用 `fileBeanRepository.findAllById(fileIds)` 一次查出所有 FileBean，构建 `Map<Long, FileBean>` 内存查找，禁止在循环中逐条 findById

#### Scenario: 全量重建仅索引文件元数据
- **WHEN** 全量重建处理每个文件
- **THEN** 系统仅索引文件元数据（fileName、fileSize、filePath 等），不提取文件内容。内容索引由 document 模块负责

#### Scenario: 批次间控制 ES 负载
- **WHEN** 一批写入完成后
- **THEN** 系统 sleep 100ms 后继续下一批

### Requirement: 独立异步线程池
系统 SHALL 为索引同步操作配置独立的线程池 `searchIndexExecutor`，与 `fileTaskExecutor` 分离。

#### Scenario: 索引操作在独立线程池执行
- **WHEN** `SearchIndexService` 的 `@Async` 方法被调用
- **THEN** 索引操作在 `searchIndexExecutor` 线程池中异步执行，不阻塞文件操作主线程

#### Scenario: 线程池参数可配置
- **WHEN** 配置文件中设置 `async.search.core-pool-size`、`async.search.max-pool-size`、`async.search.queue-capacity`
- **THEN** `searchIndexExecutor` 使用配置的参数初始化

### Requirement: 搜索模块专属异常处理器
系统 SHALL 提供 `SearchGlobalExceptionHandler`（`@RestControllerAdvice`），专门处理 `SearchModuleException`。

#### Scenario: SearchModuleException 返回正确 HTTP 状态码
- **WHEN** `SearchModuleException(SEARCH_UNAVAILABLE)` 被抛出
- **THEN** 返回 HTTP 503 + 错误码，而非笼统的 HTTP 500

#### Scenario: INVALID_KEYWORD 返回 400
- **WHEN** `SearchModuleException(INVALID_KEYWORD)` 被抛出
- **THEN** 返回 HTTP 400 + 错误码
## ADDED Requirements

### Requirement: ES 连接配置化
系统 SHALL 从 `application.yml` 读取所有 ES 连接参数（uris、username、password、connection-timeout、socket-timeout），禁止在 Java 代码中硬编码任何 ES 地址或端口。

#### Scenario: 从配置文件加载 ES 连接参数
- **WHEN** 应用启动时读取 `spring.elasticsearch.uris`、`spring.elasticsearch.username`、`spring.elasticsearch.password`、`spring.elasticsearch.connection-timeout`、`spring.elasticsearch.socket-timeout`
- **THEN** 系统使用这些配置创建 `ElasticsearchClient` Bean，不依赖任何硬编码的默认值

#### Scenario: 缺少 ES 配置时使用合理默认值
- **WHEN** `spring.elasticsearch.username` 和 `spring.elasticsearch.password` 未配置
- **THEN** 系统创建无认证的 `ElasticsearchClient`，仅使用 `uris` 连接

### Requirement: 索引 Mapping 显式定义
系统 SHALL 在 `resources/elasticsearch/` 下维护 JSON 格式的索引 mapping 文件，定义所有字段的类型和分词器。

#### Scenario: mapping 文件包含所有必需字段
- **WHEN** 检查索引 mapping 定义
- **THEN** mapping MUST 包含以下字段：`userFileId`(long)、`userId`(long)、`fileName`(text + IK)、`extendName`(keyword)、`filePath`(keyword)、`fileSize`(long)、`isDir`(integer)、`content`(text + IK)、`uploadTime`(date)、`modifyTime`(date)

#### Scenario: fileName 和 content 使用 IK 分词器
- **WHEN** 索引 mapping 中定义了 `fileName` 和 `content` 字段
- **THEN** 这两个字段 MUST 使用 `ik_max_word` 作为索引分析器，`ik_smart` 作为搜索分析器

### Requirement: 索引自动初始化
系统 SHALL 在应用启动时检查 ES 索引是否存在，如不存在则自动创建。

#### Scenario: 索引不存在时自动创建
- **WHEN** 应用启动且 ES 索引不存在
- **THEN** 系统读取 mapping JSON 文件并创建索引，log.info 记录创建成功

#### Scenario: 索引已存在时跳过创建
- **WHEN** 应用启动且 ES 索引已存在
- **THEN** 系统跳过索引创建，log.info 记录索引已存在

#### Scenario: ES 不可用时启动不阻断
- **WHEN** 应用启动时 ES 不可达
- **THEN** 系统 log.error 记录连接失败，应用正常启动不阻断

### Requirement: 增量索引同步
系统 SHALL 监听文件变更事件（`FileChangedEvent`），异步更新 ES 索引。

#### Scenario: 文件创建后索引新增文档
- **WHEN** file 模块发布 `FileChangedEvent(CREATED, userFileId)`
- **THEN** search 模块异步查询 `UserFile` 和关联的 `FileBean`，构建索引文档并写入 ES

#### Scenario: 文件重命名后索引更新
- **WHEN** file 模块发布 `FileChangedEvent(UPDATED, userFileId)`
- **THEN** search 模块异步更新 ES 中对应文档的 `fileName`、`modifyTime` 字段

#### Scenario: 文件删除后索引移除
- **WHEN** file 模块发布 `FileChangedEvent(DELETED, userFileId)`
- **THEN** search 模块异步从 ES 中删除对应文档

#### Scenario: ES 写入失败时静默处理
- **WHEN** 索引同步过程中 ES 写入抛出异常
- **THEN** 系统 log.warn 记录失败（包含 userFileId 和错误原因），不抛出异常，不影响主流程

### Requirement: 全量重建索引
系统 SHALL 提供管理端 API 触发全量索引重建，使用 bulk API 批量写入。

#### Scenario: 触发全量重建
- **WHEN** 管理员调用全量重建 API
- **THEN** 系统分页查询所有 `deleteStatus=0` 的 `UserFile` 记录，使用 bulk API 批量写入 ES，每批 500 条

#### Scenario: 全量重建使用 bulk API
- **WHEN** 全量重建执行批量写入
- **THEN** 系统 MUST 使用 ES bulk API，禁止逐条写入

#### Scenario: 全量重建包含文件内容索引
- **WHEN** 全量重建遇到文本类文件（.txt, .md, .csv, .log）且文件大小不超过配置限制（默认 5MB）
- **THEN** 系统通过 UFOP StorageFactory 读取文件内容，写入 ES `content` 字段

#### Scenario: 全量重建内容提取超时
- **WHEN** 文件内容读取超过配置的超时时间（默认 10 秒）
- **THEN** 系统跳过该文件的内容索引，仅索引文件元数据，log.warn 记录超时

### Requirement: 独立异步线程池
系统 SHALL 为索引同步操作配置独立的线程池 `searchIndexExecutor`，与 `fileTaskExecutor` 分离。

#### Scenario: 索引操作在独立线程池执行
- **WHEN** `FileChangedListener` 处理文件变更事件
- **THEN** 索引操作在 `searchIndexExecutor` 线程池中异步执行，不阻塞文件操作主线程

#### Scenario: 线程池参数可配置
- **WHEN** 配置文件中设置 `async.search.core-pool-size`、`async.search.max-pool-size`、`async.search.queue-capacity`
- **THEN** `searchIndexExecutor` 使用配置的参数初始化
