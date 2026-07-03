## ADDED Requirements

### Requirement: StorageFactory 按文件存储类型路由
`StorageFactory` SHALL 提供 `getBackendForStorageType(String storageType)` 方法，支持按文件级 `storageType` 字段路由到对应后端。

#### Scenario: 文件路由到对应后端
- **WHEN** 调用 `getBackendForStorageType("minio")`，且 minio 后端已注册
- **THEN** 系统返回 `MinioStorageBackend` 实例

#### Scenario: 文件路由时后端未注册 fallback
- **WHEN** 调用 `getBackendForStorageType("minio")`，但 minio 后端未注册
- **THEN** 系统 MUST 返回全局活跃后端（`storage.type` 对应的后端）
- **THEN** 系统 MUST 记录 `log.warn` 日志，说明 fallback 原因

#### Scenario: storageType 参数为 null
- **WHEN** 调用 `getBackendForStorageType(null)`
- **THEN** 系统返回全局活跃后端

### Requirement: StorageFactory 单元测试
系统 SHALL 为 StorageFactory 提供完整的单元测试覆盖。

#### Scenario: 获取全局活跃后端
- **WHEN** 调用 `getBackend()` 且 `storage.type=local`
- **THEN** 返回 `LocalStorageBackend` 实例

#### Scenario: 按类型获取后端
- **WHEN** 调用 `getBackend("minio")` 且 minio 后端已注册
- **THEN** 返回 `MinioStorageBackend` 实例

#### Scenario: 未知类型抛异常
- **WHEN** 调用 `getBackend("unknown")` 且 unknown 后端未注册
- **THEN** 抛出 `IllegalStateException`，消息包含未知类型和已注册后端列表

#### Scenario: 按文件路由
- **WHEN** 调用 `getBackendForStorageType("minio")` 且 minio 后端已注册
- **THEN** 返回对应后端实例

#### Scenario: 按文件路由 fallback
- **WHEN** 调用 `getBackendForStorageType("minio")` 且 minio 后端未注册
- **THEN** 返回全局活跃后端，验证 warn 日志被记录

### Requirement: StorageHealthChecker 单元测试
系统 SHALL 为 StorageHealthChecker 提供完整的单元测试覆盖。

#### Scenario: 连通性验证成功
- **WHEN** 后端 upload → download → delete 全部成功且内容校验通过
- **THEN** 系统记录成功日志，应用正常启动

#### Scenario: 连通性验证失败
- **WHEN** 后端 upload 操作抛出异常
- **THEN** 系统 MUST 抛出 RuntimeException 阻止应用启动，记录错误日志

#### Scenario: 内容校验失败
- **WHEN** download 读回内容与写入内容不一致
- **THEN** 系统 MUST 抛出 RuntimeException，消息包含"读取内容校验失败"

### Requirement: StorageProperties 配置绑定测试
系统 SHALL 为 StorageProperties 提供配置绑定测试。

#### Scenario: 默认值验证
- **WHEN** 未配置任何 storage 属性
- **THEN** `type` 默认值为 `local`，`local.basePath` 默认值为 `/data/qiwenshare/files`

#### Scenario: yml 绑定验证
- **WHEN** 在 application.yml 中配置 `storage.minio.endpoint=http://minio:9000`
- **THEN** `StorageProperties.getMinio().getEndpoint()` 返回 `http://minio:9000`

### Requirement: application.yml 配置示例
系统 SHALL 在 application.yml 中提供所有后端的完整配置示例（含注释说明）。

#### Scenario: 配置段完整性
- **WHEN** 查看 application.yml 或 application-prod.yml
- **THEN** 包含 local / minio / aliyun / qiniu / fastdfs 全部后端的配置段（默认注释，仅激活 local）
