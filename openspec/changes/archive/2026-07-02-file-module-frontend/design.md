## Context

前端升级第三阶段：基于已完成的 file-module 后端（30 个端点，4 个 Controller）和 frontend-base 骨架（布局/路由/store 壳/API 壳/主题），构建文件管理的完整前端交互。

当前状态：
- `api/file.ts` 仅有 `getStorage()` 一个函数
- `stores/fileList.ts` 仅持久化偏好（selectedColumnList / fileModel / gridSize），无文件数据
- `stores/uploadFile.ts` 仅有 `showUploadMask` 布尔值
- `views/FileView.vue` 是壳页面，仅有拖拽上传遮罩
- `views/ShareView.vue` 是壳页面，仅有提取码输入框
- 无 `components/file/` 目录，无 composable

后端 API 已就绪：
- FileController (11 端点): getfilelist, getfilelist/bycategory, renamefile, movefile, batchmovefile, copyfile, batchcopyfile, createfold, createfile, getfiledetail/{id}, getfiletree
- FileTransferController (7 端点): upload, upload/speed, upload/chunk/init, upload/chunk, upload/chunk/merge, download/{id}, batch-download
- RecoveryFileController (6 端点): list, deletefile, batchdeletefile, restorefile, deletepermanent, deleteall
- FileShareController (6 端点): createshare, info/{code}, verifyshare, download/{code}, myshares, cancelshare/{id}

约束：
- Vue 3 + Element Plus + Pinia + TypeScript + SCSS
- 1:1 还原旧项目交互和视觉样式
- 不涉及文件预览（留给 frontend-preview）
- 新增依赖：spark-md5（MD5 计算用于秒传）

## Goals / Non-Goals

**Goals:**
- 补全 API 客户端，封装后端 30 个端点
- 增强 Pinia stores，管理文件列表数据和上传任务队列
- 实现文件列表三种展示模式（表格/网格/时间线）
- 实现工具栏（上传/新建按钮组、搜索框、视图切换、设置弹窗）
- 实现面包屑导航（可编辑路径）
- 实现右键菜单（文件右键 + 空白区域右键）
- 实现 10 个操作弹窗（新建文件夹/复制/移动/重命名/删除/分享/详情/还原/解压/保存分享）
- 实现上传面板（普通上传/秒传/分片上传/拖拽上传/截图粘贴）
- 完善 ShareView 分享页面

**Non-Goals:**
- 6 种文件预览（图片/视频/音频/代码/Markdown/OnlyOffice）
- 文件搜索功能（前端搜索 UI）
- 微信登录集成
- 修改密码对话框

## Decisions

### 1. 组件目录结构

采用按功能分组的组件结构：

```
components/file/
├── FileTable.vue          # 表格视图
├── FileGrid.vue           # 网格视图
├── FileTimeLine.vue       # 时间线视图
├── OperationMenu.vue      # 工具栏
├── BreadCrumb.vue         # 面包屑导航
├── SelectColumn.vue       # 列显隐设置
├── ContextMenu.vue        # 右键菜单
├── UploadPanel.vue        # 上传面板
├── Pagination.vue         # 分页组件
└── dialogs/
    ├── AddFolderDialog.vue
    ├── CopyFileDialog.vue
    ├── MoveFileDialog.vue
    ├── RenameDialog.vue
    ├── DeleteDialog.vue
    ├── ShareDialog.vue
    ├── FileDetailDialog.vue
    ├── RestoreDialog.vue
    ├── UnzipDialog.vue
    └── SaveShareDialog.vue
```

**理由：** 按功能分组而非按类型分组，使文件管理相关组件集中管理，便于维护和代码导航。弹窗单独子目录，因为数量多且逻辑独立。

### 2. 弹窗模式：Composable + v-model 替代 imperative API

旧项目使用 Vue.extend + Promise 的 imperative 模式（`$openBox` / `$openDialog`）。新项目采用 Vue 3 推荐的 composable + v-model 模式：

```typescript
// composables/useAddFolderDialog.ts
export function useAddFolderDialog() {
  const visible = ref(false)
  const resolveRef = ref<(value: boolean) => void>()
  
  function open(): Promise<boolean> {
    visible.value = true
    return new Promise(resolve => { resolveRef.value = resolve })
  }
  
  function confirm(): void {
    visible.value = false
    resolveRef.value?.(true)
  }
  
  function cancel(): void {
    visible.value = false
    resolveRef.value?.(false)
  }
  
  return { visible, open, confirm, cancel }
}
```

