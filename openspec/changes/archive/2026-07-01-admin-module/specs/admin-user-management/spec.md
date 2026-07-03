## ADDED Requirements

### Requirement: 管理员用户列表查询
系统 SHALL 提供分页查询用户列表的管理员端点。支持按用户名模糊搜索、按可用状态过滤。返回用户基本信息（userId、username、telephone、available、registerTime）及关联角色名称列表。

#### Scenario: 分页查询所有用户
- **WHEN** 管理员请求 `GET /api/v1/admin/users?page=1&pageSize=20`
- **THEN** 系统返回分页用户列表，包含 userId、username、telephone、available、registerTime 和角色名称列表

#### Scenario: 按用户名搜索
- **WHEN** 管理员请求 `GET /api/v1/admin/users?keyword=admin&page=1&pageSize=20`
- **THEN** 系统返回用户名包含 "admin" 的分页结果

#### Scenario: 按状态过滤
- **WHEN** 管理员请求 `GET /api/v1/admin/users?available=0&page=1&pageSize=20`
- **THEN** 系统仅返回 available=0（已禁用）的用户列表

### Requirement: 管理员查询用户详情
系统 SHALL 提供查询用户详情的管理员端点，返回用户完整信息及关联的角色和权限列表。

#### Scenario: 查询用户详情成功
- **WHEN** 管理员请求 `GET /api/v1/admin/users/{userId}`
- **THEN** 系统返回用户详情，包含 userId、username、telephone、available、registerTime、角色列表（含 roleId、roleName）和权限列表（含 permKey）

#### Scenario: 用户不存在
- **WHEN** 管理员请求查询不存在的 userId
- **THEN** 系统返回 404，错误码 `USER_NOT_FOUND`

### Requirement: 管理员启用用户
系统 SHALL 提供启用用户的端点。将用户 available 字段设为 1，并发布 PermissionChangedEvent 刷新缓存。

#### Scenario: 启用已禁用用户
- **WHEN** 管理员请求 `PUT /api/v1/admin/users/{userId}/enable`，目标用户 available=0
- **THEN** 系统将用户 available 设为 1，发布 PermissionChangedEvent，返回 200

#### Scenario: 启用已启用的用户
- **WHEN** 管理员请求启用 available=1 的用户
- **THEN** 系统返回 200（幂等操作，无副作用）

### Requirement: 管理员禁用用户
系统 SHALL 提供禁用用户的端点。将用户 available 字段设为 0，清除该用户的 Redis 会话和权限缓存，发布 PermissionChangedEvent。

#### Scenario: 禁用启用中的用户
- **WHEN** 管理员请求 `PUT /api/v1/admin/users/{userId}/disable`，目标用户 available=1
- **THEN** 系统将用户 available 设为 0，清除 Redis 会话缓存和权限缓存，发布 PermissionChangedEvent，返回 200

#### Scenario: 禁用已禁用的用户
- **WHEN** 管理员请求禁用 available=0 的用户
- **THEN** 系统返回 200（幂等操作，无副作用）

#### Scenario: 管理员不能禁用自己
- **WHEN** 管理员请求禁用当前登录的管理员自身账号
- **THEN** 系统返回 400，错误码 `CANNOT_DISABLE_SELF`

### Requirement: 管理员重置用户密码
系统 SHALL 提供管理员重置用户密码的端点。新密码由系统生成随机密码或管理员指定，使用 BCrypt 加密存储。

#### Scenario: 管理员重置用户密码
- **WHEN** 管理员请求 `PUT /api/v1/admin/users/{userId}/password`，请求体包含新密码
- **THEN** 系统使用 BCrypt 加密新密码并更新 user 表，返回 200

#### Scenario: 重置密码时用户不存在
- **WHEN** 管理员请求重置不存在的 userId 的密码
- **THEN** 系统返回 404，错误码 `USER_NOT_FOUND`

### Requirement: 管理员端点权限控制
所有管理员用户管理端点 MUST 受 `@PreAuthorize("hasAuthority('admin:user-manage')")` 注解保护。

#### Scenario: 有权限的管理员可访问
- **WHEN** 拥有 ROLE_ADMIN 和 `admin:user-manage` 权限的用户请求用户管理端点
- **THEN** 请求正常处理

#### Scenario: 无细粒度权限被拒绝
- **WHEN** 拥有 ROLE_ADMIN 但不拥有 `admin:user-manage` 权限的用户请求用户管理端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`
