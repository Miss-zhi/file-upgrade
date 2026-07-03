## ADDED Requirements

### Requirement: FastDFS 后端完整实现
系统 SHALL 提供基于 `fastdfs-client`（tobato）1.27.x 的完整 StorageBackend 实现，覆盖全部 12 个接口方法。

#### Scenario: upload 上传文件
- **WHEN** 调用 `upload(inputStream, storagePath, fileSize)`
- **THEN** 系统通过 `FastFileStorageClient.uploadFile()` 上传文件，返回 `StorePath`，storagePath 格式为 `group/path`

#### Scenario: download 下载文件
- **WHEN** 调用 `download(storagePath)`
- **THEN** 系统解析 storagePath 中的 group 和 path，通过 `FastFileStorageClient.downloadFile()` 下载文件，返回 ByteArrayInputStream

#### Scenario: read 读取文件
- **WHEN** 调用 `read(storagePath)`
- **THEN** 系统委托 `download(storagePath)` 实现，返回相同的 InputStream

#### Scenario: downloadRange 范围下载（降级实现）
- **WHEN** 调用 `downloadRange(storagePath, start, end)`
- **THEN** 系统下载全量文件后截取指定范围
- **THEN** 文件 >50MB 时 MUST 记录 WARN 日志，提示调用方避免 range 请求

#### Scenario: getFileSize 获取文件大小
- **WHEN** 调用 `getFileSize(storagePath)`
- **THEN** 系统通过 `FastFileStorageClient.getFileInfo()` 获取文件大小

#### Scenario: copy 复制文件（降级实现）
- **WHEN** 调用 `copy(sourcePath, destinationPath)`
- **THEN** 系统下载源文件后重新上传到目标路径（FastDFS 无服务端 copy）

#### Scenario: delete 删除文件
- **WHEN** 调用 `delete(storagePath)`
- **THEN** 系统通过 `FastFileStorageClient.deleteFile(group, path)` 删除文件

#### Scenario: getPreviewUrl 返回 null
- **WHEN** 调用 `getPreviewUrl(storagePath)`
- **THEN** 系统返回 null（FastDFS 无内置 HTTP 预览，需配合 Nginx）

#### Scenario: exists 判断文件存在
- **WHEN** 调用 `exists(storagePath)`
- **THEN** 系统通过 `FastFileStorageClient.getFileInfo()` 判断文件是否存在，catch `FdfsServerException` 判断不存在

#### Scenario: checkConnectivity 连通性检查
- **WHEN** 调用 `checkConnectivity()`
- **THEN** 系统通过 Tracker 连接测试验证 FastDFS 可访问

#### Scenario: storagePath 格式解析
- **WHEN** 传入 storagePath（如 `group1/M00/00/01/xxx.jpg`）
- **THEN** 系统 MUST 正确拆分 group（`group1`）和 path（`M00/00/01/xxx.jpg`）

#### Scenario: IO 异常统一处理
- **WHEN** 任何操作发生 IO 异常
- **THEN** 系统 MUST 统一抛 `UncheckedIOException`，保留原始异常信息

### Requirement: FastDFS AutoConfiguration
系统 SHALL 提供 `FastDfsStorageAutoConfiguration`，当 `storage.type=fastdfs` 时激活。

#### Scenario: 条件激活
- **WHEN** `storage.type` 配置为 `fastdfs`
- **THEN** `FastDfsStorageAutoConfiguration` 被激活，引入 tobato `FdfsClientConfig`，注册 `FastDfsStorageBackend` Bean

#### Scenario: Tracker 连接池配置
- **WHEN** AutoConfiguration 激活
- **THEN** 引入 tobato `FdfsClientConfig`，tracker server 地址从 `StorageProperties.Fastdfs.trackerServers` 读取（支持多节点逗号分隔）

#### Scenario: group 名称可配置
- **WHEN** 上传文件
- **THEN** group 名称 MUST 可配置（默认 `group1`），禁止硬编码

#### Scenario: AutoConfiguration.imports 注册
- **WHEN** 应用启动
- **THEN** `FastDfsStorageAutoConfiguration` MUST 在 `AutoConfiguration.imports` 中注册

### Requirement: FastDFS 单元测试
系统 SHALL 为 FastDFS 后端提供单元测试覆盖。

#### Scenario: Mock FastFileStorageClient 测试
- **WHEN** 运行单元测试
- **THEN** 使用 Mockito Mock `FastFileStorageClient`，验证各操作正确调用 SDK 方法并传递正确参数

#### Scenario: storagePath 解析测试
- **WHEN** 传入 `group1/M00/00/01/xxx.jpg` 格式路径
- **THEN** 系统正确拆分 group 和 path 并传递给 FastDFS 客户端