**理由：** 
- Vue 3 Composition API 推荐模式，类型安全
- 弹窗渲染在 FileView 组件内，非 document.body 动态挂载，便于状态管理
- Promise 返回值保持调用处的代码简洁
- 每个弹窗独立 composable，职责单一

**替代方案：** 使用 Element Plus 的 `ElMessageBox` 或全局弹窗管理器。但旧项目弹窗逻辑复杂（如 ShareDialog 两阶段、CopyFileDialog 带 tree），不适合用 ElMessageBox 简化。

### 3. 文件列表数据流

采用 Pinia store 作为单一数据源，组件通过 store 订阅数据：

```
FileView.vue
├── el-aside: AppAside（已有，frontend-base 提供，保持不变）
├── el-main:
│   ├── 读取 route.query.fileType / filePath
│   ├── 调用 fileListStore.fetchFileList(params)
│   ├── 根据 fileModel 切换 FileTable / FileGrid / FileTimeLine
│   └── 组件从 store 读取 fileList / total / loading
└── 弹窗组件渲染在 el-main 内
```

**fileList store 增强：**
- `fileList: FileInfo[]` — 当前页文件列表
- `total: number` — 文件总数
- `loading: boolean` — 加载状态
- `currentPage: number` — 当前页码
- `fetchFileList(params)` — 根据 fileType 调用不同 API：
  - fileType=0（全部文件）→ `GET /file/getfilelist`
  - fileType=1（图片）→ `GET /file/getfilelist/bycategory?category=image`
  - fileType=2（文档）→ `GET /file/getfilelist/bycategory?category=document`
  - fileType=3（视频）→ `GET /file/getfilelist/bycategory?category=video`
  - fileType=4（音频）→ `GET /file/getfilelist/bycategory?category=music`
  - fileType=5（其他）→ `GET /file/getfilelist/bycategory?category=other`
  - fileType=6（回收站）→ `GET /recycle/list`
  - fileType=8（我的分享）→ `GET /share/myshares`

**理由：** 
- Store 作为单一数据源，避免组件间 prop drilling
- fetchFileList 内部根据 fileType 路由到不同 API，组件无需关心 API 差异
- 分页状态在 store 中管理，切换页面时自动更新

### 4. 上传架构

采用 composable 封装上传逻辑，store 管理上传队列：

```
useUploadManager()
├── 计算 MD5（SparkMD5，Web Worker 中执行避免阻塞 UI）
├── 秒传检测（POST /upload/speed）
├── 分片上传（init → chunk × N → merge）
├── 并发控制（最多 3 个并发上传）
└── 失败重试（最多 3 次指数退避）

uploadFile store
├── uploadQueue: UploadTask[]
├── addTask / removeTask / updateProgress
└── 持久化到 sessionStorage（页面刷新后恢复）
```

**MD5 计算策略：**
- 小文件（≤10MB）：直接计算完整 MD5，用于秒传检测
- 大文件（>10MB）：计算完整 MD5 用于秒传，同时分片上传

**理由：**
- SparkMD5 是成熟的前端 MD5 库，兼容性好
- Web Worker 避免大文件 MD5 计算阻塞 UI 线程
- 上传队列在 store 中管理，UploadPanel 组件订阅队列状态

**替代方案：** 使用 Web Crypto API 的 SHA-256。但后端秒传接口使用 MD5（与旧项目兼容），前端必须计算 MD5。

### 5. 右键菜单实现

采用 position fixed + Teleport 实现右键菜单：

```vue
<!-- ContextMenu.vue -->
<Teleport to="body">
  <div v-if="visible" class="context-menu" :style="{ left: x + 'px', top: y + 'px' }">
    <!-- 菜单项 -->
  </div>
</Teleport>
```

**智能定位：**
- 计算菜单尺寸，判断下方/右侧空间是否充足
- 下方空间不足时向上展开
- 右侧不足 138px 时向左展开

**理由：**
- Teleport 避免 z-index 层级问题
- position fixed 确保菜单始终在视口内
- 智能定位提升用户体验

### 6. API 客户端结构

按 Controller 分组，统一在 `api/file.ts` 中导出：

