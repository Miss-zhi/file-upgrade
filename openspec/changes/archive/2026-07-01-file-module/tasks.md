## 1. 项目基础设施与数据模型

- [x] 1.1 创建 file 模块目录结构（controller / service / repository / entity / dto / vo / common）
- [x] 1.2 创建 storage 模块目录结构（factory / interface / impl / config / starter）
- [x] 1.3 编写 Flyway 迁移脚本 V4__create_file_tables.sql（FileBean、UserFile、UploadTask、UploadTaskDetail 表）
- [x] 1.4 V5 保留为空占位文件（V5__placeholder.sql），ShareFile 表已在 V4 中创建
- [x] 1.5 创建 JPA Entity：FileBean（文件存储元数据，含 fileHash、fileSize、storageType、storagePath）
- [x] 1.6 创建 JPA Entity：UserFile（用户文件关系，含 fileName、filePath、fileType、deleteStatus、userId、fileBeanId）
- [x] 1.7 创建 JPA Entity：UploadTask（上传任务，含 fileHash、fileName、totalChunks、status）
- [x] 1.8 创建 JPA Entity：UploadTaskDetail（分片详情，含 taskId、chunkIndex、chunkSize、status）
- [x] 1.9 创建 JPA Entity：ShareFile（分享记录，含 shareCode、extractCode、expireTime、fileId、userId）
- [x] 1.10 创建 JPA Repository 接口：FileBeanRepository、UserFileRepository、UploadTaskRepository、ShareFileRepository
- [x] 1.11 创建 DTO/VO：FileUploadRequest、FileListResponse、ShareCreateRequest、QuotaResponse 等
- [x] 1.12 创建 FileErrorCode 枚举（UPLOAD_SIZE_EXCEEDED、UPLOAD_QUOTA_EXCEEDED 等错误码）
- [x] 1.13 创建 FileModuleException 继承 BusinessException

## 2. UFOP 统一存储工厂

- [x] 2.1 定义 UFOP 7 个操作接口：Uploader、Downloader、Copier、Deleter、Previewer、Reader、Writer
- [x] 2.2 创建 StorageFactory 工厂类，根据 storage.type 配置返回对应存储后端实例
- [x] 2.3 创建 StorageProperties 配置类（storage.type、各后端连接参数）
- [x] 2.4 实现 Local 存储后端（LocalStorageAutoConfiguration + 5 个接口实现）
- [x] 2.5 创建 Local 存储后端 AutoConfiguration.imports 注册文件
- [x] 2.6 实现 MinIO 存储后端（MinioStorageAutoConfiguration + 5 个接口实现）
- [x] 2.7 创建 MinIO 存储后端 AutoConfiguration.imports 注册文件
- [x] 2.8 实现 AliyunOSS 存储后端（AliyunOssStorageAutoConfiguration + 5 个接口实现）
- [x] 2.9 创建 AliyunOSS 存储后端 AutoConfiguration.imports 注册文件
- [x] 2.10 实现 Qiniu 存储后端（QiniuStorageAutoConfiguration + 5 个接口实现）
- [x] 2.11 创建 Qiniu 存储后端 AutoConfiguration.imports 注册文件
- [x] 2.12 实现 FastDFS 存储后端（FastDfsStorageAutoConfiguration + 5 个接口实现）
- [x] 2.13 创建 FastDFS 存储后端 AutoConfiguration.imports 注册文件
- [x] 2.14 实现 StorageHealthChecker 启动时连通性验证（写入→读取→删除测试）
- [x] 2.15 配置 application.yml 存储后端参数

## 3. 存储配额管理

