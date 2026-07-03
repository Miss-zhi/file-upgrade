# file-module-frontend — 文件管理前端交互模块提案

## 背景

前端升级第三阶段：基于已完成的 file-module 后端（30 个端点，4 个 Controller）和 frontend-base 骨架（布局/路由/store 壳/API 壳/主题），构建文件管理的完整前端交互。此阶段产出后，用户可以进行文件浏览、上传下载、批量操作、分享、回收站等全部核心操作。文件预览（6 种类型）留待后续 `frontend-preview` change 实现。

### 当前状态

frontend-base 已交付：

| 已有文件 | 状态 |
|---------|------|
| `layouts/AppLayout.vue` | Header 61px + Aside 210px + Footer 三段式布局 |
| `layouts/AppAside.vue` | 8 菜单项 + 存储容量条 + 折叠/展开 + 移动端 drawer |
| `views/FileView.vue` | 壳页面：el-aside + el-main + 拖拽上传遮罩（无文件列表） |
| `views/ShareView.vue` | 壳页面：提取码输入框 + empty placeholder |
| `stores/fileList.ts` | selectedColumnList / fileModel / gridSize / selectedFiles / isBatchOperation（仅偏好持久化） |
| `stores/sideMenu.ts` | storageValue / totalStorageValue / isCollapsed + fetchStorage() |
| `stores/uploadFile.ts` | showUploadMask（仅布尔值切换） |
| `api/file.ts` | 仅 getStorage() |
| `types/file.ts` | FileType enum / FileViewMode enum / fileImgMap / officeFileType / markdownFileType / allColumnList / fileSuffixCodeModeMap |
| `router/index.ts` | 9 条路由全部就绪（file / share / notice / admin / error404 等） |

### 旧项目文件管理参考

旧项目（Vue 2 + Element UI）文件管理核心组件：

- **FileList.vue**：容器组件，v-if 切换 FileTable / FileGrid / FileTimeLine，底部 el-pagination（table 50条/grid 100条），分页 `[10, 50, 100, 200]`
- **OperationMenu.vue**：工具栏，上传+新建按钮组 / 批量操作组 / 搜索框 / 视图切换图标 / 设置弹窗（列显隐 + 图标大小滑块）
- **BreadCrumb.vue**：面包屑导航，可编辑路径输入框，按 fileType 切换文本/路径模式
- **FileTable.vue**：el-table，列定义按 fileType 动态显隐，sort-by 始终 isDir 优先，右键菜单，选中行
- **FileGrid.vue**：flex wrap 网格，项宽 gridSize+40px，悬停背景 `$tabBackColor`，批量选择覆盖层
- **FileTimeLine.vue**：el-timeline 按日期分组，正序/倒序切换，仅图片类可用
- **ContextMenu**：文件右键（查看/删除/复制/移动/重命名/分享/下载/解压/在线编辑/详情）+ 空白右键（刷新/新建/上传）
- **9 个操作弹窗**：addFolder / copyFile / moveFile / renameFile / deleteFile / shareFile / showFileDetail / restoreFile / unzipFile
- **上传面板**：右下角固定 560px 宽，1MB 分片上传 + SparkMD5 秒传 + 断点续传 + 截图粘贴 + 拖拽上传全屏遮罩

## 升级目标

1:1 还原旧项目的文件管理交互和视觉样式，技术栈从 Vue 2 + Element UI + Vuex + Stylus 迁移到 Vue 3 + Element Plus + Pinia + SCSS + TypeScript。本 change 不涉及 6 种文件预览功能。

## Capabilities

### 1. file-api — 文件管理 API 客户端

补全 `api/file.ts`，封装后端 4 个 Controller 的 30 个端点。

**后端 API 清单：**

| Controller | 前缀 | 端点数 | 关键端点 |
|---|---|---|---|
| FileController | `/api/v1/file` | 11 | getfilelist, getfilelist/bycategory, renamefile, movefile, batchmovefile, copyfile, batchcopyfile, createfold, createfile, getfiledetail/{id}, getfiletree |
| FileTransferController | `/api/v1/filetransfer` | 7 | upload, upload/speed, upload/chunk/init, upload/chunk, upload/chunk/merge, download/{id}, batch-download |
| RecoveryFileController | `/api/v1/recycle` | 6 | list, deletefile, batchdeletefile, restorefile, deletepermanent, deleteall |
| FileShareController | `/api/v1/share` | 6 | createshare, info/{code}, verifyshare, download/{code}, myshares, cancelshare/{id} |