```typescript
// api/file.ts
// FileController
export async function getFileList(params: FileListParams): Promise<PageResult<FileInfo>> { ... }
export async function getFileListByCategory(params: CategoryListParams): Promise<PageResult<FileInfo>> { ... }
export async function renameFile(dto: RenameFileDTO): Promise<void> { ... }
export async function moveFile(dto: MoveFileDTO): Promise<void> { ... }
export async function batchMoveFile(dto: BatchMoveFileDTO): Promise<BatchOperationResult> { ... }
export async function copyFile(dto: CopyFileDTO): Promise<void> { ... }
export async function batchCopyFile(dto: BatchCopyFileDTO): Promise<BatchOperationResult> { ... }
export async function createFolder(dto: CreateFoldDTO): Promise<number> { ... }
export async function createFile(dto: CreateFileDTO): Promise<number> { ... }
export async function getFileDetail(userFileId: number): Promise<FileDetail> { ... }
export async function getFileTree(): Promise<TreeNode[]> { ... }

// FileTransferController
export async function uploadFile(file: File, filePath: string): Promise<UploadResult> { ... }
export async function speedUpload(dto: SpeedUploadDTO): Promise<UploadResult | null> { ... }
export async function initChunkUpload(dto: ChunkUploadInitDTO): Promise<string> { ... }
export async function uploadChunk(taskId: string, chunkIndex: number, chunkData: Blob): Promise<void> { ... }
export async function mergeChunks(taskId: string, filePath: string): Promise<UploadResult> { ... }
export function downloadFile(userFileId: number): void { ... }
export function batchDownload(userFileIds: number[]): void { ... }

// RecoveryFileController
export async function getRecycleList(page: number, size: number): Promise<PageResult<FileInfo>> { ... }
export async function deleteFile(userFileId: number): Promise<void> { ... }
export async function batchDeleteFile(userFileIds: number[]): Promise<void> { ... }
export async function restoreFile(userFileIds: number[]): Promise<void> { ... }
export async function deletePermanent(userFileIds: number[]): Promise<void> { ... }
export async function deleteAllRecycle(): Promise<void> { ... }

// FileShareController
export async function createShare(dto: ShareCreateDTO): Promise<ShareInfo> { ... }
export async function getShareInfo(shareCode: string): Promise<ShareInfo> { ... }
export async function verifyShare(shareCode: string, extractCode: string): Promise<ShareInfo> { ... }
export function downloadShareFile(shareCode: string): void { ... }
export async function getMyShares(): Promise<ShareInfo[]> { ... }
export async function cancelShare(shareId: number): Promise<void> { ... }
```

**理由：**
- 所有文件相关 API 集中在一个文件，便于查找和维护
- 函数命名与后端端点对应，降低心智负担
- 下载类函数不返回 Promise，直接触发浏览器下载

### 7. 类型定义策略

在 `types/file.ts` 中补充前端类型，与后端 VO/DTO 对应：

```typescript
// 前端 FileInfo（对应 FileListVO）
export interface FileInfo {
  userFileId: number
  fileName: string
  filePath: string
  fileType: number  // 1=file, 2=folder
  fileSize: number
  extendName: string
  uploadTime: string
  modifyTime: string
  deleteStatus: number
}

// 分页结果
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// 各 DTO 类型...
```

**理由：**
- 前端类型与后端 VO/DTO 一一对应，便于类型检查
- PageResult 泛型封装，复用于所有分页查询
- 枚举值使用 number 类型，与后端一致

## Risks / Trade-offs

### 1. SparkMD5 依赖

**风险：** 新增 spark-md5 依赖，增加打包体积（~20KB gzipped）。

**缓解：** 
- 仅在上传时使用，非全局依赖
- 可通过 dynamic import 延迟加载
- 考虑使用 Web Worker 避免阻塞 UI

### 2. 右键菜单兼容性

**风险：** 移动端无右键事件，需要长按触发。

**缓解：**
- 移动端使用长按（touchstart + 500ms 定时器）模拟右键
- 或者移动端禁用右键菜单，使用操作按钮替代

### 3. 分片上传复杂度

**风险：** 分片上传涉及 MD5 计算、并发控制、失败重试、断点续传，逻辑复杂。

**缓解：**
- 使用 composable 封装，逻辑集中
- 充分的单元测试覆盖核心逻辑
- 上传队列状态可视化，便于调试

### 4. 1:1 还原旧项目样式

**风险：** 旧项目使用 Stylus，新项目使用 SCSS，变量和混入语法不同。

**缓解：**
- 已建立 SCSS 变量体系（$primary, $secondary-text 等）
- 逐个组件对照旧项目样式，确保像素级还原
- 使用浏览器开发者工具比对

### 5. 弹窗数量多

**风险：** 10 个弹窗组件，代码量较大。

**缓解：**
- 弹窗 composable 模式统一，可快速开发
- 部分弹窗逻辑相似（CopyFileDialog / MoveFileDialog 都带 tree），可抽取公共逻辑
- 按优先级分批实现，核心弹窗优先
