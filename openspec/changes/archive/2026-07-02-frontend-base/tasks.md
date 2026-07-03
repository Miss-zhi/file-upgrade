## 1. 主题与样式系统（app-theme）

- [x] 1.1 安装 `sass` 依赖：`npm install -D sass`
- [x] 1.2 创建 `src/assets/styles/variables.scss`：从旧项目 `varibles.styl` 迁移全部 SCSS 变量（主色/成功/警告/危险/信息、文字色三档、边框色四档、布局尺寸变量、阴影变量）
- [x] 1.3 创建 `src/assets/styles/reset.scss`：从旧项目 `base.styl` 迁移 CSS reset（margin/padding 归零、box-sizing、字体族）
- [x] 1.4 创建 `src/assets/styles/mixins.scss`：迁移 `setScrollbar` 和 `setEllipsis` mixin
- [x] 1.5 创建 `src/assets/styles/element-override.scss`：Element Plus 样式覆盖（Dialog margin-top、Avatar img width、Textarea font-family）+ CSS 变量主题覆盖（`--el-color-primary` 等）
- [x] 1.6 创建 `src/assets/styles/responsive.scss`：响应式断点 `$breakpoint-xs: 768px` + 移动端布局覆盖规则
- [x] 1.7 更新 `src/main.ts`：移除 `main.css` 导入，按序导入 `reset.scss` → `variables.scss` → `element-override.scss` → `responsive.scss`

## 2. API 模块与类型定义（app-api）

- [x] 2.1 创建 `src/types/api.ts`：提取 `RestResult<T>` 接口（从 `api/client.ts`），更新 `client.ts` 改为从此文件导入
- [x] 2.2 创建 `src/types/file.ts`：`FileType` enum（ALL=0..SHARE=8）、`FileViewMode` enum（LIST=0/GRID=1/TIMELINE=2）、`fileImgMap`（50+ 扩展名→图标路径）、`officeFileType` 数组、`markdownFileType` 数组、`allColumnList` 数组
- [x] 2.3 迁移文件图标 SVG 资源：从旧项目 `src/assets/images/file/` 提取 56 个文件类型图标到 `public/img/file/`，`fileImgMap` 路径与实际文件对应
- [x] 2.4 创建 `src/api/file.ts`：仅 `getStorage()` → GET `/api/v1/filetransfer/getstorage`
- [x] 2.5 创建 `src/api/admin.ts`：`getUserList` / `updateUserAvailable` / `updateUserStorage` / `resetPassword`
- [x] 2.6 创建 `src/api/notice.ts`：`getNoticeList` / `getNoticeDetail`
- [x] 2.7 创建 `src/api/home.ts`：`getSystemParams()` → GET `/api/v1/param/grouplist`

## 3. 状态管理（app-store）

- [x] 3.1 创建 `src/stores/common.ts`：`screenWidth` 状态 + `updateScreenWidth()` action，窗口 resize 事件监听
- [x] 3.2 创建 `src/stores/fileList.ts`：`selectedColumnList` / `fileModel` / `gridSize`（持久化 localStorage）/ `selectedFiles` / `isBatchOperation`
- [x] 3.3 创建 `src/stores/sideMenu.ts`：`storageValue` / `totalStorageValue` / `isCollapsed`（持久化 localStorage `qiwen_is_collapse`）+ `fetchStorage()` action
- [x] 3.4 创建 `src/stores/uploadFile.ts`：`showUploadMask` 状态
- [x] 3.5 删除 `src/stores/counter.ts`（Vite 模板）

## 4. 应用布局框架（app-shell）

- [x] 4.1 创建 `src/layouts/AppHeader.vue`：顶部导航栏（Logo 40px + el-menu 水平模式 + 用户信息区），高度 61px，box-shadow，移动端折叠，条件显隐。含修改密码 el-dialog（调用 `api/auth.ts` 的 `changePassword`）
- [x] 4.2 创建 `src/layouts/AppAside.vue`：左侧文件分类菜单（210px 宽，8 个菜单项，底部存储容量条 66px，折叠/展开切换条，折叠状态持久化，移动端 el-drawer）
- [x] 4.3 创建 `src/layouts/AppFooter.vue`：底部版权栏（渐变蓝背景，Logo 240px + 版权信息，条件显隐）
- [x] 4.4 创建 `src/layouts/AppLayout.vue`：顶层布局容器，组合 AppHeader + el-container（el-aside + el-main）+ AppFooter，根据路由 meta.hideHeader / meta.hideFooter 控制显隐
- [x] 4.5 改造 `src/App.vue`：引入 AppLayout 替换当前 `<RouterView />`

## 5. 页面视图（app-pages）

