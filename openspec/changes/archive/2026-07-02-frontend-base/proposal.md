# frontend-base — 前端基础骨架模块提案

## 背景

前端升级第二阶段：基于已完成的 auth-module 前端（LoginView / RegisterView / auth store / api client / router guard），构建奇文网盘 Vue 3 前端的完整骨架。此阶段产出后，应用的三段式布局（顶部导航 + 左侧边栏 + 右侧内容区）将完整呈现，所有页面路由可达，但文件管理的核心交互（文件列表操作、弹窗、预览）留待后续 `file-module-frontend` 和 `frontend-preview` 两个 change 实现。

### 当前状态

新项目前端（`qiwenshare-frontend/`）已有以下基础：

| 已有文件 | 状态 |
|---------|------|
| `api/client.ts` | axios 实例 + 401 refresh 拦截器 |
| `api/auth.ts` | login / register / logout / fetchMe / changePassword |
| `stores/auth.ts` | Pinia auth store（user / isLoggedIn / roles / permissions） |
| `router/index.ts` | 4 条路由（home / about / login / register）+ beforeEach guard |
| `composables/useAuthGuard.ts` | 认证守卫 composable |
| `views/LoginView.vue` | 登录页 |
| `views/RegisterView.vue` | 注册页 |
| `views/HomeView.vue` | 默认 Vite 模板页（需替换） |
| `views/AboutView.vue` | 默认模板页（需删除） |
| `components/HelloWorld.vue` 等 | Vite 脚手架组件（需删除） |
| `stores/counter.ts` | Vite 模板 store（需删除） |

### 旧项目布局参考

旧项目（Vue 2 + Element UI + Stylus）核心布局为：

- **Header**（61px 高，全宽，box-shadow `0 2px 12px 0 rgba(0,0,0,0.1)`）：Logo + 水平菜单 + 用户信息下拉
- **AsideMenu**（210px 宽，可折叠，`calc(100vh - 61px)` 高）：文件分类菜单 + 存储容量条
- **File.vue 容器**：`el-container` > `el-aside` + `el-main`（padding `0 16px`）
- **Footer**：渐变蓝背景 `linear-gradient(to right, #409EFF, #66b1ff)`
- **App.vue**：Header + RouterView + Footer，根据路由名称条件显隐

## 升级目标

1:1 还原旧项目的页面布局和视觉样式，技术栈从 Vue 2 + Element UI + Stylus 迁移到 Vue 3 + Element Plus + SCSS + TypeScript。本 change 不涉及文件操作业务逻辑和文件预览功能。

## Capabilities

### 1. app-shell — 应用布局框架

构建三段式布局容器，与旧项目 Header/AsideMenu/Footer 布局完全一致。

**范围：**
- `layouts/AppLayout.vue`：顶层布局容器，`el-container` 嵌套结构
- `layouts/AppHeader.vue`：顶部导航栏
  - Logo（40px 高，margin `14px 24px`）
  - `el-menu` 水平模式：网盘 / 公告 菜单项
  - 右侧：未登录显示"登录/注册"链接；已登录显示用户名下拉 + 修改密码 + 退出
  - 高度约 61px，`box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1)`
  - 移动端（≤768px）Logo 缩小（24px，margin `12px 8px`），菜单折叠
  - 条件显隐：通过路由 meta.hideHeader 控制
- `layouts/AppAside.vue`：左侧文件分类菜单
  - 展开宽度 210px，折叠由 el-menu collapse 处理
  - 菜单项：全部(0) / 图片(1) / 文档(2) / 视频(3) / 音乐(4) / 其他(5) / 回收站(6) / 我的分享(8)
  - 底部存储容量条（66px 高），颜色随使用量变化：≤50% 绿色、≤80% 橙色、>80% 红色
  - 折叠/展开切换条（12px 宽，100px 高，居中右侧）
  - 折叠状态持久化到 localStorage key `qiwen_is_collapse`
  - 移动端（≤768px）渲染为 `el-drawer`（size 210px，direction ltr）
- `layouts/AppFooter.vue`：底部版权栏
  - 渐变蓝背景 `linear-gradient(to right, $Primary, #66b1ff)`
  - Logo（240px，移动端 160px）+ 版权信息（API 获取）
  - 条件显隐：通过路由 meta.hideFooter 控制
- `App.vue` 改造：引入 `AppLayout`，替换当前简单 `<RouterView />`

**约束：**
- 布局尺寸必须与旧项目精确一致（61px / 210px / 66px）
- Header/Footer 的显隐逻辑通过路由 meta 控制，不在布局组件内硬编码路由名称
- 所有组件使用 `<script setup lang="ts">`，禁止 Options API

### 2. app-theme — 主题与样式系统

将旧项目 Stylus 样式体系迁移为 SCSS，建立全局主题变量。

**范围：**
- `assets/styles/variables.scss`：SCSS 变量文件（从旧项目 `varibles.styl` 迁移）
  - 主色 `#409EFF` / 成功 `#67C23A` / 警告 `#E6A23C` / 危险 `#F56C6C` / 信息 `#909399`
  - 文字色三档：`#303133` / `#606266` / `#909399`
  - 边框色四档：`#DCDFE6` / `#E4E7ED` / `#EBEEF5` / `#F2F6FC`
  - 布局尺寸变量：`$header-height: 61px`、`$sidebar-width: 210px`、`$sidebar-storage-bar: 66px`
  - 阴影：`$tab-box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1)`
