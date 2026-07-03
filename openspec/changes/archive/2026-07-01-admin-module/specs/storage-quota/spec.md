## ADDED Requirements

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
