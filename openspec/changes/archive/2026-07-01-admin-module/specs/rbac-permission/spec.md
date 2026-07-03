## ADDED Requirements

### Requirement: 管理员启用/禁用用户端点
系统 SHALL 在 `/api/v1/admin/` 路径下提供启用和禁用用户的端点，受 `hasRole("ADMIN")` URL 级控制和 `@PreAuthorize("hasAuthority('admin:user-manage')")` 方法级控制。

| 端点 | 方法 | 权限注解 | 说明 |
|------|------|---------|------|
| `/api/v1/admin/users/{userId}/enable` | PUT | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 启用用户（设置 available=1），发布 PermissionChangedEvent |
| `/api/v1/admin/users/{userId}/disable` | PUT | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 禁用用户（设置 available=0），清除会话和权限缓存，发布 PermissionChangedEvent |

#### Scenario: 管理员禁用用户后缓存失效
- **WHEN** 管理员调用 `PUT /api/v1/admin/users/{userId}/disable`
- **THEN** 系统设置 user.available=0 → 清除 Redis 会话缓存 → 发布 PermissionChangedEvent（目标 userId）→ 返回 200

#### Scenario: 管理员启用用户后缓存失效
- **WHEN** 管理员调用 `PUT /api/v1/admin/users/{userId}/enable`
- **THEN** 系统设置 user.available=1 → 发布 PermissionChangedEvent（目标 userId）→ 返回 200

### Requirement: 禁用用户登录拒绝
UserDetailServiceImpl 加载用户时 MUST 检查 user.available 字段。available=0 的用户登录时 MUST 被拒绝。

#### Scenario: 禁用用户尝试登录
- **WHEN** available=0 的用户尝试登录
- **THEN** 系统返回 401，错误码 `USER_DISABLED`
