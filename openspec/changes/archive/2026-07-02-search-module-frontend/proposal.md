## Why

后端搜索模块已完成（Elasticsearch 全文检索，3 个端点），前端仍停留在 stub 状态——`FileView.vue` 的 `handleSearch()` 直接丢弃关键词，`OperationMenu.vue` 的搜索输入框虽然能捕获输入但事件无法落地。旧项目通过 `/file/search` 端点实现了内嵌式文件搜索（搜索结果原地替换文件列表，高亮匹配文件名），新项目需要对齐此体验并对接新的 `/api/v1/search` 端点。

## What Changes

- 新增 `api/search.ts`，封装搜索（`GET /api/v1/search`）和健康检查（`GET /api/v1/search/health`）两个端点
- 新增 `types/search.ts`，定义 `SearchResultVO`、`SearchRequestDTO`、`SearchResponse` 接口
- 新增 `composables/useSearch.ts`，封装搜索状态（关键词、结果列表、分页、排序、加载状态）
- 修改 `FileView.vue`：将 stub `handleSearch()` 替换为调用搜索 composable，搜索结果原地替换文件列表；清空搜索时恢复普通文件列表
- 修改 `components/file/OperationMenu.vue`：确保搜索事件正确传递关键词到 FileView
- 搜索结果支持高亮文件名渲染（`highlightFileName` 字段包含 `<em>` 标签）
- 搜索结果支持排序（按上传时间 / 文件大小）和分页

## Capabilities

### New Capability

- `file-search`: 文件全文搜索——在文件列表工具栏输入关键词，调用 ES 搜索接口，原地展示搜索结果（高亮文件名、分页、排序），清空搜索恢复普通文件列表

### Modified Capabilities

（无。搜索为全新能力，不修改已有 spec。）

## Impact

- **API 层**：新增 `api/search.ts`
- **类型定义**：新增 `types/search.ts`
- **Composable**：新增 `composables/useSearch.ts`
- **FileView.vue**：`handleSearch()` 从 stub 改为调用搜索逻辑，增加搜索结果渲染路径
- **OperationMenu.vue**：事件传递可能需要微调
- **文件展示组件**：`FileTable.vue`、`FileGrid.vue` 需要支持高亮文件名渲染（`v-html`）
- **后端 API**：全部依赖已有端点，无需后端变更
- **依赖**：无新增 npm 依赖
