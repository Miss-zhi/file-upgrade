## ADDED Requirements

### Requirement: 内嵌文件搜索
用户在文件列表工具栏的搜索输入框中输入关键词，按 Enter 或点击搜索图标触发搜索。系统 SHALL 调用后端 `GET /api/v1/search` 端点，传递 keyword、page、size、sortBy、sortOrder 参数。搜索结果 SHALL 原地替换当前文件列表展示，保持文件列表的视图模式（列表/网格/时间线）。

#### Scenario: 基本搜索
- **GIVEN** 用户在「全部文件」视图
- **WHEN** 用户输入 "报告" 并按 Enter
- **THEN** 调用 `GET /api/v1/search?keyword=报告&page=0&size=20`
- **AND** 文件列表替换为搜索结果
- **AND** 匹配的文件名以高亮形式展示

#### Scenario: 搜索分页
- **GIVEN** 搜索 "报告" 返回 50 条结果
- **WHEN** 用户点击第 2 页
- **THEN** 调用 `GET /api/v1/search?keyword=报告&page=1&size=20`
- **AND** 展示第 21-40 条结果

#### Scenario: 搜索排序
- **GIVEN** 搜索 "报告" 已展示结果
- **WHEN** 用户选择按「上传时间」降序排序
- **THEN** 调用 `GET /api/v1/search?keyword=报告&page=0&size=20&sortBy=uploadTime&sortOrder=desc`
- **AND** 结果按上传时间降序排列

#### Scenario: 清空搜索恢复文件列表
- **GIVEN** 用户正在查看搜索结果
- **WHEN** 用户清空搜索输入框
- **THEN** 恢复普通文件列表（调用原有 `getFileList` 接口）
- **AND** 分页和排序恢复为文件列表模式

#### Scenario: 搜索无结果
- **GIVEN** 用户输入 "xyzabc123"
- **WHEN** 搜索返回 0 条结果
- **THEN** 展示空状态提示「未找到匹配的文件」

#### Scenario: 搜索服务不可用
- **GIVEN** Elasticsearch 服务宕机
- **WHEN** 用户触发搜索
- **THEN** 后端返回 503
- **AND** 前端展示「搜索服务暂不可用，请稍后再试」错误提示

#### Scenario: 关键词为空不触发搜索
- **GIVEN** 搜索输入框为空
- **WHEN** 用户按 Enter
- **THEN** 不发起搜索请求
- **AND** 保持当前文件列表不变

### Requirement: 搜索高亮渲染
搜索结果中的文件名 SHALL 使用后端返回的 `highlightFileName`（含 `<em>` 高亮标签）渲染。前端 MUST 对高亮 HTML 做白名单过滤，仅允许 `<em>` 标签，防止 XSS。

#### Scenario: 高亮文件名展示
- **GIVEN** 搜索 "报告" 返回匹配文件
- **WHEN** 后端返回 `highlightFileName` 包含 `<em>报告</em>`
- **THEN** 前端过滤非 `<em>` 标签后通过 `v-html` 渲染
- **AND** 匹配关键词以主题色高亮显示

### Requirement: 搜索仅在全部文件视图可用
搜索 SHALL 仅在「全部文件」视图下可用。分类视图（图片/文档/视频/音乐/其他）和回收站/分享视图 SHALL NOT 展示搜索框，保持现有行为。

#### Scenario: 分类视图无搜索框
- **GIVEN** 用户在「图片」分类视图
- **WHEN** 渲染工具栏
- **THEN** 搜索输入框不展示
