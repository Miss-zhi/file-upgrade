## ADDED Requirements

### Requirement: Aliyun OSS 后端完整实现
系统 SHALL 提供基于 `aliyun-sdk-oss` 3.17.x 的完整 StorageBackend 实现，覆盖全部 12 个接口方法。

#### Scenario: upload 上传文件
- **WHEN** 调用 `upload(inputStream, storagePath, fileSize)`
- **THEN** 系统通过 `OSS.putObject(bucket, key, inputStream, metadata)` 上传文件

#### Scenario: download 下载文件
- **WHEN** 调用 `download(storagePath)`
- **THEN** 系统通过 `OSS.getObject(bucket, key).getObjectContent()` 返回 InputStream

#### Scenario: downloadRange 范围下载
- **WHEN** 调用 `downloadRange(storagePath, start, end)`
- **THEN** 系统通过 `GetObjectRequest.setRange(start, end)` 获取指定范围数据

#### Scenario: getFileSize 获取文件大小
- **WHEN** 调用 `getFileSize(storagePath)`
- **THEN** 系统通过 `OSS.getObjectMetadata(bucket, key).getContentLength()` 返回文件大小

#### Scenario: copy 复制文件
- **WHEN** 调用 `copy(sourcePath, destinationPath)`
- **THEN** 系统通过 `OSS.copyObject(bucket, srcKey, bucket, destKey)` 复制文件

#### Scenario: delete 删除文件
- **WHEN** 调用 `delete(storagePath)`
- **THEN** 系统通过 `OSS.deleteObject(bucket, key)` 删除文件

#### Scenario: getPreviewUrl 获取预览 URL
- **WHEN** 调用 `getPreviewUrl(storagePath)`
- **THEN** 系统通过 `OSS.generatePresignedUrl(bucket, key, expiration)` 生成预签名 URL

#### Scenario: exists 判断文件存在
- **WHEN** 调用 `exists(storagePath)`
- **THEN** 系统通过 `OSS.doesObjectExist(bucket, key)` 判断文件是否存在

#### Scenario: checkConnectivity 连通性检查
- **WHEN** 调用 `checkConnectivity()`
- **THEN** 系统通过 `OSS.doesBucketExist(bucket)` 检查 bucket 是否可访问

#### Scenario: IO 异常统一处理
- **WHEN** 任何操作发生 IO 异常
- **THEN** 系统 MUST 统一抛 `UncheckedIOException`，保留原始异常信息

### Requirement: Aliyun OSS 类型隔离
Aliyun OSS SDK 类型 SHALL NOT 泄露到 StorageBackend 接口层。

#### Scenario: 接口层无 Aliyun 依赖
- **WHEN** 查看 `StorageBackend` 接口或其子接口
- **THEN** 不得出现 `com.aliyun.oss` 包的 import（旧项目反模式禁止）

### Requirement: Aliyun OSS AutoConfiguration
系统 SHALL 提供 `AliyunOssStorageAutoConfiguration`，当 `storage.type=aliyun` 时激活。

#### Scenario: 条件激活
- **WHEN** `storage.type` 配置为 `aliyun`
- **THEN** `AliyunOssStorageAutoConfiguration` 被激活，注册 `OSS` 客户端和 `AliyunOssStorageBackend` Bean

#### Scenario: OSS 客户端生命周期管理
- **WHEN** 应用关闭
- **THEN** `OSS` 客户端 MUST 调用 `shutdown()` 释放连接池，通过 `@Bean(destroyMethod = "shutdown")` 或 `DisposableBean` 实现

#### Scenario: 配置读取
- **WHEN** 创建 OSS 客户端
- **THEN** endpoint / accessKeyId / accessKeySecret / bucket MUST 从 `StorageProperties.Aliyun` 读取，禁止硬编码

#### Scenario: AutoConfiguration.imports 注册
- **WHEN** 应用启动
- **THEN** `AliyunOssStorageAutoConfiguration` MUST 在 `AutoConfiguration.imports` 中注册

### Requirement: Aliyun OSS 单元测试
系统 SHALL 为 Aliyun OSS 后端提供单元测试覆盖。

#### Scenario: Mock OSS 客户端测试
- **WHEN** 运行单元测试
- **THEN** 使用 Mockito Mock `OSS` 客户端，验证各操作正确调用 SDK 方法并传递正确参数
