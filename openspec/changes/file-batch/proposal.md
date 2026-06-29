# 文件批量操作

## Why
文件管理缺少批量操作能力，用户需逐一操作大量文件。需要支持多选后进行批量删除、批量移动、批量下载。

## What Changes

### 后端
1. **FileController** 新增批量端点：POST /file/batch-delete（接收 id 列表）、POST /file/batch-move（id 列表 + 目标路径）
2. **IFileService + FileService** 新增 batchDelete(List<String> ids)、batchMove(List<String> ids, String targetPath)

### 前端
1. **FileTable.vue**：添加 el-table selection（checkbox 多选）
2. **BatchToolbar.vue**：选中后浮现的批量操作栏（删除/移动）
3. **FileManager.vue**：集成批量操作逻辑

### 不涉及
- 批量下载打包（需 zip 库，复杂度高，暂不实现）