- [x] 5.1 创建 `src/components/home/HomeBanner.vue`：轮播区（el-carousel 360px，渐变蓝背景，标题 + 描述 + CTA 按钮导航到 /file）
- [x] 5.2 创建 `src/components/home/HomeFeatures.vue`：功能介绍区（9 卡片 3 列网格，hover 渐变效果）
- [x] 5.3 创建 `src/components/home/HomeNotice.vue`：公告预览区（调用 getNoticeList API 获取最新 3 条，自动轮播 + 手动上下箭头）
- [x] 5.4 重写 `src/views/HomeView.vue`：组合 HomeBanner + HomeFeatures + HomeNotice
- [x] 5.5 创建 `src/views/FileView.vue`：文件管理壳页面（el-container > el-aside(AppAside) + el-main 预留 slot），支持拖拽上传遮罩
- [x] 5.6 创建 `src/views/AdminView.vue`：管理员页面壳（表格骨架）
- [x] 5.7 创建 `src/views/notice/NoticeListView.vue`：公告列表页
- [x] 5.8 创建 `src/views/notice/NoticeDetailView.vue`：公告详情页（路由参数 :noticeId）
- [x] 5.9 创建 `src/views/ShareView.vue`：分享查看页（路由参数 :shareBatchNum，公开端点）
- [x] 5.10 创建 `src/views/ErrorPage.vue`：404 页面

## 6. 路由更新与清理

- [x] 6.1 重写 `src/router/index.ts`：替换为 9 条业务路由（home/login/register/file/share/notice/noticeDetail/admin/error404），删除 /about 路由，meta 配置 hideHeader/hideFooter/requiresAuth
- [x] 6.2 更新 `vite.config.ts`：添加 SCSS 预处理器配置（`css.preprocessorOptions.scss.additionalData` 注入 `@use "@/assets/styles/variables" as *;`）。注意：`variables.scss` 必须只包含 SCSS 变量定义（`$primary: ...`），不包含任何 CSS 输出（`:root {}`），否则 additionalData 会导致每个文件重复输出 CSS
- [x] 6.3 删除 Vite 脚手架文件：`AboutView.vue`、`HelloWorld.vue`、`TheWelcome.vue`、`WelcomeItem.vue`、`icons/` 目录下 5 个图标组件、`assets/base.css`、`assets/main.css`、`assets/logo.svg`

## 7. 验证

- [x] 7.1 运行 `npm run type-check`：确保 TypeScript 类型检查通过
- [x] 7.2 运行 `npm run build`：确保生产构建无错误
- [ ] 7.3 启动 `npm run dev`：验证全部 9 条路由可达，页面正常渲染
- [ ] 7.4 验证布局尺寸：Header 61px / AsideMenu 210px / StorageBar 66px
- [ ] 7.5 验证响应式：窗口缩至 ≤768px 时 AsideMenu 切换为 drawer、Header 菜单折叠
- [ ] 7.6 验证 localStorage 持久化：折叠状态、文件视图模式、网格大小在刷新后恢复
## 1. 主题与样式系统（app-theme）

- [ ] 1.1 安装 `sass` 依赖：`npm install -D sass`
- [ ] 1.2 创建 `src/assets/styles/variables.scss`：从旧项目 `varibles.styl` 迁移全部 SCSS 变量（主色/成功/警告/危险/信息、文字色三档、边框色四档、布局尺寸变量、阴影变量）
- [ ] 1.3 创建 `src/assets/styles/reset.scss`：从旧项目 `base.styl` 迁移 CSS reset（margin/padding 归零、box-sizing、字体族）
- [ ] 1.4 创建 `src/assets/styles/mixins.scss`：迁移 `setScrollbar` 和 `setEllipsis` mixin
- [ ] 1.5 创建 `src/assets/styles/element-override.scss`：Element Plus 样式覆盖（Dialog margin-top、Avatar img width、Textarea font-family）+ CSS 变量主题覆盖（`--el-color-primary` 等）
- [ ] 1.6 创建 `src/assets/styles/responsive.scss`：响应式断点 `$breakpoint-xs: 768px` + 移动端布局覆盖规则
- [ ] 1.7 更新 `src/main.ts`：移除 `main.css` 导入，按序导入 `reset.scss` → `variables.scss` → `element-override.scss` → `responsive.scss`

## 2. API 模块与类型定义（app-api）

- [ ] 2.1 创建 `src/types/api.ts`：提取 `RestResult<T>` 接口（从 `api/client.ts`），更新 `client.ts` 改为从此文件导入
- [ ] 2.2 创建 `src/types/file.ts`：`FileType` enum（ALL=0..SHARE=8）、`FileViewMode` enum（LIST=0/GRID=1/TIMELINE=2）、`fileImgMap`（50+ 扩展名→图标路径）、`officeFileType` 数组、`markdownFileType` 数组、`allColumnList` 数组
- [ ] 2.3 迁移文件图标 SVG 资源：从旧项目 `public/img/file/` 提取 50+ 文件类型图标 SVG 到 `src/assets/icons/file/`，确保 `fileImgMap` 中的路径与实际文件对应
- [ ] 2.4 创建 `src/api/file.ts`：仅 `getStorage()` → GET `/api/v1/filetransfer/getstorage`
- [ ] 2.5 创建 `src/api/admin.ts`：`getUserList` / `updateUserAvailable` / `updateUserStorage` / `resetPassword`
- [ ] 2.6 创建 `src/api/notice.ts`：`getNoticeList` / `getNoticeDetail`
- [ ] 2.7 创建 `src/api/home.ts`：`getSystemParams()` → GET `/api/v1/param/grouplist`

