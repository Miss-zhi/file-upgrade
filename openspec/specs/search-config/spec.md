## ADDED Requirements

### Requirement: ES 连接配置
系统 SHALL 支持通过 `application.yml` 配置 ES 连接参数。

#### Scenario: 配置 ES 连接 URI
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.uris: http://es-host:9200`
- **THEN** ElasticsearchClient Bean 使用配置的 URI 连接 ES

#### Scenario: 配置 ES 认证信息
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.username` 和 `spring.elasticsearch.password`
- **THEN** ElasticsearchClient Bean 使用 Basic Auth 认证连接 ES

#### Scenario: 配置 ES 超时参数
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.connection-timeout` 和 `spring.elasticsearch.socket-timeout`
- **THEN** ElasticsearchClient Bean 使用配置的超时值

#### Scenario: 超时参数支持多种单位
- **WHEN** 配置值带 `ms`、`s`、`m` 后缀或纯数字
- **THEN** 系统正确解析为毫秒值，不抛 NumberFormatException

### Requirement: 搜索行为配置
系统 SHALL 支持通过 `application.yml` 配置搜索行为参数。

#### Scenario: 配置索引名称
- **WHEN** 在 `application.yml` 中配置 `search.index-name: filesearch`
- **THEN** 所有索引操作和查询使用配置的索引名称，不硬编码

#### Scenario: 配置默认分页大小
- **WHEN** 在 `application.yml` 中配置 `search.default-page-size: 20`
- **THEN** 搜索 API 未指定 page size 时使用此默认值

### Requirement: ES 客户端 Bean 配置
系统 SHALL 使用 Elasticsearch Java API Client 8.x 创建 ES 客户端 Bean。

#### Scenario: 创建 ElasticsearchClient Bean
- **WHEN** 应用启动时
- **THEN** 系统创建 `co.elastic.clients.elasticsearch.ElasticsearchClient` Bean，使用 `RestClientBuilder` 传输层

#### Scenario: 不使用废弃的 RestHighLevelClient
- **WHEN** 检查 ES 客户端配置代码
- **THEN** 系统中不存在任何 `RestHighLevelClient` 的引用

### Requirement: 索引 Mapping 文件
系统 SHALL 在 `resources/elasticsearch/` 目录下维护索引 mapping JSON 文件。

#### Scenario: mapping 文件使用正确的 JSON 层级
- **WHEN** 检查 `src/main/resources/elasticsearch/file-index-mapping.json`
- **THEN** 文件顶层为 `{"properties": {...}}`，不包含外层 `mappings` 包装（`TypeMapping.of(m -> m.withJson(...))` 期望此格式）

#### Scenario: mapping 仅包含文件元数据字段
- **WHEN** 检查索引 mapping 定义
- **THEN** mapping 包含以下字段：`userFileId`(long)、`userId`(long)、`fileName`(text + IK)、`extendName`(keyword)、`filePath`(keyword)、`fileSize`(long)、`isDir`(integer)、`uploadTime`(date)、`modifyTime`(date)，不包含 `content` 字段

#### Scenario: fileName 使用 IK 分词器
- **WHEN** 索引 mapping 中定义了 `fileName` 字段
- **THEN** 该字段 MUST 使用 `ik_max_word` 作为索引分析器，`ik_smart` 作为搜索分析器
## ADDED Requirements

### Requirement: ES 连接配置
系统 SHALL 支持通过 `application.yml` 配置 ES 连接参数。

#### Scenario: 配置 ES 连接 URI
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.uris: http://es-host:9200`
- **THEN** ElasticsearchClient Bean 使用配置的 URI 连接 ES

#### Scenario: 配置 ES 认证信息
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.username` 和 `spring.elasticsearch.password`
- **THEN** ElasticsearchClient Bean 使用 Basic Auth 认证连接 ES

#### Scenario: 配置 ES 超时参数
- **WHEN** 在 `application.yml` 中配置 `spring.elasticsearch.connection-timeout` 和 `spring.elasticsearch.socket-timeout`
- **THEN** ElasticsearchClient Bean 使用配置的超时值

### Requirement: 搜索行为配置
系统 SHALL 支持通过 `application.yml` 配置搜索行为参数。

#### Scenario: 配置索引名称
- **WHEN** 在 `application.yml` 中配置 `search.index-name: filesearch`
- **THEN** 所有索引操作和查询使用配置的索引名称，不硬编码

#### Scenario: 配置内容索引大小限制
- **WHEN** 在 `application.yml` 中配置 `search.max-content-size: 5242880`（5MB）
- **THEN** 内容索引时超过此大小的文件跳过内容提取

#### Scenario: 配置内容提取超时
- **WHEN** 在 `application.yml` 中配置 `search.content-timeout: 10`（秒）
- **THEN** 内容提取超过此时间则跳过，仅索引文件元数据

#### Scenario: 配置默认分页大小
- **WHEN** 在 `application.yml` 中配置 `search.default-page-size: 20`
- **THEN** 搜索 API 未指定 page size 时使用此默认值

### Requirement: ES 客户端 Bean 配置
系统 SHALL 使用 Elasticsearch Java API Client 8.x 创建 ES 客户端 Bean。

#### Scenario: 创建 ElasticsearchClient Bean
- **WHEN** 应用启动时
- **THEN** 系统创建 `co.elastic.clients.elasticsearch.ElasticsearchClient` Bean，使用 `RestClient` 传输层

#### Scenario: 不使用废弃的 RestHighLevelClient
- **WHEN** 检查 ES 客户端配置代码
- **THEN** 系统中不存在任何 `RestHighLevelClient` 的引用

### Requirement: 索引 Mapping 文件
系统 SHALL 在 `resources/elasticsearch/` 目录下维护索引 mapping JSON 文件。

#### Scenario: mapping 文件存在于 resources 目录
- **WHEN** 检查 `src/main/resources/elasticsearch/` 目录
- **THEN** 存在 `file-index-mapping.json` 文件，包含完整的索引 mapping 定义

#### Scenario: mapping 配置变更不需要重新编译
- **WHEN** 修改 mapping JSON 文件中的分析器配置
- **THEN** 重新部署（不需要重新编译 Java 代码）即可生效
