# 全文搜索：ES 索引 + 关键词高亮

## Why
用户需要快速搜索文件。利用已有的 Elasticsearch 8.x 基础设施，实现文件索引自动管理和关键词搜索。

## What Changes

### 后端
1. **FileSearchService**：createIndex/deleteIndex/search（关键词+高亮）
2. **SearchController**：POST /search
3. **FileService 修改**：upload 后调用 createIndex，delete 后调用 deleteIndex
4. **ES 配置**：测试环境禁用健康检查

### 前端
1. **SearchBar.vue**：搜索输入组件（全局搜索入口）
2. **SearchResult.vue**：搜索结果页面（关键词高亮）
3. **AppHeader.vue**：集成搜索栏
4. **api/search.js**：搜索 API
5. **router**：添加 /search 路由