**类型定义（types/file.ts 补充）：**
- `FileInfo`：userFileId, fileName, extendName, isDir, filePath, fileSize, uploadTime, deleteTime, shareBatchNum, extractionCode, shareType, endTime 等
- `FileListParams`：fileType, filePath, currentPage, pageCount
- `FileTreeNode`：label, filePath, children
- 各 DTO 请求类型（RenameFileDTO, MoveFileDTO, CopyFileDTO, CreateFoldDTO, ShareCreateDTO 等）

**约束：**
- API 函数统一返回 `Promise<T>`（解包 RestResult），与 frontend-base 约定一致
- 上传相关函数单独分组（upload / speedUpload / chunkInit / chunkUpload / chunkMerge）

### 2. file-store — Pinia 状态管理增强

在 frontend-base 已有的 store 基础上，增加文件数据和操作逻辑。

**fileList store 增强：**
- `fileList: FileInfo[]` — 当前页文件列表
- `total: number` — 文件总数
- `loading: boolean` — 加载状态
- `fetchFileList(params)` — 根据 fileType 调用不同 API（0→getfilelist, 6→recycle/list, 8→share/myshares）
- `setFileList(data)` / `clearSelection()`

**uploadFile store 增强：**
- `uploadQueue: UploadTask[]` — 上传任务队列
- `addTask(file)` / `removeTask(id)` / `updateProgress(id, percent)`
- `UploadTask` 类型：id, fileName, fileSize, progress, status(pending/uploading/success/error), errorMsg

**新增 composables：**
- `useFileOperations()` — 封装文件操作（delete/move/copy/rename/share）的 loading 状态和错误处理
- `useUploadManager()` — 封装分片上传、秒传检测、MD5 计算、并发控制逻辑

### 3. file-list-display — 文件列表三种展示模式

1:1 还原旧项目 FileTable / FileGrid / FileTimeLine 组件。

**FileTable（`components/file/FileTable.vue`）：**
- el-table，`fit` + `highlight-current-row` + `v-loading`
- 列定义按 fileType 动态显隐（与旧项目完全一致）：
  - 选择列（56px，fileType≠8 显示）
  - 图标列（56px，移动端 40px，视频显示 video 标签，其他显示 img）
  - 文件名（flex 自动宽度，sortable `['isDir','fileName']`，show-overflow-tooltip）
  - 路径（fileType 非 0/8/分享 且 screenWidth>768 显示）
  - 类型（80px，按 selectedColumnList 控制）
  - 大小（100px，align right，calculateFileSize 格式化）
  - 修改日期（160px）/ 删除日期（fileType=6）/ 分享类型/时间/过期时间（fileType=8）
  - 更多列（移动端 48px，el-icon-more）
- sort-by 始终 `['isDir', ...]` 确保文件夹优先
- 选中行 → fileList store selectedFiles + isBatchOperation
- 右键行 → 打开 ContextMenu（screenWidth>768）
- 高度：`calc(100vh - 206px)`（file-type-0），`calc(100vh - 211px)`（file-type-6），`calc(100vh - 109px)`（share）
- 表头 padding `4px 0`，行 padding `8px 0`，自定义滚动条 6px

**FileGrid（`components/file/FileGrid.vue`）：**
- flex wrap 布局，ul/li 结构
- 项宽 `gridSize + 40px`，margin `0 16px 16px 0`，padding 8px
- 悬停背景 `$tabBackColor (#F5F7FA)`，文件名加粗
- 文件名 2 行 ellipsis，font-size 12px，height 44px
- 批量操作时显示 checkbox 覆盖层
- 容器高度 `calc(100vh - 206px)`

