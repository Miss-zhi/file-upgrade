## 1. 类型定义与 API 客户端

- [x] 1.1 在 `types/file.ts` 中补充 FileInfo、PageResult、FileDetail、TreeNode、UploadResult、ShareInfo、BatchOperationResult 等接口类型
- [x] 1.2 在 `types/file.ts` 中补充各 DTO 请求类型（RenameFileDTO、MoveFileDTO、CopyFileDTO、CreateFoldDTO、CreateFileDTO、DeleteFileDTO、BatchDeleteFileDTO、RestoreFileDTO、ShareCreateDTO、ShareVerifyDTO、SpeedUploadDTO、ChunkUploadInitDTO、BatchMoveFileDTO、BatchCopyFileDTO、FileListParams）
- [x] 1.3 在 `types/file.ts` 中补充 UploadTask 类型（id、fileName、fileSize、progress、status、errorMsg）
- [x] 1.4 在 `api/file.ts` 中封装 FileController 的 11 个端点（getFileList、getFileListByCategory、renameFile、moveFile、batchMoveFile、copyFile、batchCopyFile、createFolder、createFile、getFileDetail、getFileTree）
- [x] 1.5 在 `api/file.ts` 中封装 FileTransferController 的 7 个端点（uploadFile、speedUpload、initChunkUpload、uploadChunk、mergeChunks、downloadFile、batchDownload）
- [x] 1.6 在 `api/file.ts` 中封装 RecoveryFileController 的 6 个端点（getRecycleList、deleteFile、batchDeleteFile、restoreFile、deletePermanent、deleteAllRecycle）
- [x] 1.7 在 `api/file.ts` 中封装 FileShareController 的 6 个端点（createShare、getShareInfo、verifyShare、downloadShareFile、getMyShares、cancelShare）

## 2. Pinia Store 增强

- [x] 2.1 增强 `stores/fileList.ts`：添加 fileList、total、loading、currentPage 状态，实现 fetchFileList（根据 fileType 路由到不同 API）、clearFileList、setFileList、clearSelection 方法
- [x] 2.2 增强 `stores/uploadFile.ts`：添加 uploadQueue 状态，实现 addTask、removeTask、updateProgress、updateStatus 方法
- [x] 2.3 创建 `composables/useFileOperations.ts`：封装文件操作（delete/move/copy/rename/share）的 loading 状态和错误处理
- [x] 2.4 创建 `composables/useUploadManager.ts`：封装 MD5 计算（SparkMD5）、秒传检测、分片上传、并发控制逻辑

## 3. 文件列表展示组件

- [x] 3.1 创建 `components/file/FileTable.vue`：el-table 表格视图，支持动态列显隐、排序、行选择、右键菜单、v-loading、高度自适应
- [x] 3.2 创建 `components/file/FileGrid.vue`：flex wrap 网格视图，支持 gridSize 动态调整、悬停效果、批量选择覆盖层
- [x] 3.3 创建 `components/file/FileTimeLine.vue`：el-timeline 时间线视图，按日期分组、正序/倒序切换
- [x] 3.4 创建 `components/file/Pagination.vue`：分页组件，支持页码切换和 [10, 50, 100, 200] 大小选项

## 4. 工具栏与导航组件

- [x] 4.1 创建 `components/file/OperationMenu.vue`：工具栏，包含上传按钮组、新建按钮组、批量操作按钮、搜索框、视图切换图标、设置 el-popover
- [x] 4.2 创建 `components/file/BreadCrumb.vue`：面包屑导航，支持路径点击导航、fileType=0 时可编辑路径
- [x] 4.3 创建 `components/file/SelectColumn.vue`：列显隐设置弹窗，el-checkbox-group 控制类型/大小/修改日期/删除日期列

## 5. 右键菜单组件

- [x] 5.1 创建 `components/file/ContextMenu.vue`：右键菜单，支持文件右键（查看/删除/复制/移动/重命名/分享/下载/详情等）和空白区域右键（刷新/新建/上传），智能定位

## 6. 操作弹窗组件

- [x] 6.1 创建 `components/file/dialogs/AddFolderDialog.vue` 及 `composables/useAddFolderDialog.ts`：新建文件夹弹窗
- [x] 6.2 创建 `components/file/dialogs/RenameDialog.vue` 及 `composables/useRenameDialog.ts`：重命名弹窗
- [x] 6.3 创建 `components/file/dialogs/DeleteDialog.vue` 及 `composables/useDeleteDialog.ts`：删除弹窗（软删除/永久删除双模式）
- [x] 6.4 创建 `components/file/dialogs/CopyFileDialog.vue` 及 `composables/useCopyFileDialog.ts`：复制文件弹窗（el-tree 选择目标）
- [x] 6.5 创建 `components/file/dialogs/MoveFileDialog.vue` 及 `composables/useMoveFileDialog.ts`：移动文件弹窗（el-tree 选择目标，支持批量）
- [x] 6.6 创建 `components/file/dialogs/ShareDialog.vue` 及 `composables/useShareDialog.ts`：分享弹窗（两阶段：配置→结果）
- [x] 6.7 创建 `components/file/dialogs/FileDetailDialog.vue` 及 `composables/useFileDetailDialog.ts`：文件详情弹窗
- [x] 6.8 创建 `components/file/dialogs/RestoreDialog.vue` 及 `composables/useRestoreDialog.ts`：还原弹窗（自动执行）
- [x] 6.9 创建 `components/file/dialogs/UnzipDialog.vue` 及 `composables/useUnzipDialog.ts`：解压弹窗（⚠️ 后端当前 4 Controller 无解压端点，此任务为 UI 预留，API 调用留空或 mock，待后端补充 unzip endpoint 后对接）
- [x] 6.10 创建 `components/file/dialogs/SaveShareDialog.vue` 及 `composables/useSaveShareDialog.ts`：保存分享文件弹窗

## 7. 上传面板与拖拽上传

- [x] 7.1 安装 spark-md5 依赖并添加类型声明
- [x] 7.2 创建 `components/file/UploadPanel.vue`：上传面板，固定右下角，显示上传任务列表和进度，支持折叠/展开/关闭
- [x] 7.3 完善 `views/FileView.vue` 中的拖拽上传遮罩：全屏 fixed、截图粘贴支持

## 8. 页面集成

- [x] 8.1 完善 `views/FileView.vue`：集成 OperationMenu、BreadCrumb、FileTable/FileGrid/FileTimeLine（按 fileModel 切换）、ContextMenu、UploadPanel、所有弹窗组件
- [x] 8.2 完善 `views/ShareView.vue`：集成分享信息获取、提取码验证、文件列表展示、下载、保存到我的网盘功能
- [x] 8.3 在 `views/FileView.vue` 中实现路由参数监听：根据 route.query.fileType 和 filePath 自动刷新文件列表

## 9. 样式与响应式

- [x] 9.1 确保所有组件样式与旧项目一致（SCSS 变量、间距、字体大小、颜色）
- [x] 9.2 确保移动端适配（screenWidth <= 768 时隐藏部分功能、drawer 侧边栏、网格布局调整）
