## ADDED Requirements

### Requirement: 文件名搜索
系统 SHALL 提供独立的 `SearchService`，支持文件名全文搜索。

#### Scenario: 按文件名搜索
- **WHEN** 用户搜索关键词且匹配到文件名
- **THEN** 系统返回匹配的文件列表，使用 bool 查询组合 `fileName` match（IK 分词，boost 2.0）和 `extendName` match（boost 1.0）

#### Scenario: 不使用 wildcard 查询
- **WHEN** 搜索请求到达
- **THEN** 系统 MUST NOT 使用 `wildcard` 查询（IK 分词器已覆盖中文子串匹配，leading wildcard 在大索引上性能开销高）

#### Scenario: 搜索逻辑在 SearchService 中
- **WHEN** 搜索请求到达 SearchController
- **THEN** Controller 只做参数校验和响应包装，搜索逻辑 MUST 在 SearchService 中执行

### Requirement: 搜索权限隔离
系统 SHALL 确保用户只能搜索自己的文件，通过 `userId` term 过滤实现。

#### Scenario: 搜索结果仅包含当前用户文件
- **WHEN** 用户发起搜索请求
- **THEN** 系统从 SecurityContext 获取当前用户 userId，在 ES 查询中添加 `userId` term 过滤条件

#### Scenario: 用户 A 无法搜索到用户 B 的文件
- **WHEN** 用户 A 搜索关键词，该关键词也匹配用户 B 的文件
- **THEN** 搜索结果中不包含用户 B 的任何文件

### Requirement: 搜索结果分页
系统 SHALL 正确使用 ES `from/size` 实现分页，返回 `totalHits.value()` 作为总数。

#### Scenario: 分页返回正确的总数
- **WHEN** 用户搜索并请求第 2 页（page=2, size=20）
- **THEN** 系统返回 `totalHits.value()` 作为总记录数，当前页最多 20 条结果

#### Scenario: 分页参数使用 totalHits 而非 list.size()
- **WHEN** ES 查询返回结果
- **THEN** 系统 MUST 使用 `hits.totalHits.value()` 作为总数，禁止使用当前页 `list.size()`

### Requirement: 搜索结果高亮
系统 SHALL 对 `fileName` 字段开启高亮，使用 `<em>` 标签。

#### Scenario: 文件名高亮
- **WHEN** 搜索关键词匹配文件名
- **THEN** 返回结果中 `highlightFileName` 字段包含 `<em>` 标签包裹的匹配片段

### Requirement: 搜索排序
系统 SHALL 支持按相关性评分、上传时间、文件大小排序。

#### Scenario: 默认按相关性评分排序
- **WHEN** 用户搜索未指定排序参数
- **THEN** 系统按 ES 相关性评分（`_score`）降序排列结果

#### Scenario: 按上传时间排序
- **WHEN** 用户指定排序为 `uploadTime`
- **THEN** 系统按 `uploadTime` 字段排序（支持升序/降序）

#### Scenario: 按文件大小排序
- **WHEN** 用户指定排序为 `fileSize`
- **THEN** 系统按 `fileSize` 字段排序（支持升序/降序）

### Requirement: 搜索错误处理
系统 SHALL 在 ES 查询异常时通过 `SearchModuleException` 返回友好提示。

#### Scenario: ES 查询异常返回 SearchModuleException
- **WHEN** ES 查询过程中抛出异常（如连接超时、查询语法错误）
- **THEN** 系统抛出 `SearchModuleException(SEARCH_UNAVAILABLE)`，由 `SearchGlobalExceptionHandler` 返回 HTTP 503 + 友好错误信息

#### Scenario: ES 返回空结果时正确处理
- **WHEN** ES 返回空结果或 `totalHits` 为 null
- **THEN** 系统正确处理空值，返回 total=0 和空列表，不抛出 NullPointerException

### Requirement: 搜索关键词校验
系统 SHALL 通过 `@Valid` + DTO 注解限制搜索关键词。

#### Scenario: 关键词超过 100 字符被拒绝
- **WHEN** 用户提交超过 100 字符的搜索关键词
- **THEN** `@Size(max=100)` 校验失败，返回参数校验错误

#### Scenario: 空关键词被拒绝
- **WHEN** 用户提交空字符串或纯空白的搜索关键词
- **THEN** `@NotBlank` 校验失败，返回参数校验错误

#### Scenario: Controller 不做手动校验
- **WHEN** SearchController 接收搜索请求
- **THEN** Controller MUST NOT 手动检查 keyword 空值或长度，由 `@Valid` + DTO 注解统一处理

### Requirement: 搜索响应类型明确
系统 SHALL 使用明确的类型定义搜索响应。

#### Scenario: SearchResponse items 类型为 List<SearchResultVO>
- **WHEN** 搜索 API 返回结果
- **THEN** `SearchResponse` record 的 `items` 字段类型为 `List<SearchResultVO>`，不使用 `Object` 或原始类型

#### Scenario: SearchResultVO 包含完整元数据
- **WHEN** 搜索结果返回给前端
- **THEN** 每个结果包含 `userFileId`、`fileName`、`extendName`、`filePath`、`fileSize`、`uploadTime`、`modifyTime`、`highlightFileName`

### Requirement: 搜索服务健康检查
系统 SHALL 提供搜索服务可用性端点。