**FileTimeLine（`components/file/FileTimeLine.vue`）：**
- el-timeline，按 uploadTime 日期分组
- 正序/倒序 el-radio-group 切换
- 每项 gridSize x gridSize 图片 + 2 行文件名
- 容器高度 `calc(100vh - 215px)`
- 仅 fileType=1（图片）时可用

### 4. file-toolbar — 工具栏与导航

1:1 还原 OperationMenu + BreadCrumb + SelectColumn。

**OperationMenu（`components/file/OperationMenu.vue`）：**
- flex 布局，`justify-content: space-between`，padding `16px 0`
- 左侧（fileType=0 且非批量时）：上传按钮组（el-dropdown：上传文件/上传文件夹/拖拽上传）+ 新建按钮组（el-dropdown：新建文件夹/Word/Excel/PPT）
- 中部（批量操作时）：批量删除 / 批量移动 / 批量下载 / 批量分享
- 搜索框（fileType=0）：el-input 250px，prefix el-icon-search，clearable
- 右侧：批量切换图标（grid 模式）/ 刷新图标 / 竖分隔线 / 视图模式图标（列表/网格/时间线，screenWidth>768 显示）/ 竖分隔线 / 设置 el-popover（SelectColumn + 移动端视图切换 + 图标大小滑块）
- fileType=6（回收站）时：`margin: 8px 0; justify-content: flex-end`，隐藏上传/新建
- 所有图标 20px，默认色 `$SecondaryText (#909399)`，激活色 `$Primary (#409EFF)`

**BreadCrumb（`components/file/BreadCrumb.vue`）：**
- flex 布局，height 30px，line-height 30px
- el-breadcrumb separator `el-icon-arrow-right`
- fileType=0 时路径可编辑（点击空白区域切换为 el-input）
- fileType=1~6 时显示静态文本标签
- fileType=8 时显示路径面包屑
- 容器可悬停背景 `$tabBackColor`

**SelectColumn（`components/file/SelectColumn.vue`）：**
- el-dialog title "设置表格列显隐"，width 700px
- el-checkbox-group：类型/大小/修改日期/删除日期
- 确认后写入 fileList store（已持久化到 localStorage）

### 5. file-context-menu — 右键菜单

1:1 还原旧项目的右键菜单组件。

**ContextMenu（`components/file/ContextMenu.vue`）：**
- position fixed，z-index 2，白色背景，border `$BorderLighter`，border-radius 4px，shadow `$tabBoxShadow`
- 项高 36px，padding `0 16px`，font-size 14px
- 悬停：背景 `$PrimaryHover (#ecf5ff)`，文字 `$Primary (#409EFF)`
- 智能定位：下方空间不足时向上展开，右侧不足 138px 时向左展开
- 点击 document.body 关闭

**文件右键菜单项（selectedFile 存在时）：**
- 查看（fileType≠6）/ 删除（fileType≠8，非分享页）/ 还原（fileType=6）/ 复制到 / 移动 / 重命名 / 分享 / 下载 / 解压缩（zip/rar/7z/tar/gz，3 子菜单项）/ 在线编辑（office/markdown/code 类型）/ 复制链接（fileType=8）/ 文件详情（始终显示）

**空白区域右键菜单项（selectedFile 不存在，fileType=0）：**
- 刷新 / 分隔线 / 新建文件夹 / 新建 Word / 新建 Excel / 新建 PPT / 分隔线 / 上传文件 / 上传文件夹 / 拖拽上传

### 6. file-dialogs — 操作弹窗

1:1 还原旧项目的 10 个操作弹窗。Vue 3 使用 composable + el-dialog v-model 替代旧项目的 Vue.extend + Promise 模式。

**统一弹窗模式：**
- 每个弹窗一个 composable（如 `useAddFolderDialog()`），返回 `{ visible, open(params), confirm, cancel, confirmed: Promise<boolean> }`
- el-dialog 渲染在 FileView 组件内（非 document.body 动态挂载）
- 替代旧项目 `$openBox` / `$openDialog` 的 imperative API

**弹窗清单：**

