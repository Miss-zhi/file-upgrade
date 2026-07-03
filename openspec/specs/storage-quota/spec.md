## ADDED Requirements

### Requirement: 配额查询
系统 SHALL 支持查询用户当前存储使用情况，包含总配额、已用空间、可用空间。

#### Scenario: 查询用户存储配额
- **WHEN** 认证用户请求查询存储配额
- **THEN** 系统返回用户的 totalQuota（总配额）、usedQuota（已用空间）、availableQuota（可用空间）
- **THEN** 已用空间从 Redis 缓存读取，确保数据实时性

### Requirement: 上传前配额校验
系统 SHALL 在每次上传前校验用户配额是否充足。配额不足时 MUST 拒绝上传。

#### Scenario: 配额充足允许上传
- **WHEN** 用户上传文件，用户可用空间 ≥ 文件大小
- **THEN** 系统允许上传，继续后续上传流程

#### Scenario: 配额不足拒绝上传
- **WHEN** 用户上传文件，用户可用空间 < 文件大小
- **THEN** 系统拒绝上传，返回 HTTP 507 和错误码 `UPLOAD_QUOTA_EXCEEDED`

### Requirement: 配额预扣机制
系统 SHALL 在上传开始时预扣存储空间，确保并发上传不会超额使用配额。预扣操作 MUST 使用 Redis 原子操作。

#### Scenario: 预扣空间成功
- **WHEN** 上传通过配额校验后开始
- **THEN** 系统通过 Redis INCRBY 原子递增用户已用空间
- **THEN** 记录预扣量，用于后续确认或释放

#### Scenario: 预扣后确认实际大小
- **WHEN** 上传成功完成
- **THEN** 系统比较预扣值和实际文件大小
- **THEN** 若实际大小 < 预扣值，通过 Redis DECRBY 释放差额
- **THEN** 若实际大小 > 预扣值（理论上不应发生），记录告警日志

#### Scenario: 上传失败释放预扣
- **WHEN** 上传失败或取消
- **THEN** 系统通过 Redis DECRBY 原子释放预扣的存储空间

### Requirement: 配额同步与恢复
系统 SHALL 定期将 Redis 中的配额数据同步到数据库，并在 Redis 故障时从数据库恢复。

#### Scenario: 定时同步到数据库
- **WHEN** 定时任务执行（每小时）
- **THEN** 系统计算每个用户的实际已用空间（从 UserFile 汇总），更新数据库中的配额记录
- **THEN** 同步 Redis 缓存值与数据库值

#### Scenario: Redis 故障恢复
- **WHEN** Redis 中用户配额数据丢失或不可用
- **THEN** 系统从数据库中重新计算并恢复 Redis 缓存值

### Requirement: 管理员配额管理
系统 SHALL 支持管理员为用户设置和调整存储配额。

#### Scenario: 管理员设置用户配额
- **WHEN** 管理员请求设置用户配额（提供 userId 和 quota 值）
- **THEN** 系统更新用户的 totalQuota，同步更新 Redis 缓存

#### Scenario: 批量设置用户配额
- **WHEN** 管理员请求批量设置多个用户的配额
- **THEN** 系统在同一事务中更新所有用户的配额

### Requirement: 配额超限通知
系统 SHALL 在用户存储使用率达到阈值时发出提醒。

#### Scenario: 使用率达到 80% 提醒
- **WHEN** 用户上传文件后，存储使用率达到 80%
- **THEN** 系统在上传响应中包含配额警告标识

#### Scenario: 使用率达到 100% 阻止上传
- **WHEN** 用户存储使用率达到 100%
- **THEN** 系统拒绝后续上传，返回 `UPLOAD_QUOTA_EXCEEDED`

### Requirement: 管理员配额管理端点从 admin 模块暴露
系统 SHALL 在 admin 模块 `/api/v1/admin/quota` 路径下暴露管理员配额管理端点，内部调用 file-module 的 StorageQuotaService 实现配额操作。

| 端点 | 方法 | 权限注解 | 说明 |
|------|------|---------|------|
| `/api/v1/admin/quota/{userId}` | GET | `@PreAuthorize("hasAuthority('admin:quota-manage')")` | 查询用户配额（调用 StorageQuotaService.getQuotaInfo） |
| `/api/v1/admin/quota/{userId}` | PUT | `@PreAuthorize("hasAuthority('admin:quota-manage')")` | 设置用户配额（调用 StorageQuotaService.setQuota） |
| `/api/v1/admin/quota/batch` | PUT | `@PreAuthorize("hasAuthority('admin:quota-manage')")` | 批量设置用户配额 |

#### Scenario: 管理员查询配额复用 StorageQuotaService
- **WHEN** 管理员请求 `GET /api/v1/admin/quota/{userId}`
- **THEN** admin 模块调用 StorageQuotaService.getQuotaInfo(userId)，返回配额信息

#### Scenario: 管理员设置配额复用 StorageQuotaService
- **WHEN** 管理员请求 `PUT /api/v1/admin/quota/{userId}`
- **THEN** admin 模块调用 StorageQuotaService.setQuota(userId, totalQuota)，更新 DB 和 Redis
