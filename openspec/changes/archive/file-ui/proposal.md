# 文件管理 UI 增强

## Why

当前 File.vue 是简单的文件表格，缺少专业文件管理器应有的 UI 组件：左侧目录树导航、拖拽上传对话框、文件右键操作、重命名/移动/复制对话框。需要按照 AGENTS.md 约定的组件分层（views → components → stores → api）重新组织文件管理 UI。

## What Changes

### 前端新增/修改

1. **FileManager.vue**（新建，替代 File.vue）：左右分栏布局 — 左侧 AsideMenu 目录树 + 右侧文件操作区（工具栏 + 面包屑 + 文件列表）
2. **AsideMenu.vue**（新建）：目录树组件，支持展开/折叠，点击切换目录，高亮当前路径
3. **UploadDialog.vue**（新建 `components/file/dialog/`）：拖拽区域 + 点击选择文件 + 进度条 + 上传路径
4. **FileTable.vue**（重命名 FileList.vue → FileTable.vue）：文件表格，右键菜单（重命名/移动/复制/删除）
5. **RenameDialog.vue**（新建）：重命名对话框，输入新名称
6. **MoveDialog.vue**（新建）：移动到对话框，选择目标目录
7. **CopyDialog.vue**（新建）：复制到对话框，选择目标目录
8. **DeleteDialog.vue**（新建）：删除确认对话框（带文件信息）
9. **BreadCrumb.vue**（新建 `components/common/`）：公共面包屑组件
10. **stores/fileList.js**（完善）：对接新操作 API
11. **api/file.js**（完善）：增加 rename / move / copy API 调用

### 不涉及

- 不新增后端 API（复用 file-crud 已创建的端点）
- 不实现批量操作
- 不实现文件预览/分享

## Impact

- **新增**：FileManager.vue、AsideMenu.vue、UploadDialog.vue、RenameDialog.vue、MoveDialog.vue、CopyDialog.vue、DeleteDialog.vue、BreadCrumb.vue、FileTable.vue
- **修改**：stores/fileList.js、api/file.js、router/index.js
- **路由变更**：`/file` 改为指向 FileManager.vue