| 弹窗 | 标题 | 宽度 | 核心行为 |
|------|------|------|---------|
| AddFolderDialog | 新建文件夹 | 580px | textarea 输入名称，正则拒绝 `\/:*?"<>\|`，调用 createfold API |
| CopyFileDialog | 选择目标路径 | 默认 | el-tree 目录浏览，节点悬停显示"新建文件夹"，调用 copyfile API |
| MoveFileDialog | 选择目标路径 | 默认 | 同 CopyFileDialog，支持批量，调用 movefile/batchmovefile |
| RenameDialog | 重命名文件 | 550px | textarea 预填旧名，调用 renamefile API |
| DeleteDialog | 删除文件 | 550px | 双模式：mode=1 软删除提示 / mode=2 永久删除警告，支持批量 |
| ShareDialog | 分享文件 | 550px | 两阶段 UI：配置（有效期选择）→ 结果（链接+提取码+复制按钮），提取码由后端自动生成 |
| FileDetailDialog | 文件详情 | 550px | 只读展示：图标+名称+路径+类型+大小+日期，按 fileType 条件显示字段 |
| RestoreDialog | 还原文件 | 550px | 打开即自动执行 restorefile API，loading 文字提示 |
| UnzipDialog | 解压缩文件 | 默认 | 3 模式：0/1 自动执行+spinner，2 显示 tree 选择目标（⚠️ 后端暂无解压端点，UI 预留） |
| SaveShareDialog | 保存文件到网盘 | 默认 | tree 选择目标路径，调用 savesharefile API |

### 7. file-upload — 文件上传

1:1 还原旧项目的上传面板和拖拽上传功能。

**上传面板（`components/file/UploadPanel.vue`）：**
- 固定定位：right 16px，bottom 16px，z-index 20
- 面板宽 560px，标题栏 40px，文件列表 240px 可滚动
- 背景白色，border `#e2e2e2`，border-radius `7px 7px 0 0`，shadow `0 0 10px rgba(0,0,0,0.2)`
- 折叠/展开/关闭按钮

**上传逻辑（composable `useUploadManager()`）：**
- 普通上传（≤10MB）：FormData POST `/filetransfer/upload`
- 秒传：SparkMD5 计算 hash（1MB 分块），POST `/filetransfer/upload/speed` 检测去重
- 分片上传（>10MB）：init → chunk × N → merge，每片 1MB，最多 3 次重试
- MD5 计算进度显示："计算MD5" / "校验MD5 XX%"
- 上传前配额校验：remainderStorageValue vs 文件总大小
- 并发控制 + 失败重试

**拖拽上传遮罩：**
- 全屏 fixed，z-index 19，border 5px dashed `#8091a5`，背景 `#ffffffd9`
- 居中文字 "截图粘贴或将文件拖拽至此区域上传"，font-size 30px
- 右上角：关闭按钮 / 上传按钮 / 删除按钮
- 支持截图粘贴（paste event → clipboard image）

### 8. file-share-view — 分享页面完善

完善 ShareView 的分享文件列表展示和交互。

**ShareView 增强：**
- 提取码验证：POST `/share/verifyshare`，成功后展示文件列表
- 分享文件列表：显示文件名、大小、过期时间
- 下载按钮：GET `/share/download/{shareCode}`
- 保存到我的网盘（登录用户）：SaveShareDialog
- 公开端点，无需登录

## 路由更新

不新增路由（frontend-base 已配置 file / share / admin 等全部路由）。仅完善 FileView 和 ShareView 内部组件渲染。

## 不在范围内

- 6 种文件预览（图片/视频/音频/代码/Markdown/OnlyOffice）— 留给 frontend-preview
- 文件搜索功能（前端搜索 UI）— 留给 search-module-frontend
- 微信登录集成
- 修改密码对话框（属于 auth-module 前端）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~35 个（3 列表组件 + 3 工具栏组件 + 1 右键菜单 + 10 弹窗 + 10 composable + 1 上传面板 + 2 util + 3 types 补充 + 2 API 补充） |
| 修改文件 | 5 个（api/file.ts / stores/fileList.ts / stores/uploadFile.ts / views/FileView.vue / views/ShareView.vue） |
| 删除文件 | 0 个 |
| 新增依赖 | spark-md5（MD5 计算，用于秒传和分片校验） |
