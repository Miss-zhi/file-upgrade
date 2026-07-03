## ADDED Requirements

### Requirement: 管理员查询用户配额
系统 SHALL 提供管理员查看指定用户存储配额使用情况的端点，返回总配额、已用空间、可用空间。

| 端点 | 方法 | 权限注解 | 说明 |
|------|------|---------|------|
| `/api/v1/admin/quota/{userId}` | GET | `@PreAuthorize("hasAuthority('admin:quota-manage')")` | 查询用户配额（调用 StorageQuotaService.getQuotaInfo） |

#### Scenario: 查询用户配额成功
- **WHEN** 管理员请求 `GET /api/v1/admin/quota/{userId}`
- **THEN** 系统调用 StorageQuotaService.getQuotaInfo 返回 totalQuota、usedQuota、availableQuota

#### Scenario: 用户无存储记录
- **WHEN** 管理员查询未初始化存储配额的用户
- **THEN** 系统返回默认配额（10GB）和已用 0

### Requirement: 管理员设置用户配额
系统 SHALL 提供管理员为指定用户设置存储配额的端点。更新 DB 后同步刷新 Redis 缓存。

#### Scenario: 设置用户配额成功
- **WHEN** 管理员请求 `PUT /api/v1/admin/quota/{userId}`，请求体包含 totalQuota 值
- **THEN** 系统调用 StorageQuotaService.setQuota 更新配额，同步 Redis 缓存，返回 200

#### Scenario: 配额值非法
- **WHEN** 管理员请求设置 totalQuota ≤ 0
- **THEN** 系统返回 400，错误码 `INVALID_QUOTA`

### Requirement: 管理员批量设置用户配额
系统 SHALL 提供批量设置多个用户配额的端点。在同一事务中完成所有更新。

#### Scenario: 批量设置配额成功
- **WHEN** 管理员请求 `PUT /api/v1/admin/quota/batch`，请求体包含多个 userId 和对应的 totalQuota
- **THEN** 系统在同一事务中调用 StorageQuotaService.setQuota 为每个用户更新配额，返回 200

#### Scenario: 批量设置部分用户不存在
- **WHEN** 批量请求中包含不存在的 userId
- **THEN** 系统跳过不存在的用户，仅更新有效用户的配额，返回 200 并在响应中列出跳过的 userId

### Requirement: 配额管理端点权限控制
所有配额管理端点 MUST 受 `@PreAuthorize("hasAuthority('admin:quota-manage')")` 注解保护。

#### Scenario: 有权限的管理员可访问
- **WHEN** 拥有 `admin:quota-manage` 权限的管理员请求配额管理端点
- **THEN** 请求正常处理

#### Scenario: 无权限被拒绝
- **WHEN** 不拥有 `admin:quota-manage` 权限的用户请求配额管理端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`
