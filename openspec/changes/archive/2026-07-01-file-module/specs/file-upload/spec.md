## ADDED Requirements

### Requirement: 普通文件上传
系统 SHALL 支持单文件上传，文件大小不超过系统配置的最大限制（默认 500MB）。上传前 MUST 校验用户存储配额，配额不足时拒绝上传并返回 `UPLOAD_QUOTA_EXCEEDED` 错误。

#### Scenario: 成功上传小文件（≤10MB）
- **WHEN** 认证用户上传一个 ≤10MB 的文件
- **THEN** 系统计算文件 SHA-256 hash，检查是否已存在相同 hash 的 FileBean
- **THEN** 若不存在，通过 UFOP Uploader 写入存储，创建 FileBean 和 UserFile 记录
- **THEN** 返回文件元数据（fileId、fileName、fileSize、createTime）

#### Scenario: 文件超过大小限制
- **WHEN** 用户上传文件超过系统配置的最大大小限制
- **THEN** 系统返回 HTTP 413 和错误码 `UPLOAD_SIZE_EXCEEDED`

#### Scenario: 配额不足
- **WHEN** 用户已用空间 + 文件大小超过配额上限
- **THEN** 系统返回 HTTP 507 和错误码 `UPLOAD_QUOTA_EXCEEDED`

### Requirement: 分片上传
系统 SHALL 对大于 10MB 的文件强制使用分片上传。分片大小固定为 5MB，最后一个分片可以小于 5MB。每个分片独立上传，服务端通过 UploadTask + UploadTaskDetail 追踪进度。

#### Scenario: 初始化分片上传任务
- **WHEN** 客户端请求上传 >10MB 文件并发送初始化请求（包含 fileName、fileSize、fileHash、totalChunks）
- **THEN** 系统创建 UploadTask 记录，返回 taskId 和已上传的分片列表（支持断点续传）

#### Scenario: 上传单个分片
- **WHEN** 客户端上传第 N 个分片（包含 taskId、chunkIndex、chunkData）
- **THEN** 系统校验分片序号和大小，通过 UFOP 写入临时存储，更新 UploadTaskDetail 状态
- **THEN** 返回该分片上传成功状态

#### Scenario: 分片序号或 hash 不匹配
- **WHEN** 客户端上传的分片序号与预期不符或 hash 校验失败
- **THEN** 系统返回 HTTP 400 和错误码 `UPLOAD_CHUNK_MISMATCH`

#### Scenario: 完成分片上传合并
- **WHEN** 所有分片上传完成后客户端发送合并请求
- **THEN** 系统按序号合并分片为完整文件，计算合并后文件 hash 并与初始化时的 hash 校验
- **THEN** 校验通过后创建 FileBean 和 UserFile，清理分片临时数据

#### Scenario: 分片上传失败重试
- **WHEN** 单个分片上传失败
- **THEN** 客户端可重试该分片（最多 3 次指数退避），不影响已成功的其他分片

### Requirement: 秒传（文件去重）
系统 SHALL 支持秒传功能。客户端发送文件 hash，服务端检查是否已存在相同 hash 的 FileBean。若存在则直接创建 UserFile 引用，跳过实际文件传输。

#### Scenario: 秒传成功（文件已存在）
- **WHEN** 客户端发送秒传请求（包含 fileHash、fileName、fileSize），且服务端已存在相同 hash 的 FileBean
- **THEN** 系统复用已有 FileBean，创建新的 UserFile 引用
- **THEN** 返回秒传成功标识和文件元数据，无需实际传输文件内容

#### Scenario: 秒传失败（文件不存在）
- **WHEN** 客户端发送秒传请求，但服务端不存在相同 hash 的 FileBean
- **THEN** 系统返回需要普通上传的标识，客户端切换到普通/分片上传流程

### Requirement: 三层大小限制校验
系统 SHALL 在三层实施一致的文件大小限制校验：前端表单校验、Nginx/Gateway 配置、Spring 后端配置。三层值 MUST 保持一致。

#### Scenario: 前端校验拦截
- **WHEN** 用户在前端选择超过大小限制的文件
- **THEN** 前端在表单提交前拦截并给用户明确提示

#### Scenario: 后端校验拦截
- **WHEN** 文件绕过前端校验到达后端
- **THEN** Spring 层通过 `spring.servlet.multipart.max-file-size` 配置拦截，返回 `UPLOAD_SIZE_EXCEEDED`

### Requirement: 上传错误码规范
系统 SHALL 对上传失败返回结构化错误码，禁止笼统的"超时"包装。

#### Scenario: 存储后端写入失败
- **WHEN** UFOP 存储后端写入失败
- **THEN** 系统返回 HTTP 503 和错误码 `UPLOAD_STORAGE_ERROR`

#### Scenario: 文件格式不允许
- **WHEN** 上传的文件格式不在允许列表中
- **THEN** 系统返回 HTTP 415 和错误码 `UPLOAD_FORMAT_REJECTED`

#### Scenario: 同名文件冲突
- **WHEN** 同目录下已存在同名文件
- **THEN** 系统返回 HTTP 409 和错误码 `UPLOAD_DUPLICATE`

### Requirement: 上传配额预扣与释放
系统 SHALL 在上传开始时预扣存储空间，上传完成时确认实际大小，上传失败或取消时释放预扣空间。

#### Scenario: 预扣空间
- **WHEN** 上传开始（通过配额校验后）
- **THEN** 系统通过 Redis INCRBY 预扣用户已用空间

#### Scenario: 确认实际大小
- **WHEN** 上传成功完成
- **THEN** 系统确认实际文件大小，调整预扣值（预扣值与实际值之差）

#### Scenario: 释放预扣空间
- **WHEN** 上传失败或取消
- **THEN** 系统通过 Redis DECRBY 释放预扣的存储空间
