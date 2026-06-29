# 上传任务管理：分片上传 + 进度追踪

## Why

当前文件上传缺少大文件支持和进度反馈。原项目（qiwen-file 旧版）有 UploadTask/UploadTaskDetail/FiletransferController 完整的分片上传体系。需要实现分片上传接口解决大文件上传问题，同时提供上传进度查询，前端显示实时进度条。

## What Changes

### 后端

1. **UploadTask Entity**（双 ORM）：identifier（MD5）/ userId / fileName / filePath / totalSize / chunkNum / totalChunks / uploadStatus / createTime
2. **UploadTaskMapper**
3. **IFiletransferService + FiletransferService**：uploadChunk（接收分片并写临时文件）、mergeChunks（合并分片生成最终文件）、getProgress（查询进度）、cleanupTask（清理临时文件）
4. **FiletransferController**：
   - POST /filetransfer/upload-chunk（分片上传，multipart + chunkNum/totalChunks/identifier）
   - POST /filetransfer/merge-chunks（合并分片，调用 FileService.upload）
   - GET /filetransfer/progress/{identifier}（轮询进度）
5. **临时文件目录**：`{ufop.local.root-path}/chunks/{identifier}/`
6. **FileService.upload 集成**：merge 后自动调用 FileService.upload 完成文件入库

### 前端

1. **UploadDialog.vue**（升级）：每个文件显示进度条（百分比+速度）、取消按钮、完成状态
2. **api/filetransfer.js**：uploadChunk / mergeChunks / getProgress
3. 分片策略：前端按 2MB 切片，逐片上传，全部完成后 merge

### 不涉及

- 断点续传（需前端 localStorage 记录，复杂度高，暂不实现）
- 秒传（需 MD5 去重查库）
- WebSocket 实时推送（使用轮询方式）

## Impact

| 类型 | 文件 |
|---|---|
| 新增 | UploadTask.java, UploadTaskMapper.java, IFiletransferService.java, FiletransferService.java, FiletransferController.java, api/filetransfer.js |
| 修改 | UploadDialog.vue（+进度条）, FileManager.vue（集成新的上传逻辑） |
| 新增测试 | FiletransferTest.java |
