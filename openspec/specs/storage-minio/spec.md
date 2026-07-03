## ADDED Requirements

### Requirement: MinIO 后端完整实现
系统 SHALL 提供基于 MinIO Java SDK 8.5.x 的完整 StorageBackend 实现，覆盖全部 12 个接口方法。

#### Scenario: upload 上传文件
- **WHEN** 调用 `upload(inputStream, storagePath, fileSize)`
- **THEN** 系统通过 `MinioClient.putObject(PutObjectArgs)` 上传文件到配置的 bucket

#### Scenario: download 下载文件
- **WHEN** 调用 `download(storagePath)`
- **THEN** 系统通过 `MinioClient.getObject(GetObjectArgs)` 返回 InputStream

#### Scenario: downloadRange 范围下载
- **WHEN** 调用 `downloadRange(storagePath, start, end)`
- **THEN** 系统通过 `GetObjectArgs` 带 `offset` 和 `length` 参数获取指定范围数据

#### Scenario: getFileSize 获取文件大小
- **WHEN** 调用 `getFileSize(storagePath)`
- **THEN** 系统通过 `MinioClient.statObject(StatObjectArgs).size()` 返回文件大小

#### Scenario: copy 复制文件
- **WHEN** 调用 `copy(sourcePath, destinationPath)`
- **THEN** 系统通过 `MinioClient.copyObject(CopyObjectArgs)` 在同 bucket 内复制文件

#### Scenario: delete 删除文件
- **WHEN** 调用 `delete(storagePath)`
- **THEN** 系统通过 `MinioClient.removeObject(RemoveObjectArgs)` 删除文件

#### Scenario: getPreviewUrl 获取预览 URL
- **WHEN** 调用 `getPreviewUrl(storagePath)`
- **THEN** 系统通过 `MinioClient.getPresignedObjectUrl()` 生成预签名 URL，默认 1 小时过期，过期时间可配置

#### Scenario: exists 判断文件存在
- **WHEN** 调用 `exists(storagePath)`
- **THEN** 系统通过 `MinioClient.statObject()` 判断文件是否存在，catch `ErrorResponseException` 判断 404

#### Scenario: checkConnectivity 连通性检查
- **WHEN** 调用 `checkConnectivity()`
- **THEN** 系统通过 `MinioClient.bucketExists()` 检查 bucket 是否可访问

#### Scenario: IO 异常统一处理
- **WHEN** 任何操作发生 IO 异常
- **THEN** 系统 MUST 统一抛 `UncheckedIOException`，保留原始异常信息

### Requirement: MinIO AutoConfiguration
系统 SHALL 提供 `MinioStorageAutoConfiguration`，当 `storage.type=minio` 时激活。

#### Scenario: 条件激活
- **WHEN** `storage.type` 配置为 `minio`
- **THEN** `MinioStorageAutoConfiguration` 被激活，注册 `MinioClient` 和 `MinioStorageBackend` Bean

#### Scenario: MinioClient 单例
- **WHEN** AutoConfiguration 激活
- **THEN** `MinioClient` MUST 作为单例 `@Bean` 注册，禁止每次操作 new builder

#### Scenario: bucket 自动创建
- **WHEN** 应用启动且 bucket 不存在
- **THEN** AutoConfiguration MUST 在启动时检查并自动创建 bucket（`checkAndCreateBucket()`）

#### Scenario: 配置读取
- **WHEN** 创建 MinioClient
- **THEN** endpoint / accessKey / secretKey / bucket MUST 从 `StorageProperties.Minio` 读取，禁止硬编码

#### Scenario: AutoConfiguration.imports 注册
- **WHEN** 应用启动
- **THEN** `MinioStorageAutoConfiguration` MUST 在 `AutoConfiguration.imports` 中注册

### Requirement: MinIO 单元测试
系统 SHALL 为 MinIO 后端提供单元测试覆盖。

#### Scenario: Mock MinioClient 测试
- **WHEN** 运行单元测试
- **THEN** 使用 Mockito Mock `MinioClient`，验证各操作正确调用 SDK 方法并传递正确参数

#### Scenario: 集成测试（可选）
- **WHEN** 运行标记为 `integration` 的测试
- **THEN** 使用 Testcontainers MinIO 容器验证端到端功能