- [x] 3.1 创建 StorageQuotaService（Redis INCRBY/DECRBY 原子操作管理配额）
- [x] 3.2 实现 checkQuota 方法（上传前校验用户配额）
- [x] 3.3 实现 preDeduct 方法（上传开始预扣空间）
- [x] 3.4 实现 confirmQuota 方法（上传完成确认实际大小）
- [x] 3.5 实现 releaseQuota 方法（上传失败释放预扣空间）
- [x] 3.6 实现 getQuotaInfo 方法（查询用户配额信息）
- [x] 3.7 创建 QuotaSyncTask 定时任务（每小时同步 Redis 配额到 DB）
- [x] 3.8 创建 QuotaController（/api/v1/quota 端点）

## 4. 文件上传

- [x] 4.1 创建 FileUploadService（普通上传逻辑：hash 计算→去重检查→UFOP 上传→元数据创建）
- [x] 4.2 实现秒传接口（/api/v1/filetransfer/upload/speed：hash 匹配→复用 FileBean→创建 UserFile）
- [x] 4.3 实现普通上传接口（/api/v1/filetransfer/upload：≤10MB 文件直接上传）
- [x] 4.4 创建分片上传初始化接口（/api/v1/filetransfer/upload/chunk/init：创建 UploadTask）
- [x] 4.5 创建分片上传接口（/api/v1/filetransfer/upload/chunk：上传单个分片）
- [x] 4.6 创建分片合并接口（/api/v1/filetransfer/upload/chunk/merge：合并分片→创建 FileBean+UserFile）
- [x] 4.7 实现三层大小限制校验（Spring multipart 配置 + Service 层校验）
- [x] 4.8 实现上传配额预扣/确认/释放集成
- [x] 4.9 创建 FileTransferController（/api/v1/filetransfer/upload/* 端点）
- [x] 4.10 创建 UploadCleanupTask 定时任务（清理超时未完成的上传任务）

## 5. 文件下载

- [x] 5.1 创建 FileDownloadService（通过 UFOP Downloader 获取文件流）
- [x] 5.2 实现流式下载（< 50MB 直接流式返回，设置 Content-Type/Content-Disposition）
- [x] 5.3 实现断点续传下载（≥ 50MB 支持 Range 请求，返回 206 Partial Content）
- [x] 5.4 实现下载审计日志记录
- [x] 5.5 创建 FileTransferController（/api/v1/filetransfer/download/* 端点）

## 6. 文件 CRUD 操作

- [x] 6.1 创建 FileOperationService（文件列表查询、重命名、移动、创建文件夹）
- [x] 6.2 实现文件列表查询（按 filePath 前缀过滤，支持分页排序）
- [x] 6.3 实现文件重命名（更新 fileName + filePath，文件夹递归更新子文件路径）
- [x] 6.4 实现文件移动（更新 filePath，文件夹移动含子文件路径更新）
- [x] 6.5 实现创建文件夹（创建 fileType=DIRECTORY 的 UserFile 记录）
- [x] 6.6 实现文件详情查看
- [x] 6.7 创建 FileOperationController（/api/v1/file/* 端点）

## 7. 回收站

- [x] 7.1 创建 FileRecoveryService（软删除、恢复、永久删除）
- [x] 7.2 实现软删除（更新 deleteStatus=1，记录 deleteTime）
- [x] 7.3 实现批量软删除（事务中批量更新）
- [x] 7.4 实现回收站列表查询（deleteStatus=1 的文件，分页按删除时间降序）
- [x] 7.5 实现文件恢复（恢复 deleteStatus=0，检查同名冲突）
- [x] 7.6 实现永久删除（删除 UserFile → 异步检查 FileBean 引用 → UFOP Deleter 清理存储）
- [x] 7.7 创建 AsyncConfig 配置（fileTaskExecutor 线程池）
- [x] 7.8 创建 RecycleBinCleanupTask 定时任务（自动清理 >30 天的回收站文件）
- [x] 7.9 创建 RecoveryFileController（/api/v1/recycle/* 端点）

## 8. 文件分享

- [x] 8.1 创建 FileShareService（创建分享、验证提取码、查看分享、取消分享）
- [x] 8.2 实现创建分享链接（生成 8 位 shareCode + 4-6 位提取码 + 过期时间）
- [x] 8.3 实现查看分享内容（验证 shareCode + 提取码 + 过期检查）
- [x] 8.4 实现分享文件下载（验证后通过 UFOP Downloader 下载）
- [x] 8.5 实现分享列表管理（查看我的分享、取消分享）
- [x] 8.6 创建 ShareCleanupTask 定时任务（清理过期分享记录）
- [x] 8.7 创建 FileShareController（/api/v1/share/* 端点）

## 9. 全局异常处理与配置

- [x] 9.1 创建 FileGlobalExceptionHandler 扩展（处理 FileModuleException）
- [x] 9.2 配置 application.yml 文件模块参数（multipart 大小限制、UFOP 配置、异步线程池）
- [x] 9.3 配置 CORS 允许文件上传相关端点

## 10. 测试

- [x] 10.1 编写 FileUploadService 单元测试（普通上传、秒传、分片上传）
- [x] 10.2 编写 FileDownloadService 单元测试（流式下载、断点续传）
- [x] 10.3 编写 FileOperationService 单元测试（CRUD 操作）
- [x] 10.4 编写 FileRecoveryService 单元测试（软删除、恢复、永久删除）
- [x] 10.5 编写 FileShareService 单元测试（创建分享、验证提取码）
- [x] 10.6 编写 StorageQuotaService 单元测试（配额校验、预扣、释放）
- [x] 10.7 编写 LocalStorage 集成测试（UFOP 接口验证）
- [x] 10.8 编写 FileTransferController 集成测试（API 端点验证）

---
## Supplement: 补充任务

以下任务需追加到对应 Section 中。

### 追加到 Section 1（项目基础设施与数据模型）

- [x] 1.14 创建 StorageBean JPA Entity（userId、totalQuota、usedSize、preUsedSize），对应 storage_bean 表
- [x] 1.15 创建 AuditLog JPA Entity（userId、userFileId、action、ipAddress、userAgent、createTime），对应 audit_log 表
- [x] 1.16 创建 StorageBeanRepository（findByUserId）和 AuditLogRepository
- [x] 1.17 在 V4__create_file_tables.sql 中添加 user_file 唯一索引 `uk_user_path_name`（userId + filePath + fileName + extendName + deleteStatus + fileType），防止并发创建同名文件
- [x] 1.18 创建 BatchOperationResultVO record（successCount、failedItems 列表），用于批量操作统一响应

### 追加到 Section 6（文件 CRUD 操作）

- [x] 6.8 实现批量移动端点 POST /api/v1/file/batchmovefile（逐个移动，部分失败返回 BatchOperationResultVO）
- [x] 6.9 实现复制端点 POST /api/v1/file/copyfile（创建新 UserFile 复用 FileBean，文件夹复制含递归子文件）
- [x] 6.10 实现批量复制端点 POST /api/v1/file/batchcopyfile
- [x] 6.11 实现文件树端点 GET /api/v1/file/getfiletree（仅返回文件夹层级结构，排除 deleteStatus=1）
- [x] 6.12 实现按类型分类浏览（fileType 参数 + 无 filePath 时跨目录列出所有同类型文件）
- [x] 6.13 实现批量软删除端点 POST /api/v1/file/batchdeletefile（同一事务，共享 deleteBatchNum）

### 追加到 Section 5（文件下载）

- [x] 5.6 创建 AuditLogService（@Async 写入审计日志到 audit_log 表）
- [x] 5.7 在 FileDownloadService 中集成 AuditLogService：每次成功下载后异步记录 userId、userFileId、IP、User-Agent、时间
- [x] 5.8 分享文件下载成功后同样调用 AuditLogService，action="share_download"

### 新 Section 12: auth 模块补丁

- [x] 12.1 在 auth 模块添加 UserRegisteredEvent（Spring ApplicationEvent），在 AuthService.register 成功后发布，供 file 模块监听创建 StorageBean 初始配额记录

