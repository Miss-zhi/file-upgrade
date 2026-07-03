# app-pages — 页面视图

## Description

替换 Vite 脚手架页面，创建奇文网盘全部业务页面视图。本 capability 创建页面壳和基本内容，文件管理的详细交互（文件列表、操作弹窗、预览）由后续 change 填充。

## Requirements

### REQ-1: HomeView 首页

首页由 3 个子组件组成，展示产品介绍和功能亮点。

**Scenarios:**

- **HomeBanner**：`el-carousel` 轮播区，高度 360px，渐变蓝背景（`linear-gradient(to right, #409EFF, #79bbff)`），左侧标题"一款功能齐全的文件管理系统" + 3 行描述 + CTA 按钮"开源免费，立即体验"（导航到 `/file`），右侧装饰图片（max-width 443px）。内容区宽度 85%
- **HomeFeatures**：功能介绍区，标题"功能介绍"（28px，居中，60px 上下 padding），9 张功能卡片 3 列网格（32% 宽度，max-width 1200px），每张卡片包含圆形图标容器（100x100，内含 70x70 图标）+ 标题 + 描述。背景 `#ecf5ff`，hover 时渐变蓝背景 + 白色文字。9 项功能：多种存储方式、分片断点续传、在线文档编辑、回收站、文件分类与视图、全局搜索、文件分享、基础操作、在线预览
- **HomeNotice**：公告预览区，调用 `getNoticeList()` API 获取最新 3 条公告。自动轮播（setInterval），手动上下箭头暂停/恢复。点击条目导航到公告详情，"查看全部"导航到公告列表。标题使用 setEllipsis mixin 截断

### REQ-2: FileView 文件管理壳页面

FileView 是文件管理的容器页面，提供侧边栏 + 内容区的布局。

**Scenarios:**

- **布局**：`el-container` > `el-aside`（渲染 AppAside）+ `el-main`（padding `0 16px`）
- **高度**：`calc(100vh - 61px)`（减去 Header 高度）
- **拖拽上传遮罩**：当 `uploadFileStore.showUploadMask` 为 true 时显示全屏遮罩（具体上传组件由后续 change 实现）
- **拖拽事件**：`@dragenter` 时设置 `showUploadMask = true`
- **内容区**：预留 slot/router-view 给后续的 FileList 组件
- **不显示 Footer**：路由 meta.hideFooter = true

### REQ-3: AdminView 管理员页面

管理员后台的页面壳，包含用户管理表格骨架。

**Scenarios:**

- **表格骨架**：`el-table` 展示用户列表列定义（用户名、手机号、注册时间、状态、存储空间、操作）
- **数据加载**：调用 `getUserList()` API
- **操作列**：启用/禁用、修改存储配额、重置密码（按钮骨架，具体逻辑由后续 change 实现）
- **需要登录**：路由 meta.requiresAuth = true

### REQ-4: NoticeListView 公告列表

公告列表页面。

**Scenarios:**

- **列表展示**：调用 `getNoticeList()` API，分页展示公告条目
- **分页**：`el-pagination` 组件
- **点击跳转**：点击条目导航到 `/notice/:noticeId`

### REQ-5: NoticeDetailView 公告详情

公告详情页面。

**Scenarios:**

- **路由参数**：`:noticeId` 从路由获取公告 ID
- **数据加载**：调用 `getNoticeDetail(noticeId)` API
- **内容渲染**：展示公告标题、发布时间、正文内容

### REQ-6: ShareView 分享查看页

公开端点，无需登录即可查看他人分享的文件。

**Scenarios:**

- **路由参数**：`:shareBatchNum` 分享批次号
- **提取码验证**：如需提取码，显示输入框验证
- **文件列表**：验证通过后展示分享的文件列表
- **不显示 Footer**：路由 meta.hideFooter = true
- **无需登录**：路由 meta.requiresAuth 不设置

### REQ-7: ErrorPage 404 页面

404 错误页面。

**Scenarios:**

- **外观**：居中显示 404 提示信息和返回首页链接
- **隐藏 Header 和 Footer**：路由 meta.hideHeader = true，meta.hideFooter = true

### REQ-8: 脚手架文件清理

删除 Vite 脚手架模板文件。

**Scenarios:**

- 删除 `views/AboutView.vue`
- 删除 `components/HelloWorld.vue`
- 删除 `components/TheWelcome.vue`
- 删除 `components/WelcomeItem.vue`
- 删除 `components/icons/` 目录下全部 5 个文件
- 删除 `assets/base.css`、`assets/main.css`、`assets/logo.svg`

## Component Hierarchy

```
HomeView
├── components/home/HomeBanner.vue
├── components/home/HomeFeatures.vue
└── components/home/HomeNotice.vue

FileView
├── layouts/AppAside.vue (from app-shell)
└── (slot for FileList — future change)

AdminView
└── (inline el-table skeleton)

NoticeListView / NoticeDetailView / ShareView / ErrorPage
└── (standalone pages)
```

## Dependencies

- Element Plus: el-carousel, el-table, el-pagination, el-input, el-button
- API: notice.getNoticeList, notice.getNoticeDetail, home.getSystemParams, admin.getUserList
- Pinia stores: uploadFile (for drag upload mask)
