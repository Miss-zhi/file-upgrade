# app-store — 状态管理

## Description

将旧项目的 5 个 Vuex module 迁移为 4 个 Pinia store（auth store 已由 auth-module 完成）。管理文件列表偏好、侧边栏状态、公共状态和上传状态。

## Requirements

### REQ-1: fileList Store

文件列表偏好状态管理，3 个字段持久化到 localStorage。

**Scenarios:**

- **selectedColumnList**：`string[]`，默认值 `['extendName', 'fileSize', 'uploadTime', 'deleteTime']`。控制文件表格显示哪些列。变更时写入 localStorage key `qiwen_selected_columns`
- **fileModel**：`0 | 1 | 2`（LIST=0 / GRID=1 / TIMELINE=2），默认值 0。控制文件列表显示模式。变更时写入 localStorage key `qiwen_file_model`
- **gridSize**：`number`，默认值 80（px）。控制网格/时间线模式的图标大小。变更时写入 localStorage key `qiwen_grid_size`
- **selectedFiles**：`unknown[]`，默认空数组。批量选中的文件列表。不持久化
- **isBatchOperation**：`boolean`，默认 false。是否处于批量操作模式。不持久化
- **初始化**：从 localStorage 读取已有值，不存在则使用默认值

### REQ-2: sideMenu Store

侧边栏状态管理，包括存储容量和折叠状态。

**Scenarios:**

- **storageValue**：`number`，默认 0。用户已使用的存储空间（字节）
- **totalStorageValue**：`number`，默认 0。用户的总存储配额（字节）
- **isCollapsed**：`boolean`。侧边栏折叠状态。从 localStorage key `qiwen_is_collapse` 读取初始值，变更时写入
- **fetchStorage() action**：调用 `getStorage()` API（GET `/api/v1/filetransfer/getstorage`），将返回的 `storageSize` 和 `totalStorageSize` 转为 Number 后更新状态。失败时通过 `ElMessage.error` 提示
- **storagePercentage getter**：计算 `(storageValue / totalStorageValue) * 100`，totalStorageValue 为 0 时返回 0

### REQ-3: common Store

公共应用状态管理。

**Scenarios:**

- **screenWidth**：`number`，初始值 `document.body.clientWidth`
- **updateScreenWidth()**：更新 screenWidth 为当前 `document.body.clientWidth`
- **窗口监听**：在 App.vue 的 `onMounted` 中注册 `window.addEventListener('resize', ...)` 调用 `updateScreenWidth()`

### REQ-4: uploadFile Store

上传文件状态管理（最简版本，仅控制遮罩显隐）。

**Scenarios:**

- **showUploadMask**：`boolean`，默认 false。拖拽上传遮罩的显隐状态
- **toggleUploadMask()**：切换 showUploadMask 的值

### REQ-5: 删除 Vite 模板 Store

删除 `stores/counter.ts`（Vite 脚手架模板文件）。

## Store Type Definitions

```typescript
// stores/fileList.ts
type FileViewMode = 0 | 1 | 2
interface FileListState {
  selectedColumnList: string[]
  fileModel: FileViewMode
  gridSize: number
  selectedFiles: unknown[]
  isBatchOperation: boolean
}

// stores/sideMenu.ts
interface SideMenuState {
  storageValue: number
  totalStorageValue: number
  isCollapsed: boolean
}

// stores/common.ts
interface CommonState {
  screenWidth: number
}

// stores/uploadFile.ts
interface UploadFileState {
  showUploadMask: boolean
}
```

## Dependencies

- Pinia: defineStore (setup function style)
- API: file.getStorage (for sideMenu.fetchStorage)
- Element Plus: ElMessage (for error notification)
- localStorage: persistence with `qiwen_` prefix
