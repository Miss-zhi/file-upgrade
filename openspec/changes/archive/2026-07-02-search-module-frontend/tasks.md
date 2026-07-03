## 1. 基础设施：类型定义与 API 层

- [x] 1.1 创建 `types/search.ts`，定义 `SearchResultVO`、`SearchRequestDTO`、`SearchResponse`、`SearchHealthVO` 接口类型
- [x] 1.2 创建 `api/search.ts`，封装 `searchFiles()` 和 `searchHealth()` 两个 API 函数，对接 `GET /api/v1/search` 和 `GET /api/v1/search/health`

## 2. 搜索 Composable

- [x] 2.1 创建 `composables/useSearch.ts`，封装搜索状态（keyword、results、total、page、size、sortBy、sortOrder、loading）和操作方法（search、clear、searchPageChange、searchSortChange）
- [x] 2.2 在 `useSearch.ts` 中实现搜索结果到文件展示格式的映射函数 `toFileInfo()`，将 `SearchResultVO` 转换为 `FileInfo`
- [x] 2.3 在 `useSearch.ts` 中实现高亮 HTML 白名单过滤函数 `sanitizeHighlight()`（仅允许 `<em>` 标签，与 design.md 一致）

## 3. FileView.vue 接线

- [x] 3.1 修改 `FileView.vue`：`handleSearch()` 调用 `search.search(keyword)`，空关键词自动触发 `clear()` 恢复文件列表；路由切换时调用 `search.clear()`
- [x] 3.2 修改 `FileView.vue`：搜索模式下通过 `fileListStore` 共享搜索结果，`highlightMap` 传递给 FileTable/FileGrid
- [x] 3.3 修改 `FileView.vue`：`handlePageChange` / `handleSortChange` 中根据 `search.isSearch` 分发到搜索或文件列表

## 4. 文件展示组件高亮支持

- [x] 4.1 修改 `FileTable.vue`：新增 `highlightMap` prop，文件名 `<template>` 中通过 `v-html` 渲染高亮内容
- [x] 4.2 修改 `FileGrid.vue`：新增 `highlightMap` prop，文件名通过 `v-html` 渲染高亮内容（同上）

## 5. Bug 修复

- [x] 5.1 **【严重】** `useSearch()` 返回值缺少 `highlightMap`——已在 return 中添加 `highlightMap`
- [x] 5.2 **【中等】** 搜索模式下 loading 状态未同步——所有搜索操作（search/searchPageChange/searchSortChange/clear）现在设置 `fileListStore.loading`
- [x] 5.3 **【中等】** `searchSortChange` 排序方向值映射——新增 `SORT_ORDER_MAP`，将 Element Plus 的 `ascending`/`descending` 映射为后端 `asc`/`desc`
- [x] 5.4 **【低】** `searchPageChange` / `searchSortChange` 未同步更新 `fileListStore.total`——现在均设置 `fileListStore.total = res.total`
- [x] 5.5 **【低】** 搜索错误提示区分——`search()` 中针对 HTTP 503 返回"搜索服务暂不可用"；`sanitizeHighlight` 白名单收紧为仅 `<em>`

## 6. 验证

- [x] 6.1 运行 `vue-tsc --noEmit` 确保 TypeScript 零错误 ✅ 通过
- [x] 6.2 运行 `vite build` 确保生产构建通过 ✅ 通过（22.59s，2118 模块，零错误）