## 3. 状态管理（app-store）

- [ ] 3.1 创建 `src/stores/common.ts`：`screenWidth` 状态 + `updateScreenWidth()` action，窗口 resize 事件监听
- [ ] 3.2 创建 `src/stores/fileList.ts`：`selectedColumnList` / `fileModel` / `gridSize`（持久化 localStorage）/ `selectedFiles` / `isBatchOperation`
- [ ] 3.3 创建 `src/stores/sideMenu.ts`：`storageValue` / `totalStorageValue` / `isCollapsed`（持久化 localStorage `qiwen_is_collapse`）+ `fetchStorage()` action
- [ ] 3.4 创建 `src/stores/uploadFile.ts`：`showUploadMask` 状态
- [ ] 3.5 删除 `src/stores/counter.ts`（Vite 模板）

## 4. 应用布局框架（app-shell）

- [ ] 4.1 创建 `src/layouts/AppHeader.vue`：顶部导航栏（Logo 40px + el-menu 水平模式 + 用户信息区），高度 61px，box-shadow，移动端折叠，条件显隐。含修改密码 el-dialog（调用 `api/auth.ts` 的 `changePassword`）
- [ ] 4.2 创建 `src/layouts/AppAside.vue`：左侧文件分类菜单（210px 宽，8 个菜单项，底部存储容量条 66px，折叠/展开切换条，折叠状态持久化，移动端 el-drawer）
- [ ] 4.3 创建 `src/layouts/AppFooter.vue`：底部版权栏（渐变蓝背景，Logo 240px + 版权信息，条件显隐）
- [ ] 4.4 创建 `src/layouts/AppLayout.vue`：顶层布局容器，组合 AppHeader + el-container（el-aside + el-main）+ AppFooter，根据路由 meta.hideHeader / meta.hideFooter 控制显隐
- [ ] 4.5 改造 `src/App.vue`：引入 AppLayout 替换当前 `<RouterView />`

## 5. 页面视图（app-pages）

- [ ] 5.1 创建 `src/components/home/HomeBanner.vue`：轮播区（el-carousel 360px，渐变蓝背景，标题 + 描述 + CTA 按钮导航到 /file）
- [ ] 5.2 创建 `src/components/home/HomeFeatures.vue`：功能介绍区（9 卡片 3 列网格，hover 渐变效果）
- [ ] 5.3 创建 `src/components/home/HomeNotice.vue`：公告预览区（调用 getNoticeList API 获取最新 3 条，自动轮播 + 手动上下箭头）
- [ ] 5.4 重写 `src/views/HomeView.vue`：组合 HomeBanner + HomeFeatures + HomeNotice
- [ ] 5.5 创建 `src/views/FileView.vue`：文件管理壳页面（el-container > el-aside(AppAside) + el-main 预留 slot），支持拖拽上传遮罩
- [ ] 5.6 创建 `src/views/AdminView.vue`：管理员页面壳（表格骨架）
- [ ] 5.7 创建 `src/views/notice/NoticeListView.vue`：公告列表页
- [ ] 5.8 创建 `src/views/notice/NoticeDetailView.vue`：公告详情页（路由参数 :noticeId）
- [ ] 5.9 创建 `src/views/ShareView.vue`：分享查看页（路由参数 :shareBatchNum，公开端点）
- [ ] 5.10 创建 `src/views/ErrorPage.vue`：404 页面

## 6. 路由更新与清理

- [ ] 6.1 重写 `src/router/index.ts`：替换为 9 条业务路由（home/login/register/file/share/notice/noticeDetail/admin/error404），删除 /about 路由，meta 配置 hideHeader/hideFooter/requiresAuth
- [ ] 6.2 更新 `vite.config.ts`：添加 SCSS 预处理器配置（`css.preprocessorOptions.scss.additionalData` 注入 `@use "@/assets/styles/variables" as *;`）。注意：`variables.scss` 必须只包含 SCSS 变量定义（`$primary: ...`），不包含任何 CSS 输出（`:root {}`），否则 additionalData 会导致每个文件重复输出 CSS
- [ ] 6.3 删除 Vite 脚手架文件：`AboutView.vue`、`HelloWorld.vue`、`TheWelcome.vue`、`WelcomeItem.vue`、`icons/` 目录下 5 个图标组件、`assets/base.css`、`assets/main.css`、`assets/logo.svg`

## 7. 验证

- [ ] 7.1 运行 `npm run type-check`：确保 TypeScript 类型检查通过
- [ ] 7.2 运行 `npm run build`：确保生产构建无错误
- [ ] 7.3 启动 `npm run dev`：验证全部 9 条路由可达，页面正常渲染
- [ ] 7.4 验证布局尺寸：Header 61px / AsideMenu 210px / StorageBar 66px
- [ ] 7.5 验证响应式：窗口缩至 ≤768px 时 AsideMenu 切换为 drawer、Header 菜单折叠
- [ ] 7.6 验证 localStorage 持久化：折叠状态、文件视图模式、网格大小在刷新后恢复
