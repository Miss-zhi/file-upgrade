## ADDED Requirements

### Requirement: 文件列表数据管理
fileList store SHALL 管理当前页文件列表数据、分页状态、加载状态，并提供 fetchFileList 方法根据 fileType 调用不同 API。

#### Scenario: 获取普通文件列表
- **WHEN** 调用 `fetchFileList({ fileType: 0, filePath: '/', page: 0, size: 20 })`
- **THEN** 调用 `getFileList` API，更新 fileList、total、loading 状态

#### Scenario: 获取分类文件列表
- **WHEN** 调用 `fetchFileList({ fileType: 1, page: 0, size: 20 })`（图片分类）
- **THEN** 调用 `getFileListByCategory({ category: 'image' })` API

#### Scenario: fileType 到 category 的完整映射
- **GIVEN** fileType 枚举值与 API category 字符串的映射关系：
  - fileType=1 → category='image'（图片）
  - fileType=2 → category='document'（文档）
  - fileType=3 → category='video'（视频）
  - fileType=4 → category='music'（音频）
  - fileType=5 → category='other'（其他）
- **WHEN** 调用 `fetchFileList` 且 fileType 为 1~5
- **THEN** 使用上述映射将 fileType 转换为 category 字符串，调用 `getFileListByCategory`

#### Scenario: 获取回收站列表
- **WHEN** 调用 `fetchFileList({ fileType: 6, page: 0, size: 20 })`
- **THEN** 调用 `getRecycleList` API

#### Scenario: 获取我的分享列表
- **WHEN** 调用 `fetchFileList({ fileType: 8 })`
- **THEN** 调用 `getMyShares` API

#### Scenario: 清空文件列表
- **WHEN** 调用 `clearFileList()`
- **THEN** fileList 重置为空数组，total 重置为 0

### Requirement: 文件选择状态管理
fileList store SHALL 管理文件选择状态，支持单选、多选、全选、取消选择。

#### Scenario: 选择单个文件
- **WHEN** 用户点击文件行的选择框
- **THEN** 该文件加入 selectedFiles 数组

#### Scenario: 取消选择文件
- **WHEN** 用户取消选择已选中的文件
- **THEN** 该文件从 selectedFiles 数组中移除

#### Scenario: 全选当前页
- **WHEN** 用户点击表头全选框
- **THEN** 当前页所有文件加入 selectedFiles

#### Scenario: 清空选择
- **WHEN** 调用 `clearSelection()`
- **THEN** selectedFiles 重置为空数组，isBatchOperation 设为 false

### Requirement: 上传任务队列管理
uploadFile store SHALL 管理上传任务队列，支持添加、移除、更新进度、更新状态。

#### Scenario: 添加上传任务
- **WHEN** 调用 `addTask(file)`
- **THEN** 创建 UploadTask 并加入 uploadQueue，状态为 pending

#### Scenario: 更新上传进度
- **WHEN** 调用 `updateProgress(taskId, 50)`
- **THEN** 对应任务的 progress 更新为 50

#### Scenario: 上传成功
- **WHEN** 调用 `updateStatus(taskId, 'success')`
- **THEN** 对应任务状态更新为 success

#### Scenario: 上传失败
- **WHEN** 调用 `updateStatus(taskId, 'error', '网络错误')`
- **THEN** 对应任务状态更新为 error，errorMsg 记录错误信息

#### Scenario: 移除任务
- **WHEN** 调用 `removeTask(taskId)`
- **THEN** 该任务从队列中移除

### Requirement: 文件操作 composable
系统 SHALL 提供 `useFileOperations()` composable，封装文件操作（删除/移动/复制/重命名/分享）的 loading 状态和错误处理。

#### Scenario: 删除文件操作
- **WHEN** 调用 `deleteFile(fileId)`
- **THEN** 设置 loading 为 true，调用 API，成功后显示成功消息，刷新文件列表
- **THEN** 失败时显示错误消息

#### Scenario: 批量删除操作
- **WHEN** 调用 `batchDeleteFile(fileIds)`
- **THEN** 设置 loading 为 true，调用批量 API，显示操作结果

### Requirement: 上传管理 composable
系统 SHALL 提供 `useUploadManager()` composable，封装分片上传、秒传检测、MD5 计算、并发控制逻辑。

#### Scenario: 上传小文件（≤10MB）
- **WHEN** 调用 `upload(file)`，文件大小 ≤10MB
- **THEN** 计算 MD5，尝试秒传，秒传失败则普通上传

#### Scenario: 上传大文件（>10MB）
- **WHEN** 调用 `upload(file)`，文件大小 >10MB
- **THEN** 计算 MD5，尝试秒传，秒传失败则分片上传（init → chunk × N → merge）

#### Scenario: 配额不足
- **WHEN** 上传前检查配额，剩余空间 < 文件大小
- **THEN** 拒绝上传，显示配额不足错误

#### Scenario: 并发控制
- **WHEN** 同时上传多个文件
- **THEN** 最多 3 个文件并发上传，其余排队等待
