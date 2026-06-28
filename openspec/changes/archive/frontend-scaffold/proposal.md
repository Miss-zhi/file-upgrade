# 前端骨架搭建：Vue 3.4 + Vite 5 项目结构与基础设施

## Why

奇文网盘前端从 Vue 2 / Element UI / Vue CLI 升级到 Vue 3.4 / Element Plus / Vite 5，旧源码已清除。需要从零搭建完整的现代前端项目骨架，覆盖构建工具链、状态管理、路由、HTTP 封装、组件注册方式，为后续页面迁移提供坚实基础。

## What Changes

### 新增

1. **Vite 项目骨架**：`package.json`（含所有依赖）、`vite.config.ts`（路径别名 + 代理配置）、`tsconfig.json`、`index.html`
2. **入口 + 根组件**：`src/main.ts`（createApp + 插件注册）、`src/App.vue`
3. **路径别名**：`@/` → `src/`、`_c/` → `components/`、`_v/` → `views/`、`_a/` → `assets/`、`_api/` → `api/`（vite.config.ts + tsconfig.json）
4. **ESLint + TypeScript**：`eslint.config.js`、`tsconfig.json`、`vue-tsc --noEmit` 类型检查
5. **Axios 封装**（`src/api/http.js`）：baseURL、超时、请求拦截器（自动附加 JWT Token）、响应拦截器（统一错误处理）
6. **Vue Router 4**（`src/router/index.js`）：基础路由表 + 全局守卫（`src/router/guards.js`）
7. **Pinia 2 Setup Store**：创建 5 个 Store 骨架（user / fileList / sideMenu / common / uploadFile）
8. **Element Plus 按需引入**：`unplugin-vue-components` + `unplugin-auto-import` 插件配置在 vite.config.ts
9. **命令式弹窗服务**（`src/plugins/fileOperationPlugins.js`）：`createApp` 替代旧版 `Vue.extend`
10. **目录骨架**：`src/composables/`、`src/libs/`、`src/components/`（含子目录）、`src/views/`、`src/assets/styles/`
11. **全局配置**：`src/config/index.js`
12. **CI 验证**：`npm run dev` 启动 + `npx vite build` 生产构建通过

### 不涉及

- 不实现具体页面组件（留待后续 change）
- 不实现具体的 API 调用函数（留待后续 change）
- 不修改 CI 流水线（`.github/workflows/ci.yml` 已就绪）

## Capabilities

### New Capabilities
- `frontend-build`：Vite 开发服务器启动 + 生产构建通过
- `frontend-router`：Vue Router 4 路由 + 全局守卫就绪
- `frontend-store`：Pinia 2 Setup Store 架构就绪
- `frontend-http`：Axios 封装 + JWT 拦截器就绪
- `frontend-ui`：Element Plus 按需引入 + 命令式弹窗服务就绪

### Modified Capabilities
- 无（新项目，无旧代码）

## Impact

- **新增文件**：`qiwen-file-web/package.json`、`vite.config.ts`、`tsconfig.json`、`eslint.config.js`、`index.html`、`src/` 下全部骨架文件
- **依赖**：package.json 中导入所有必需依赖，无外部新增依赖
- **兼容性**：CI 流水线 `frontend-lint` + `frontend-build` job 可直接通过
