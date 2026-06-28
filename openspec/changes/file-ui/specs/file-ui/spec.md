# 文件管理 UI 增强 — 目录树 + 上传/操作对话框

## Purpose

增强文件管理前端 UI：左右分栏布局（目录树+文件表格）、拖拽上传对话框、文件操作对话框（重命名/移动/复制/删除）、面包屑导航，全部使用 script setup + Stylus scoped。

## ADDED Requirements

### Requirement: 左右分栏布局
FileManager.vue SHALL render a left sidebar (240px) with AsideMenu directory tree and a right area with BreadCrumb + toolbar + FileTable

#### Scenario: 页面加载
- **GIVEN** 用户导航到 /file
- **WHEN** FileManager.vue 挂载
- **THEN** 左侧显示目录树，右侧显示根目录文件列表

### Requirement: 目录树
AsideMenu.vue SHALL use el-tree to display folder hierarchy, highlight current path, and emit navigation events on click

#### Scenario: 点击目录
- **GIVEN** 目录树已加载
- **WHEN** 点击某个文件夹节点
- **THEN** 右侧文件列表切换到该目录内容

### Requirement: 面包屑导航
BreadCrumb.vue SHALL render clickable path segments from root to current directory

#### Scenario: 面包屑点击
- **GIVEN** 当前路径为 /docs/notes
- **WHEN** 点击 "docs"
- **THEN** 文件列表切换到 /docs/ 目录

### Requirement: 上传对话框
UploadDialog.vue SHALL support drag-and-drop zone and click-to-select, with path display, progress bar, and confirm/cancel buttons

#### Scenario: 拖拽上传
- **GIVEN** 上传对话框打开
- **WHEN** 用户拖拽文件到拖拽区域
- **THEN** 显示文件名和大小，可点击确认上传

### Requirement: 文件操作对话框
RenameDialog / MoveDialog / CopyDialog / DeleteDialog SHALL each use el-dialog with appropriate form fields and provide confirm/cancel actions

#### Scenario: 重命名
- **GIVEN** 右键点击文件选择重命名
- **WHEN** 输入新文件名并确认
- **THEN** 调用 rename API，刷新列表

#### Scenario: 删除确认
- **GIVEN** 右键选择删除
- **WHEN** 确认删除对话框
- **THEN** 调用 delete API，文件从列表移除

### Requirement: 文件表格右键菜单
FileTable.vue SHALL provide context menu with Rename/Move/Copy/Delete options via el-dropdown or @contextmenu

#### Scenario: 右键菜单显示
- **GIVEN** 文件表格有数据
- **WHEN** 右键点击某行
- **THEN** 弹出操作菜单

### Requirement: CI 兼容
Frontend SHALL pass npx vue-tsc --noEmit and npx vite build without errors

#### Scenario: 类型检查通过
- **WHEN** 执行 npx vue-tsc --noEmit
- **THEN** 0 type errors

#### Scenario: 生产构建通过
- **WHEN** 执行 npx vite build
- **THEN** dist/ 目录生成，无编译错误
