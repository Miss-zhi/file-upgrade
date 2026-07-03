## ADDED Requirements

### Requirement: 上传面板组件
系统 SHALL 提供 UploadPanel 组件，固定定位在右下角，显示上传任务列表和进度。

#### Scenario: 显示上传面板
- **WHEN** uploadFile store 中有上传任务
- **THEN** 显示上传面板，固定定位 right 16px, bottom 16px, z-index 20

#### Scenario: 面板样式
- **WHEN** 渲染上传面板
- **THEN** 宽度 560px，标题栏 40px，文件列表 240px 可滚动，白色背景，border #e2e2e2

#### Scenario: 折叠/展开/关闭
- **WHEN** 用户点击折叠按钮
- **THEN** 面板折叠为标题栏
- **WHEN** 用户点击展开按钮
- **THEN** 面板展开显示文件列表
- **WHEN** 用户点击关闭按钮
- **THEN** 面板隐藏

#### Scenario: 显示上传进度
- **WHEN** 有上传任务在进行
- **THEN** 每个任务显示文件名、进度条、状态（计算MD5/上传中/成功/失败）

### Requirement: 普通上传（≤10MB）
系统 SHALL 对 ≤10MB 文件使用普通上传，先计算 MD5 尝试秒传。

#### Scenario: 秒传成功
- **WHEN** 文件 MD5 在服务端已存在
- **THEN** 秒传成功，任务状态直接变为 success

#### Scenario: 秒传失败转普通上传
- **WHEN** 文件 MD5 在服务端不存在
- **THEN** 使用 FormData POST 上传完整文件

### Requirement: 分片上传（>10MB）
系统 SHALL 对 >10MB 文件使用分片上传，每片 1MB。

#### Scenario: 初始化分片任务
- **WHEN** 文件 >10MB 且秒传失败
- **THEN** 调用 initChunkUpload，获取 taskId

#### Scenario: 并发上传分片
- **WHEN** 获取到 taskId
- **THEN** 将文件切分为 1MB 分片，最多 3 个并发上传

#### Scenario: 分片失败重试
- **WHEN** 单个分片上传失败
- **THEN** 重试最多 3 次，指数退避

#### Scenario: 合并分片
- **WHEN** 所有分片上传成功
- **THEN** 调用 mergeChunks 完成上传

### Requirement: MD5 计算
系统 SHALL 使用 SparkMD5 计算文件 MD5，用于秒传和分片校验。

#### Scenario: 计算小文件 MD5
- **WHEN** 文件 ≤10MB
- **THEN** 直接读取完整文件内容计算 MD5

#### Scenario: 计算大文件 MD5
- **WHEN** 文件 >10MB
- **THEN** 分块读取（1MB 块）计算 MD5，显示"计算MD5"进度

### Requirement: 拖拽上传遮罩
系统 SHALL 提供全屏拖拽上传遮罩，支持拖拽和截图粘贴。

#### Scenario: 显示拖拽遮罩
- **WHEN** 用户拖拽文件进入窗口
- **THEN** 显示全屏 fixed 遮罩，z-index 19，border 5px dashed #8091a5

#### Scenario: 释放文件上传
- **WHEN** 用户在遮罩上释放文件
- **THEN** 关闭遮罩，将文件加入上传队列

#### Scenario: 截图粘贴上传
- **WHEN** 用户在遮罩中按 Ctrl+V 粘贴截图
- **THEN** 从 clipboard 获取图片，加入上传队列

### Requirement: 配额校验
系统 SHALL 在上传前检查用户存储配额。

#### Scenario: 配额充足
- **WHEN** 剩余存储空间 ≥ 文件总大小
- **THEN** 允许上传

#### Scenario: 配额不足
- **WHEN** 剩余存储空间 < 文件总大小
- **THEN** 拒绝上传，显示"存储空间不足"错误
