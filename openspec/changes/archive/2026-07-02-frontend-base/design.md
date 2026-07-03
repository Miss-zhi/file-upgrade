## Context

前端升级项目已完成 auth-module（认证模块）前后端，新项目前端已初始化 Vue 3 + Vite + Element Plus + Pinia + TypeScript 骨架，包含登录/注册页面、auth store、API client（含 401 refresh 拦截器）、路由守卫。

旧项目前端（Vue 2 + Element UI + Stylus）有完整的三段式布局（Header 61px + AsideMenu 210px + 内容区 + Footer）、5 个 Vuex store 模块、6 个请求模块、9 条路由。需要将布局/样式/状态管理/API 基础设施 1:1 迁移到 Vue 3 技术栈，不涉及文件操作业务逻辑和预览功能。

## Goals / Non-Goals

**Goals:**

- 构建三段式布局框架（AppLayout / AppHeader / AppAside / AppFooter），尺寸与旧项目精确一致（~28 个新增文件）
- 将 Stylus 样式体系迁移为 SCSS（变量/reset/mixin/响应式/Element Plus 覆盖）
- 创建全部 9 条业务路由和对应页面视图
- 将 4 个 Vuex module 迁移为 Pinia store（fileList / sideMenu / common / uploadFile）
- 补充基础 API 模块（admin / notice / home / file-storage）和公共类型定义
- 清理 Vite 脚手架模板文件

**Non-Goals:**

- 文件列表展示（FileTable / FileGrid / FileTimeLine）
- 文件操作弹窗和右键菜单
- 文件上传组件
- 6 种文件预览（图片/视频/音频/代码/Markdown/OnlyOffice）
- 面包屑导航、搜索功能

## Decisions

### D1: 布局组件放在 layouts/ 目录，不放 components/

**决定**：布局组件（AppLayout / AppHeader / AppAside / AppFooter）放在 `src/layouts/` 目录。

**理由**：AGENTS.md 前端目录规范明确列出 `layouts/` 作为独立目录。布局组件是应用级框架，与业务组件（components/）有本质区别。旧项目将 Header/Footer 放在 components/ 是因为 Vue 2 项目没有 layouts 约定，新项目应遵循 AGENTS.md 规范。

### D2: SCSS 替代 Stylus，Element Plus 通过 CSS 变量覆盖主题

**决定**：使用 SCSS 作为 CSS 预处理器，主题色通过 `--el-color-primary` 等 CSS 变量覆盖，不修改 Element Plus 源码。

**理由**：
- Stylus 社区生态萎缩，Vue 3 生态中 SCSS 是主流选择
- Element Plus 官方推荐通过 CSS 变量覆盖主题，比 SCSS 变量覆盖更灵活
- SCSS 变量用于自定义组件样式，CSS 变量用于 Element Plus 主题覆盖，职责分离

**替代方案**：继续使用 Stylus → 生态风险，且 Vite 对 SCSS 支持更好，放弃。

### D3: 路由 meta 控制 Header/Footer 显隐，不硬编码路由名称

**决定**：通过路由 meta 字段 `hideHeader` / `hideFooter` 控制布局组件显隐，而非在 AppLayout 中检查 `$route.name`。

**理由**：
- 旧项目在 App.vue 中硬编码 `routerNameList`，每次新增路由都需要修改布局组件
- meta 方案解耦了布局逻辑与具体路由，新增页面只需设置 meta 字段
- 符合 Vue Router 的 meta 字段设计意图

### D4: Pinia store 使用 setup 函数风格，localStorage 持久化

**决定**：所有 store 使用 `defineStore` + setup 函数风格（与已有 auth store 一致），持久化状态使用 `localStorage`，key 前缀 `qiwen_`。

**理由**：
- setup 函数风格是 Pinia 推荐方式，比 Options 风格更适合 TypeScript 类型推导
- localStorage 持久化保持与旧项目行为一致（旧项目 fileList module 的 3 个字段都持久化到 localStorage）
- key 前缀 `qiwen_` 与旧项目一致，便于数据迁移和调试

**替代方案**：使用 `pinia-plugin-persistedstate` → 引入额外依赖，且旧项目是手动 localStorage，简单场景不需要插件。

### D5: API 函数解包 RestResult，返回 Promise<T>

**决定**：API 模块函数统一解包 `RestResult<T>` 外层，返回 `Promise<T>`。调用方直接使用业务数据，无需处理 code/message/data 包装。

**理由**：
- 现有 `api/auth.ts` 已采用此模式（`return data.data`），保持一致
- 错误处理由 axios 响应拦截器统一处理（已有 401 refresh 逻辑）
- 减少调用方重复的 `if (res.code === 0)` 判断

### D6: 文件类型常量集中定义在 types/file.ts

**决定**：`FileType` enum、`FileViewMode` enum、`fileImgMap`（50+ 扩展名→图标）、`officeFileType`、`markdownFileType` 等常量集中定义在 `src/types/file.ts`。

**理由**：
- 旧项目这些常量分散在 `libs/map.js`，缺乏类型安全
- 集中定义便于 file-module-frontend 和 frontend-preview 两个后续 change 直接引用
- TypeScript enum 提供编译期类型检查，优于旧项目的字符串数组

### D7: HomeView 子组件拆分策略

**决定**：HomeView 拆分为 3 个子组件（HomeBanner / HomeFeatures / HomeNotice），放在 `components/home/` 目录。

**理由**：
- 旧项目已有此拆分（Banner.vue / Function.vue / Notice.vue），保持一致
- 每个子组件职责单一，便于维护和测试
- Notice 组件需要独立调用 API 获取公告数据

### D8: 路由守卫策略——全局 beforeEach + 组件级 useAuthGuard 并存

**决定**：保留 `router/index.ts` 中的全局 `beforeEach` 守卫（负责首次页面加载时恢复用户认证状态），同时保留 `composables/useAuthGuard.ts` 作为组件内辅助 composable（负责组件级认证检查）。

**理由**：
- 全局 beforeEach 在 SPA 首次加载时执行 `fetchMe()` 恢复登录状态，这是组件级守卫无法替代的
- useAuthGuard 在组件 `setup()` 中使用，适合特定组件内的认证检查（如 AdminView）
- 两者职责不同：全局守卫负责"恢复状态"，组件守卫负责"检查并跳转"

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| SCSS 变量与 Element Plus CSS 变量可能冲突 | SCSS 变量用于自定义组件，Element Plus 使用 `--el-*` CSS 变量，命名空间隔离 |
| 旧项目 50+ 文件图标 SVG 资源需要迁移 | 从旧项目 public 目录提取 SVG 文件，或使用 Element Plus 内置图标 + 自定义 SVG |
| localStorage key 与旧项目冲突（同一浏览器同时访问新旧项目） | 新旧项目运行在不同端口，localStorage 按源隔离，不冲突 |
| 响应式断点迁移可能遗漏某些组件的移动端适配 | responsive.scss 先覆盖全局和布局组件，页面级响应式留待各 change 处理 |
| 文件图标资源体积可能较大（50+ SVG） | 使用 SVG sprite 或按需导入，避免全量打包 |