#### Scenario: ES 可用时返回健康状态
- **WHEN** 调用 `/api/v1/search/health` 且 ES 连通
- **THEN** 返回 `RestResult.success()` 包含 ES 集群状态信息

#### Scenario: ES 不可用时返回不可用状态
- **WHEN** 调用 `/api/v1/search/health` 且 ES 不可达
- **THEN** 返回 `RestResult.fail("SEARCH_UNAVAILABLE", "搜索服务不可用")`
## ADDED Requirements

### Requirement: 文件名和内容搜索
系统 SHALL 提供独立的 `SearchService`，支持文件名全文搜索和内容全文搜索。

#### Scenario: 按文件名搜索
- **WHEN** 用户搜索关键词且匹配到文件名
- **THEN** 系统返回匹配的文件列表，使用 bool 查询组合 `fileName` match（IK 分词）和 wildcard 匹配

#### Scenario: 按文件内容搜索
- **WHEN** 用户搜索关键词且匹配到文件内容
- **THEN** 系统返回匹配的文件列表，使用 `content` 字段 match 查询

#### Scenario: 搜索逻辑在 SearchService 中
- **WHEN** 搜索请求到达 SearchController
- **THEN** Controller 只做参数校验和响应包装，搜索逻辑 MUST 在 SearchService 中执行

### Requirement: 搜索权限隔离
系统 SHALL 确保用户只能搜索自己的文件，通过 `userId` term 过滤实现。

#### Scenario: 搜索结果仅包含当前用户文件
- **WHEN** 用户发起搜索请求
- **THEN** 系统从 SecurityContext 获取当前用户 userId，在 ES 查询中添加 `userId` term 过滤条件

#### Scenario: 用户 A 无法搜索到用户 B 的文件
- **WHEN** 用户 A 搜索关键词，该关键词也匹配用户 B 的文件
- **THEN** 搜索结果中不包含用户 B 的任何文件

### Requirement: 搜索结果分页
系统 SHALL 正确使用 ES `from/size` 实现分页，返回 `totalHits.value()` 作为总数。

#### Scenario: 分页返回正确的总数
- **WHEN** 用户搜索并请求第 2 页（page=2, size=20）
- **THEN** 系统返回 `totalHits.value()` 作为总记录数，当前页最多 20 条结果

#### Scenario: 分页参数使用 totalHits 而非 list.size()
- **WHEN** ES 查询返回结果
- **THEN** 系统 MUST 使用 `hits.totalHits.value()` 作为总数，禁止使用当前页 `list.size()`

### Requirement: 搜索结果高亮
系统 SHALL 对 `fileName` 和 `content` 字段开启高亮，使用 `<em>` 标签。

#### Scenario: 文件名高亮
- **WHEN** 搜索关键词匹配文件名
- **THEN** 返回结果中 `fileName` 字段包含 `<em>` 标签包裹的匹配片段

#### Scenario: 内容高亮带片段大小限制
- **WHEN** 搜索关键词匹配文件内容
- **THEN** 返回结果中 `content` 字段包含 `<em>` 标签包裹的匹配片段，`fragment_size` 为 150 字符

### Requirement: 搜索排序
系统 SHALL 支持按相关性评分、上传时间、文件大小排序。

#### Scenario: 默认按相关性评分排序
- **WHEN** 用户搜索未指定排序参数
- **THEN** 系统按 ES 相关性评分（`_score`）降序排列结果

#### Scenario: 按上传时间排序
- **WHEN** 用户指定排序为 `uploadTime`
- **THEN** 系统按 `uploadTime` 字段排序（支持升序/降序）

#### Scenario: 按文件大小排序
- **WHEN** 用户指定排序为 `fileSize`
- **THEN** 系统按 `fileSize` 字段排序（支持升序/降序）

### Requirement: 搜索错误处理
系统 SHALL 在 ES 查询异常时返回友好提示，不抛出 NPE。

#### Scenario: ES 查询异常返回友好错误
- **WHEN** ES 查询过程中抛出异常（如连接超时、查询语法错误）
- **THEN** 系统返回 `RestResult.fail("SEARCH_UNAVAILABLE", "搜索服务暂不可用，请稍后再试")`，log.error 记录异常详情

#### Scenario: ES 查询异常不抛出 NPE
- **WHEN** ES 返回空结果或异常响应
- **THEN** 系统正确处理空值，不抛出 NullPointerException

### Requirement: 搜索关键词校验
系统 SHALL 限制搜索关键词最大长度为 100 字符。

#### Scenario: 关键词超过 100 字符被拒绝
- **WHEN** 用户提交超过 100 字符的搜索关键词
- **THEN** 系统返回参数校验错误，不执行搜索

#### Scenario: 空关键词被拒绝
- **WHEN** 用户提交空字符串或纯空白的搜索关键词
- **THEN** 系统返回参数校验错误

### Requirement: 搜索服务健康检查
系统 SHALL 提供搜索服务可用性端点。

#### Scenario: ES 可用时返回健康状态
- **WHEN** 调用 `/api/v1/search/health` 且 ES 连通
- **THEN** 返回 `RestResult.success()` 包含 ES 集群状态信息

#### Scenario: ES 不可用时返回不可用状态
- **WHEN** 调用 `/api/v1/search/health` 且 ES 不可达
- **THEN** 返回 `RestResult.fail("SEARCH_UNAVAILABLE", "搜索服务不可用")`
