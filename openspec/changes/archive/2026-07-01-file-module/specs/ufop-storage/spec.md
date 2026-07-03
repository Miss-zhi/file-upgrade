## ADDED Requirements

### Requirement: UFOP 操作接口定义
系统 SHALL 定义 7 个统一的存储操作接口：Uploader、Downloader、Copier、Deleter、Previewer、Reader、Writer。所有存储后端 MUST 实现全部接口。

#### Scenario: 接口定义完整性
- **WHEN** 新增存储后端实现
- **THEN** 该实现 MUST 实现 Uploader（上传）、Downloader（下载）、Copier（复制）、Deleter（删除）、Previewer（预览）、Reader（读取）、Writer（写入）全部 7 个接口

#### Scenario: 操作前连通性校验
- **WHEN** 执行任何存储操作
- **THEN** 系统 MUST 在操作前快速校验存储后端连通性（快速失败）

#### Scenario: 操作后元数据更新
- **WHEN** 存储操作成功完成
- **THEN** 系统 MUST 更新文件元数据（FileBean 状态）

### Requirement: StorageFactory 工厂模式
系统 SHALL 通过 StorageFactory 根据配置获取当前激活的存储后端实现。配置项 `storage.type` 决定使用哪种后端。

#### Scenario: 获取存储后端实例
- **WHEN** 业务代码通过 StorageFactory 请求存储操作
- **THEN** 工厂根据 `storage.type` 配置返回对应的存储后端实现

#### Scenario: 配置切换存储后端
- **WHEN** 修改 `application.yml` 中的 `storage.type` 配置
- **THEN** 应用重启后使用新的存储后端

### Requirement: AutoConfiguration.imports 注册
每种存储后端 MUST 通过独立 Starter 模块实现，使用 Spring Boot 3 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册自动配置类。禁止使用已废弃的 `spring.factories` 方式。

#### Scenario: Local 存储后端注册
- **WHEN** 应用启动且 classpath 包含 local-storage starter
- **THEN** Spring Boot 通过 AutoConfiguration.imports 自动注册 LocalStorageAutoConfiguration

#### Scenario: MinIO 存储后端注册
- **WHEN** 应用启动且 classpath 包含 minio-storage starter
- **THEN** Spring Boot 通过 AutoConfiguration.imports 自动注册 MinioStorageAutoConfiguration

#### Scenario: AliyunOSS 存储后端注册
- **WHEN** 应用启动且 classpath 包含 aliyun-oss-storage starter
- **THEN** Spring Boot 通过 AutoConfiguration.imports 自动注册 AliyunOssStorageAutoConfiguration

#### Scenario: Qiniu 存储后端注册
- **WHEN** 应用启动且 classpath 包含 qiniu-storage starter
- **THEN** Spring Boot 通过 AutoConfiguration.imports 自动注册 QiniuStorageAutoConfiguration

#### Scenario: FastDFS 存储后端注册
- **WHEN** 应用启动且 classpath 包含 fastdfs-storage starter
- **THEN** Spring Boot 通过 AutoConfiguration.imports 自动注册 FastDfsStorageAutoConfiguration

### Requirement: 启动时连通性验证
系统 SHALL 在应用启动时对当前激活的存储后端执行连通性验证。

#### Scenario: 核心存储后端验证通过
- **WHEN** 应用启动，核心存储后端（`storage.type` 配置的后端）连通性验证通过
- **THEN** 系统正常启动，记录验证成功日志

#### Scenario: 核心存储后端验证失败
- **WHEN** 应用启动，核心存储后端连通性验证失败
- **THEN** 系统阻止应用启动，记录错误日志

#### Scenario: 验证流程
- **WHEN** 执行连通性验证
- **THEN** 系统依次执行：写入测试（1KB 小文件）→ 读取测试（读回并校验内容）→ 删除测试（清理测试文件）

### Requirement: 流式处理大文件
系统 SHALL 对所有存储操作使用流式处理，禁止将大文件全量加载到内存。

#### Scenario: 大文件上传流式处理
- **WHEN** 上传大文件到存储后端
- **THEN** 系统使用 InputStream 流式写入，不将整个文件加载到内存

#### Scenario: 大文件下载流式处理
- **WHEN** 从存储后端下载大文件
- **THEN** 系统使用 InputStream 流式读取，直接写入 HTTP 响应流

#### Scenario: 资源释放
- **WHEN** 存储操作完成或发生异常
- **THEN** 系统在 finally 块中释放所有资源（stream、connection）

### Requirement: Local 存储后端实现
系统 SHALL 提供基于本地文件系统的存储后端实现。

#### Scenario: 本地文件存储
- **WHEN** 使用 Local 存储后端上传文件
- **THEN** 文件存储到配置的本地目录（`storage.local.base-path`），按日期分目录组织

#### Scenario: 本地文件读取
- **WHEN** 使用 Local 存储后端下载文件
- **THEN** 从本地目录读取文件流并返回

### Requirement: MinIO 存储后端实现
系统 SHALL 提供基于 MinIO 的 S3 兼容存储后端实现。

#### Scenario: MinIO 文件操作
- **WHEN** 使用 MinIO 存储后端进行文件操作
- **THEN** 系统通过 MinIO Java SDK 执行 putObject/getObject/deleteObject 操作

### Requirement: AliyunOSS 存储后端实现
系统 SHALL 提供基于阿里云 OSS 的存储后端实现。

#### Scenario: AliyunOSS 文件操作
- **WHEN** 使用 AliyunOSS 存储后端进行文件操作
- **THEN** 系统通过阿里云 OSS SDK 执行文件上传/下载/删除操作

### Requirement: Qiniu 存储后端实现
系统 SHALL 提供基于七牛云 Kodo 的存储后端实现。

#### Scenario: Qiniu 文件操作
- **WHEN** 使用 Qiniu 存储后端进行文件操作
- **THEN** 系统通过七牛 SDK 执行文件上传/下载/删除操作

### Requirement: FastDFS 存储后端实现
系统 SHALL 提供基于 FastDFS 的存储后端实现。

#### Scenario: FastDFS 文件操作
- **WHEN** 使用 FastDFS 存储后端进行文件操作
- **THEN** 系统通过 FastDFS 客户端执行文件上传/下载/删除操作
