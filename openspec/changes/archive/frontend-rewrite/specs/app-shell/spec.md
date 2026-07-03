## MODIFIED Requirements

### REQ-1: AppLayout 顶层容器

AppLayout 是应用级布局容器，组合 AppHeader + 主内容区 + AppFooter。

**Scenarios:**

- **正常页面**：渲染 AppHeader（顶部 61px）+ el-container（el-aside + el-main）+ AppFooter
- **隐藏 Header 页面**：路由 meta.hideHeader 为 true 时不渲染 AppHeader，主内容区占满顶部
- **隐藏 Footer 页面**：路由 meta.hideFooter 为 true 时不渲染 AppFooter
- **FileView 页面**：el-aside 渲染 AppAside（侧边栏 210px），el-main 渲染子路由，el-main padding `0 16px`
- **非文件页面**：el-aside 不渲染，el-main 占满宽度
- **主内容区宽度**：非FileView页面，主内容区 width 90%，min-height `calc(100vh - 70px)`，margin `0 auto`

### REQ-2: AppHeader 顶部导航栏

AppHeader 是固定顶部导航栏，高度约 61px，全宽，带底部阴影。

**Scenarios:**

- **Logo 展示**：Logo 图片高度 40px，margin `14px 24px`，点击导航到首页
- **水平菜单**：`el-menu` 水平模式，菜单项"网盘"导航到 `/file`，"公告"导航到 `/notice`
- **未登录状态**：右侧显示"登录"和"注册"链接
- **已登录状态**：右侧显示 `el-dropdown`，包含用户名、修改密码对话框、退出登录
- **移动端（≤768px）**：Logo 缩小（高度 24px，margin `12px 8px`），菜单折叠为 hamburger
- **路由激活**：当前路由对应菜单项高亮
- **Header整体尺寸**：padding `0 20px`，box-shadow 与旧项目一致

### REQ-3: AppAside 左侧文件分类菜单

AppAside 是可折叠的侧边栏菜单，展开宽度 210px，高度 `calc(100vh - 61px)`。

**Scenarios:**

- **菜单项列表**：8 个菜单项，每个有图标和文字
  - 全部（fileType=0，icon Menu）
  - 图片（fileType=1，icon Picture）
  - 文档（fileType=2，icon Document）
  - 视频（fileType=3，icon VideoCamera）
  - 音乐（fileType=4，icon Headset）
  - 其他（fileType=5，icon Box）
  - 回收站（fileType=6，icon Delete）
  - 我的分享（fileType=8，icon Share）
- **菜单点击**：点击菜单项更新路由 query（fileType 和 filePath），刷新文件列表
- **管理员入口**：仅当用户角色包含"超级管理员"时显示"管理后台"菜单项
- **折叠/展开**：右侧有 12px 宽 100px 高的切换条，圆角 `0 16px 16px 0`，背景 #DCDFE6，点击切换 el-menu collapse 状态
- **折叠状态持久化**：折叠状态存储到 localStorage key `qiwen_is_collapse`，刷新后恢复
- **存储容量条**：底部 66px 区域，显示"已用 X / 总共 Y"，进度条颜色按使用率变化（≤50% `$success` 绿色、≤80% `$warning` 橙色、>80% `$danger` 红色）
- **移动端（≤768px）**：渲染为 `el-drawer`（size 210px，`direction="ltr"`）
- **路由激活高亮**：当前 fileType 对应的菜单项高亮，选中项背景 #ecf5ff
- **侧栏高度**：MUST 为 `calc(100vh - 61px)`，右侧 padding 11px

### REQ-4: AppFooter 底部版权栏

AppFooter 是页面底部版权信息栏。

**Scenarios:**

- **外观**：背景 `linear-gradient(to right, #409EFF, #66b1ff)`，padding `16px 0 16px 5vw`
- **Logo**：白色 Logo 图片，宽度 240px（≤920px 屏幕宽度 160px）
- **版权信息**：通过 API 获取 copyright 配置，显示在 Logo 下方；API 不可用时显示默认版权文字
- **条件显隐**：File / Share / OnlyOffice / Error 页面不显示（通过路由 meta.hideFooter 控制）
- **移动端**：≤768px 时内容垂直堆叠，居中对齐
