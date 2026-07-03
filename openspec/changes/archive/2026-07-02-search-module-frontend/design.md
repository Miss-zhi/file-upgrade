## Context

当前前端项目（Vue 3 + Element Plus + TypeScript）的文件视图工具栏已有一个搜索输入框（`OperationMenu.vue`），能捕获用户输入并通过 `@search` 事件传递给 `FileView.vue`。但 `FileView.vue` 的 `handleSearch()` 是 stub，直接丢弃关键词并刷新文件列表。

后端搜索模块基于 Elasticsearch，提供 `GET /api/v1/search` 端点，支持关键词搜索、分页、排序、文件名高亮。返回结构为 `RestResult<{ total: number, items: SearchResultVO[] }>`。

现有基础设施：
- `OperationMenu.vue` 已有搜索 UI（`el-input` + Search 图标），仅在「全部文件」视图显示
- `FileView.vue` 已有文件列表渲染逻辑（列表/网格/时间线三种视图）
- `FileTable.vue`、`FileGrid.vue` 等展示组件已有文件名渲染逻辑
- `api/client.ts` 提供 Axios 实例（baseURL: `/api/v1`）
- 项目约定使用 `<script setup lang="ts">`、SCSS、composable 模式

## Goals / Non-Goals

**Goals:**
- 将搜索 stub 替换为真实搜索逻辑，对接后端 `/api/v1/search` 端点
- 搜索结果原地替换文件列表，保持视图模式一致
- 支持高亮文件名渲染
- 支持搜索分页和排序
- 清空搜索恢复普通文件列表

**Non-Goals:**
- 不创建独立的搜索页面/路由（搜索内嵌在文件视图中，与旧项目一致）
- 不实现文件内容搜索（仅搜索文件名和扩展名）
- 不实现搜索建议/自动补全
- 不修改 admin 模块的搜索重建功能（已有权限码 `admin:search-rebuild`，但不在本次范围）
- 不引入新的 npm 依赖

## Decisions

### 1. 搜索模式：内嵌文件视图，非独立页面

**选择**：搜索结果原地替换文件列表，不创建 `/search` 路由。

**理由**：与旧项目行为一致。搜索是文件浏览的辅助操作，用户期望搜索后仍在文件列表中操作（打开文件、预览等）。独立页面会增加导航复杂度。

### 2. 搜索状态管理：composable 而非 store

**选择**：创建 `useSearch.ts` composable，在 `FileView.vue` 中实例化。搜索状态（关键词、结果、分页）作为组件局部状态。

**理由**：搜索状态仅在文件视图内有意义，不需要跨页面共享。composable 模式与项目现有风格一致。

### 3. 高亮渲染：v-html + 后端高亮字段 + 白名单过滤

**选择**：后端返回 `highlightFileName`（含 `<em>` 标签），前端通过 `v-html` 渲染。对高亮 HTML 做白名单过滤（仅允许 `<em>` 标签），防止 XSS。

**理由**：后端已使用 IK 分词器生成高亮片段。前端无需自行实现高亮逻辑。白名单过滤确保安全。

### 4. 搜索结果与文件列表的数据结构对齐

**选择**：搜索结果 `SearchResultVO` 与文件列表 `FileListVO` 字段不完全相同。在 `useSearch` composable 中将搜索结果映射为文件展示组件可消费的格式。

**理由**：避免修改文件展示组件的接口定义，减少改动范围。映射层隔离两个数据源的差异。

## Risks / Trade-offs

- **[搜索结果文件操作受限]** 搜索结果缺少部分文件列表字段（如 `fileId`、`uuid`），可能导致某些操作（分享、下载）无法直接使用。→ 缓解：搜索结果中仅支持预览和「打开所在目录」，禁用不支持的操作按钮。
- **[XSS 风险]** `highlightFileName` 包含 HTML 标签，使用 `v-html` 渲染有 XSS 风险。→ 缓解：前端对高亮 HTML 做白名单过滤，仅保留 `<em>` 标签。
- **[ES 服务依赖]** 搜索依赖 Elasticsearch，ES 宕机时搜索不可用。→ 缓解：后端返回 503 时前端展示友好错误提示，不影响正常文件浏览。
