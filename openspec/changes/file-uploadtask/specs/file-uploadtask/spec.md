# 上传任务管理 — 详细需求规格

## ADDED Requirements

### Requirement: 分片上传接口
POST /filetransfer/upload-chunk SHALL accept MultipartFile chunk + query params (chunkNum, totalChunks, identifier, relativePath) and save chunk to temporary directory

#### Scenario: 上传第一个分片
- **GIVEN** 文件 totalChunks=5, identifier="abc123"
- **WHEN** 上传 chunk 0 到 /filetransfer/upload-chunk
- **THEN** 分片写入 `uploads/chunks/abc123/0`，UploadTask.uploadStatus=0

#### Scenario: 上传最后一个分片
- **GIVEN** chunkNum=4 是最后一个分片
- **WHEN** 上传 chunk 4
- **THEN** 所有分片已就位，UploadTask.uploadStatus=0，等待 merge

### Requirement: 合并分片接口
POST /filetransfer/merge-chunks SHALL accept { identifier, filePath } and merge all chunks into the target file, then call FileService.upload to register the file

#### Scenario: 合并 5 个分片
- **GIVEN** 5 个分片全部上传完毕
- **WHEN** 调用 merge-chunks { identifier: "abc123", filePath: "/docs/report.pdf" }
- **THEN** 分片合并为完整文件，调用 FileService.upload，UploadTask.uploadStatus=1，清理临时分片

### Requirement: 进度查询
GET /filetransfer/progress/{identifier} SHALL return { chunkNum, totalChunks, uploadStatus, fileName } for polling

#### Scenario: 轮询进度
- **GIVEN** 上传了 3/5 个分片
- **WHEN** GET /filetransfer/progress/abc123
- **THEN** 返回 { chunkNum: 3, totalChunks: 5, uploadStatus: 0 }

### Requirement: 前端进度条
UploadDialog.vue SHALL display per-file progress bar (el-progress) with percentage, speed indicator, and cancel button during chunked upload

#### Scenario: 上传大文件显示进度
- **GIVEN** 选择 10MB 文件，分 5 片
- **WHEN** 逐片上传中
- **THEN** 每片完成后进度条更新 20% → 40% → 60% → 80% → 100%

### Requirement: 前端分片策略
UploadDialog SHALL split files > 2MB into 2MB chunks, upload sequentially with a concurrency of 1 for simplicity

#### Scenario: 5MB 文件分片
- **GIVEN** 选择 5MB 文件
- **WHEN** 开始上传
- **THEN** 前端切分为 3 片（2MB+2MB+1MB），逐片 POST upload-chunk

### Requirement: CI 兼容
Backend SHALL pass mvn test, frontend SHALL pass vue-tsc + vite build

#### Scenario: 全量测试通过
- **WHEN** 执行 mvn test + vue-tsc + vite build
- **THEN** 全部成功