- `assets/styles/reset.scss`：CSS reset（从旧项目 `base.styl` 迁移）
- `assets/styles/element-override.scss`：Element Plus 样式覆盖
- `assets/styles/mixins.scss`：通用 mixin（setScrollbar / setEllipsis）
- `assets/styles/responsive.scss`：响应式断点和移动端样式覆盖
- `main.ts` 更新：导入新的全局样式文件，移除旧的 `main.css`

**约束：**
- 所有颜色值必须通过 SCSS 变量引用，禁止在组件中硬编码色值
- Element Plus 主题通过 CSS 变量覆盖，不修改 Element Plus 源码

### 3. app-pages — 页面视图

替换 Vite 脚手架页面，创建全部业务页面视图。

**范围：**
- `views/HomeView.vue`（替换）：Banner(360px carousel) + 功能介绍(9卡片3列) + 公告预览(3条)
- `views/FileView.vue`（新增）：文件管理壳页面，el-aside + el-main，支持拖拽上传遮罩
- `views/AdminView.vue`（新增）：管理员用户列表页面壳
- `views/notice/NoticeListView.vue`（新增）：公告列表页
- `views/notice/NoticeDetailView.vue`（新增）：公告详情页
- `views/ShareView.vue`（新增）：分享查看页（公开端点）
- `views/ErrorPage.vue`（新增）：404 页面
- 删除 Vite 脚手架文件：AboutView.vue、HelloWorld.vue、TheWelcome.vue、WelcomeItem.vue、icons/*.vue、stores/counter.ts

**约束：**
- HomeView 的 Banner / Function / Notice 拆分为独立子组件（`components/home/`）
- 所有页面使用 `<script setup lang="ts">`

### 4. app-store — 状态管理

将旧项目 5 个 Vuex module 迁移为 Pinia store（auth store 已完成，新增 4 个）。

**范围：**
- `stores/fileList.ts`：selectedColumnList / fileModel(0|1|2) / gridSize(80) / selectedFiles / isBatchOperation
- `stores/sideMenu.ts`：storageValue / totalStorageValue / isCollapsed + fetchStorage() action
- `stores/common.ts`：screenWidth + updateScreenWidth()
- `stores/uploadFile.ts`：showUploadMask
- 删除 `stores/counter.ts`

**约束：**
- 所有 store 使用 `defineStore` + setup 函数风格
- 持久化状态使用 `localStorage`，key 前缀 `qiwen_`
- 禁止 `any` 类型

### 5. app-api — API 模块与类型定义

补充基础骨架所需的 API 模块和公共类型/常量定义。

**范围：**
- `api/admin.ts`：getUserList / updateUserAvailable / updateUserStorage / resetPassword
- `api/notice.ts`：getNoticeList / getNoticeDetail
- `api/file.ts`（部分）：仅 getStorage()
- `api/home.ts`：getSystemParams()
- `types/file.ts`：FileType enum / FileViewMode enum / fileImgMap / officeFileType / markdownFileType / allColumnList
- `types/api.ts`：RestResult<T> 接口（从 client.ts 提取）

**约束：**
- API 函数统一返回 `Promise<T>`（解包 RestResult）
- 类型定义中禁止 `any`

## 路由更新

| 路径 | 名称 | 视图 | requiresAuth | hideHeader | hideFooter |
|------|------|------|---|---|---|
| `/` | home | HomeView | false | false | false |
| `/login` | login | LoginView | false | false | false |
| `/register` | register | RegisterView | false | false | false |
| `/file` | file | FileView | true | false | true |
| `/share/:shareBatchNum` | share | ShareView | false | false | true |
| `/notice` | notice | NoticeListView | false | false | false |
| `/notice/:noticeId` | noticeDetail | NoticeDetailView | false | false | false |
| `/admin` | admin | AdminView | true | false | false |
| `/:pathMatch(.*)*` | error404 | ErrorPage | false | true | true |

删除 `/about` 路由。

## 不在范围内

- 文件列表展示（FileTable / FileGrid / FileTimeLine）
- 文件操作弹窗（新建/复制/移动/删除/重命名/分享/详情/解压）
- 右键菜单、面包屑导航、搜索功能
- 文件上传组件
- 全部 6 种文件预览
- 微信登录集成
- LoginView 的 DragVerify 滑块验证组件迁移（属于 auth-module 前端优化）
- 修改密码对话框组件（属于 auth-module 前端，Header 中仅放置入口按钮）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~28 个（4 布局 + 7 页面视图 + 3 首页子组件 + 4 store + 4 API 模块 + 2 类型文件 + 5 SCSS 样式文件） |
| 修改文件 | 5 个（App.vue / main.ts / router/index.ts / vite.config.ts / package.json） |
| 删除文件 | ~7 个 Vite 脚手架文件 |
| 新增依赖 | sass（SCSS 编译器） |
