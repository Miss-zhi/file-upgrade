## ADDED Requirements

### Requirement: 新建文件夹弹窗
系统 SHALL 提供 AddFolderDialog 组件，支持输入文件夹名称并创建。

#### Scenario: 打开新建文件夹弹窗
- **WHEN** 用户点击"新建文件夹"
- **THEN** 显示弹窗，标题"新建文件夹"，宽度 580px，包含 textarea 输入框

#### Scenario: 输入合法名称
- **WHEN** 用户输入合法文件夹名称（不含 `\/:*?"<>|`）
- **THEN** 确认按钮可用

#### Scenario: 输入非法名称
- **WHEN** 用户输入包含 `\/:*?"<>|` 的名称
- **THEN** 显示错误提示，确认按钮不可用

#### Scenario: 确认创建
- **WHEN** 用户点击确认
- **THEN** 调用 createFolder API，成功后关闭弹窗并刷新文件列表

### Requirement: 复制文件弹窗
系统 SHALL 提供 CopyFileDialog 组件，使用 el-tree 选择目标文件夹并复制。

#### Scenario: 打开复制文件弹窗
- **WHEN** 用户对文件执行"复制到"操作
- **THEN** 显示弹窗，包含 el-tree 展示目录结构

#### Scenario: 选择目标文件夹
- **WHEN** 用户在 tree 中选择目标文件夹
- **THEN** 确认按钮可用

#### Scenario: 确认复制
- **WHEN** 用户点击确认
- **THEN** 调用 copyFile API，成功后关闭弹窗并提示成功

### Requirement: 移动文件弹窗
系统 SHALL 提供 MoveFileDialog 组件，使用 el-tree 选择目标文件夹并移动。

#### Scenario: 打开移动文件弹窗
- **WHEN** 用户对文件执行"移动"操作
- **THEN** 显示弹窗，包含 el-tree 展示目录结构

#### Scenario: 批量移动
- **WHEN** 用户对多个选中文件执行移动
- **THEN** 调用 batchMoveFile API

### Requirement: 重命名弹窗
系统 SHALL 提供 RenameDialog 组件，支持修改文件名。

#### Scenario: 打开重命名弹窗
- **WHEN** 用户对文件执行"重命名"操作
- **THEN** 显示弹窗，宽度 550px，textarea 预填当前文件名

#### Scenario: 确认重命名
- **WHEN** 用户修改文件名并点击确认
- **THEN** 调用 renameFile API，成功后刷新文件列表

### Requirement: 删除弹窗
系统 SHALL 提供 DeleteDialog 组件，支持软删除和永久删除两种模式。

#### Scenario: 软删除确认
- **WHEN** 用户在普通视图删除文件（mode=1）
- **THEN** 显示"确定将文件移入回收站？"提示

#### Scenario: 永久删除警告
- **WHEN** 用户在回收站永久删除文件（mode=2）
- **THEN** 显示红色警告"永久删除后无法恢复，确定删除？"

#### Scenario: 批量删除
- **WHEN** 用户批量删除多个文件
- **THEN** 显示选中文件数量，调用批量删除 API

### Requirement: 分享弹窗
系统 SHALL 提供 ShareDialog 组件，两阶段 UI：配置 → 结果。

#### Scenario: 配置阶段
- **WHEN** 用户打开分享弹窗
- **THEN** 显示有效期选择（1天/7天/30天/永久）
- **NOTE** 当前后端 `ShareCreateDTO` 仅接受 `userFileId` + `expireType`，提取码由后端自动生成。前端不显示"是否设置提取码"选项。若后续后端支持自定义提取码，再补充此 UI。

#### Scenario: 生成分享链接
- **WHEN** 用户点击"创建分享"
- **THEN** 调用 createShare API，切换到结果阶段

#### Scenario: 结果阶段
- **WHEN** 分享创建成功
- **THEN** 显示分享链接、提取码、复制按钮

### Requirement: 文件详情弹窗
系统 SHALL 提供 FileDetailDialog 组件，只读展示文件详细信息。

#### Scenario: 显示文件详情
- **WHEN** 用户打开文件详情弹窗
- **THEN** 显示图标、文件名、路径、类型、大小、上传时间

### Requirement: 还原弹窗
系统 SHALL 提供 RestoreDialog 组件，从回收站恢复文件。

#### Scenario: 自动执行还原
- **WHEN** 用户打开还原弹窗
- **THEN** 自动调用 restoreFile API，显示 loading 状态

#### Scenario: 还原成功
- **WHEN** 还原成功
- **THEN** 显示成功消息，关闭弹窗，刷新回收站列表

### Requirement: 解压弹窗
系统 SHALL 提供 UnzipDialog 组件，支持解压缩文件。

#### Scenario: 自动解压
- **WHEN** 用户对 zip/rar/7z 文件执行解压（mode=0/1）
- **THEN** 自动执行解压，显示 spinner

#### Scenario: 选择目标路径解压
- **WHEN** mode=2
- **THEN** 显示 tree 选择目标路径

### Requirement: 保存分享文件弹窗
系统 SHALL 提供 SaveShareDialog 组件，将分享文件保存到用户网盘。

#### Scenario: 选择目标路径保存
- **WHEN** 登录用户点击"保存到我的网盘"
- **THEN** 显示 tree 选择目标路径，确认后保存
