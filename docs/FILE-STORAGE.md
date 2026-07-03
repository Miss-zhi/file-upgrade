# 文件上传与存储规则

## 上传大小限制

三层一致配置，文档中明确记录限制值：

1. **前端**：表单提交前校验文件大小，超出时给用户明确提示
2. **Nginx/Gateway**：`client_max_body_size` 配置
3. **Spring**：`spring.servlet.multipart.max-file-size` 和 `max-request-size`

三层值必须一致。修改限制时必须同步更新三层配置。

## 配额校验

每次上传前必须调用 `StorageService.checkQuota(userId, fileSize)` 校验用户配额。

配额计算必须考虑在途上传（已分配但未完成的空间）：
- 上传开始时预扣空间
- 上传完成时确认实际大小
- 上传失败或取消时释放预扣空间

## 分片上传

大于 10MB 的文件强制使用分片/断点续传协议：

- 每个分片独立超时和重试（最多 3 次指数退避重试）
- 服务端通过 `UploadTask` + `UploadTaskDetail` 追踪上传进度
- 分片大小建议 5MB，最后一个分片可以小于 5MB
- 所有分片上传完成后合并，更新文件元数据
- 超时未完成的上传任务定期清理（定时任务）

## 文件去重

文件存储采用内容寻址：

1. 上传前计算文件 hash（MD5 或 SHA-256）
2. 检查 hash 是否已存在于存储中
3. 若存在：复用已有存储对象，仅创建新的文件元数据引用（`FileBean` + `UserFile`）
4. 若不存在：写入存储，创建元数据

秒传接口（`/filetransfer/uploadFileSpeed`）利用此机制：客户端发送文件 hash，服务端检查后直接创建引用，跳过实际上传。

## 错误码规范

上传失败返回结构化错误码，禁止笼统的"超时"包装：

| 错误码 | HTTP 状态 | 触发条件 |
|--------|----------|---------|
| `UPLOAD_SIZE_EXCEEDED` | 413 | 文件超过大小限制 |
| `UPLOAD_QUOTA_EXCEEDED` | 507 | 用户配额不足 |
| `UPLOAD_FORMAT_REJECTED` | 415 | 文件格式不允许 |
| `UPLOAD_AUTH_FAILED` | 401 | 认证失败 |
| `UPLOAD_STORAGE_ERROR` | 503 | 存储后端写入失败 |
| `UPLOAD_NETWORK_ERROR` | 502 | 网络中断 |
| `UPLOAD_CHUNK_MISMATCH` | 400 | 分片序号或 hash 不匹配 |
| `UPLOAD_DUPLICATE` | 409 | 同名文件已存在（同目录下） |

## 存储后端验证

存储后端配置在应用启动时做连通性验证：

1. 写入测试：写入一个小文件（1KB）
2. 读取测试：读回并校验内容
3. 删除测试：清理测试文件

验证失败时：
- 核心存储后端（当前激活的 storage-type）→ 阻止应用启动
- 非激活的存储后端 → 记录警告，允许启动

每个存储后端所需的 IAM 权限在部署文档中明确记录：

| 后端 | 所需权限 |
|------|---------|
| 阿里云 OSS | `oss:PutObject`, `oss:GetObject`, `oss:DeleteObject`, `oss:ListObjects` |
| MinIO | `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` |
| 七牛云 Kodo | Bucket 写入权限 |
| FastDFS | storage 写入权限 |

## UFOP 工厂模式约束

UFOP 统一文件操作框架的 7 个操作接口：Uploader、Downloader、Copier、Deleter、Previewer、Reader、Writer。

每个操作必须：
- 在操作前校验存储后端连通性（快速失败）
- 在操作后更新元数据（成功时更新，失败时记录）
- 资源在 `finally` 块中释放（stream、connection）
- 大文件操作使用流式处理，禁止全量加载到内存

## 文件下载

通过 UFOP Downloader 抽象，支持所有存储后端：
- 小文件（< 50MB）直接流式返回
- 大文件（≥ 50MB）支持 Range 请求（断点续传下载）
- 下载操作记录审计日志

## 文件删除

删除文件分两步：
1. 软删除：将文件移入回收站（更新 `FileBean` 状态）
2. 永久删除：从回收站删除时，通过 UFOP Deleter 清理存储对象 + 删除元数据

永久删除是异步操作（通过 `AsyncTaskComp`），避免阻塞用户请求。
