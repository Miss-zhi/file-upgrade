## Requirements

1. 用户在文件列表工具栏的搜索输入框中输入关键词，按 Enter 或点击搜索图标触发搜索
2. 搜索调用后端 `GET /api/v1/search` 端点，传递 keyword、page、size、sortBy、sortOrder 参数
3. 搜索结果原地替换当前文件列表展示，保持文件列表的视图模式（列表/网格/时间线）
4. 搜索结果中的文件名使用后端返回的 `highlightFileName`（含 `<em>` 高亮标签）渲染
5. 搜索结果支持分页，分页控件与文件列表共用
6. 搜索结果支持按上传时间和文件大小排序
7. 用户清空搜索输入框时，恢复普通文件列表（调用原有的文件列表接口）
8. 搜索无结果时展示空状态提示
9. 搜索服务不可用（ES 宕机）时，展示友好的错误提示
10. 搜索仅在「全部文件」视图下可用（分类视图不展示搜索框，保持现有行为）

## Scenarios

### Scenario 1: 基本搜索
- Given 用户在「全部文件」视图
- When 用户输入 "报告" 并按 Enter
- Then 调用 `GET /api/v1/search?keyword=报告&page=0&size=20`
- And 文件列表替换为搜索结果
- And 匹配的文件名以高亮形式展示

### Scenario 2: 搜索分页
- Given 搜索 "报告" 返回 50 条结果
- When 用户点击第 2 页
- Then 调用 `GET /api/v1/search?keyword=报告&page=1&size=20`
- And 展示第 21-40 条结果

### Scenario 3: 搜索排序
- Given 搜索 "报告" 已展示结果
- When 用户选择按「上传时间」降序排序
- Then 调用 `GET /api/v1/search?keyword=报告&page=0&size=20&sortBy=uploadTime&sortOrder=desc`
- And 结果按上传时间降序排列

### Scenario 4: 清空搜索恢复文件列表
- Given 用户正在查看搜索结果
- When 用户清空搜索输入框
- Then 恢复普通文件列表（调用原有 `getFileList` 接口）
- And 分页和排序恢复为文件列表模式

### Scenario 5: 搜索无结果
- Given 用户输入 "xyzabc123"
- When 搜索返回 0 条结果
- Then 展示空状态提示「未找到匹配的文件」

### Scenario 6: 搜索服务不可用
- Given Elasticsearch 服务宕机
- When 用户触发搜索
- Then 后端返回 503
- And 前端展示「搜索服务暂不可用，请稍后再试」错误提示

### Scenario 7: 关键词为空不触发搜索
- Given 搜索输入框为空
- When 用户按 Enter
- Then 不发起搜索请求
- And 保持当前文件列表不变
