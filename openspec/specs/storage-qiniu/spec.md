## ADDED Requirements

### Requirement: Qiniu 后端完整实现
系统 SHALL 提供基于 `qiniu-java-sdk` 7.15.x 的完整 StorageBackend 实现，覆盖全部 12 个接口方法。

#### Scenario: upload 上传文件
- **WHEN** 调用 `upload(inputStream, storagePath, fileSize)`
- **THEN** 系统通过 `UploadManager.put(inputStream, key, getUpToken(), null, null)` 上传文件

#### Scenario: download 下载文件
- **WHEN** 调用 `download(storagePath)`
- **THEN** 系统构建签名 URL，通过 `new URL(signedUrl).openStream()` 返回 InputStream

#### Scenario: downloadRange 范围下载
- **WHEN** 调用 `downloadRange(storagePath, start, end)`
- **THEN** 系统通过 `Auth.privateDownloadUrl()` 生成签名 URL，再用 `HttpURLConnection` 对签名 URL 发起 GET 请求并设置 `Range: bytes=start-end` header 获取指定范围数据

#### Scenario: getFileSize 获取文件大小
- **WHEN** 调用 `getFileSize(storagePath)`
- **THEN** 系统通过 `BucketManager.stat(bucket, key).getFsize()` 返回文件大小

#### Scenario: copy 复制文件
- **WHEN** 调用 `copy(sourcePath, destinationPath)`
- **THEN** 系统通过 `BucketManager.copy(bucket, srcKey, bucket, destKey, true)` 复制文件

#### Scenario: delete 删除文件
- **WHEN** 调用 `delete(storagePath)`
- **THEN** 系统通过 `BucketManager.delete(bucket, key)` 删除文件

#### Scenario: getPreviewUrl 获取预览 URL
- **WHEN** 调用 `getPreviewUrl(storagePath)`
- **THEN** 系统通过 `Auth.privateDownloadUrl(domain + "/" + key, expireSeconds)` 生成私有下载 URL，过期时间可配置（默认 1 小时）

#### Scenario: exists 判断文件存在
- **WHEN** 调用 `exists(storagePath)`
- **THEN** 系统通过 `BucketManager.stat()` 判断文件是否存在，catch `QiniuException` 判断错误码 612

#### Scenario: checkConnectivity 连通性检查
- **WHEN** 调用 `checkConnectivity()`
- **THEN** 系统通过 `BucketManager.listBucket(bucket, "", 1)` 检查 bucket 是否可访问

#### Scenario: IO 异常统一处理
- **WHEN** 任何操作发生 IO 异常
- **THEN** 系统 MUST 统一抛 `UncheckedIOException`，保留原始异常信息

### Requirement: Qiniu AutoConfiguration
系统 SHALL 提供 `QiniuStorageAutoConfiguration`，当 `storage.type=qiniu` 时激活。

#### Scenario: 条件激活
- **WHEN** `storage.type` 配置为 `qiniu`
- **THEN** `QiniuStorageAutoConfiguration` 被激活，注册 `Auth`、`UploadManager`、`BucketManager` 和 `QiniuStorageBackend` Bean

#### Scenario: SDK 客户端单例
- **WHEN** AutoConfiguration 激活
- **THEN** `Auth` / `UploadManager` / `BucketManager` MUST 作为单例 `@Bean` 注册，禁止每次操作 new 实例

#### Scenario: 配置读取
- **WHEN** 创建 SDK 客户端
- **THEN** accessKey / secretKey / bucket / domain MUST 从 `StorageProperties.Qiniu` 读取，禁止硬编码

#### Scenario: 上传 token 可配置
- **WHEN** 生成上传 token
- **THEN** token 过期时间 MUST 可配置（默认 1 小时）

#### Scenario: AutoConfiguration.imports 注册
- **WHEN** 应用启动
- **THEN** `QiniuStorageAutoConfiguration` MUST 在 `AutoConfiguration.imports` 中注册

### Requirement: Qiniu 单元测试
系统 SHALL 为 Qiniu 后端提供单元测试覆盖。

#### Scenario: Mock SDK 客户端测试
- **WHEN** 运行单元测试
- **THEN** 使用 Mockito Mock `Auth` / `UploadManager` / `BucketManager`，验证各操作正确调用 SDK 方法并传递正确参数
