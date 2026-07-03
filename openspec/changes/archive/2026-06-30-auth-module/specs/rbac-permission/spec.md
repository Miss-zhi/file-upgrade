## ADDED Requirements

### Requirement: RBAC 五表模型
系统 SHALL 维护 User → UserRole → Role → RolePermission → Permission 五张表的 RBAC 权限模型。User 通过 UserRole 关联 Role，Role 通过 RolePermission 关联 Permission。UserRole 和 RolePermission 使用联合主键。

#### Scenario: 用户通过角色获取权限
- **WHEN** 用户绑定了角色 A，角色 A 关联了权限 P1 和 P2
- **THEN** 该用户的权限列表包含 P1 和 P2

#### Scenario: 多角色权限合并
- **WHEN** 用户同时绑定角色 A（权限 P1）和角色 B（权限 P2、P3）
- **THEN** 该用户的权限列表为 P1、P2、P3 的并集

### Requirement: 权限编码规范
所有权限编码 MUST 遵循 `resource:action` 格式（如 `file:upload`、`admin:user-manage`）。Permission 表的 `perm_key` 字段存储权限编码。

#### Scenario: 权限编码格式正确
- **WHEN** 系统中创建新权限
- **THEN** perm_key 值 MUST 符合 `resource:action` 格式

### Requirement: 角色可用性检查
Role 表的 `available` 字段 MUST 在认证时检查。`available = 0` 的角色不参与权限计算，其关联的权限不计入用户的权限列表。

#### Scenario: 禁用角色的权限不计入
- **WHEN** 用户绑定了角色 A（available=1）和角色 B（available=0）
- **THEN** 仅角色 A 关联的权限计入用户权限列表

### Requirement: 权限缓存与 Redis 失效
系统 SHALL 将用户权限缓存到 Redis（key: `user:perms:{userId}`，value: 权限列表 JSON，TTL: 5 分钟）。缓存 miss 时查数据库并回填缓存。权限或角色变更时 MUST 通过 Spring ApplicationEvent 驱动缓存失效。

#### Scenario: 权限从缓存加载
- **WHEN** 用户发起请求且 Redis 中存在 `user:perms:{userId}` 缓存
- **THEN** 系统从 Redis 读取权限列表，不查询数据库

#### Scenario: 缓存 miss 时从数据库加载
- **WHEN** 用户发起请求且 Redis 中无对应缓存
- **THEN** 系统查询 user → user_role → role → role_permission → permission 获取权限列表，写入 Redis（TTL 5 分钟）

#### Scenario: 权限变更后缓存立即失效
- **WHEN** 管理员修改了用户的角色或权限
- **THEN** 系统发布 PermissionChangedEvent，监听器删除 `user:perms:{userId}` 缓存，下次请求重新加载

### Requirement: 方法级权限注解
系统 SHALL 使用 `@PreAuthorize` 注解实现方法级权限控制。注解中引用 `resource:action` 格式的权限编码。需配合 `@EnableMethodSecurity` 启用。

#### Scenario: 有权限的用户可访问受保护方法
- **WHEN** 用户拥有 `admin:user-manage` 权限，调用 `@PreAuthorize("hasAuthority('admin:user-manage')")` 标注的方法
- **THEN** 方法正常执行

#### Scenario: 无权限的用户被拒绝
- **WHEN** 用户不拥有 `admin:user-manage` 权限，调用该方法
- **THEN** 返回 403，错误码 `ACCESS_DENIED`

### Requirement: URL 级权限控制
SecurityFilterChain MUST 配置 URL 级权限规则：`/api/v1/admin/**` 限制 `hasRole("ADMIN")`，其余认证请求 `authenticated()`。公开端点（login、register、refresh）配置为 `permitAll()`。

#### Scenario: 非管理员访问 admin 端点被拒绝
- **WHEN** 普通用户（无 ROLE_ADMIN）访问 `/api/v1/admin/users/123/password`
- **THEN** 返回 403

#### Scenario: 管理员可访问 admin 端点
- **WHEN** 拥有 ROLE_ADMIN 的用户访问 `/api/v1/admin/**` 端点
- **THEN** 请求通过 URL 级权限检查，继续到 Controller 处理

### Requirement: 权限继承
Permission 表的 `parent_id` 字段支持层级继承，最大深度 2 级（父权限 → 子权限，禁止更深层级）。

**继承方向**：拥有父权限的用户自动获得其所有子权限。反向不成立——拥有子权限不意味着拥有父权限。

**查询策略**：`UserDetailServiceImpl.loadUserByUsername` 中，先查询角色直接关联的 perm_key 列表，再对 `parent_id = 0` 的每个权限执行一次子查询，将子权限合并到结果集中。SQL 参考：
```sql
SELECT p.perm_key FROM role_permission rp
JOIN permission p ON rp.permission_id = p.permission_id
WHERE rp.role_id IN (
  SELECT ur.role_id FROM user_role ur
  JOIN role r ON ur.role_id = r.role_id
  WHERE ur.user_id = ? AND r.available = 1
)
UNION
SELECT c.perm_key FROM permission c
JOIN permission p ON c.parent_id = p.permission_id
JOIN role_permission rp ON rp.permission_id = p.permission_id
WHERE rp.role_id IN (
  SELECT ur.role_id FROM user_role ur
  JOIN role r ON ur.role_id = r.role_id
  WHERE ur.user_id = ? AND r.available = 1
)
```

**数据完整性**：`parent_id = 0` 表示顶级权限（无父节点），非 0 值 MUST 引用已存在的 permission_id。插入/更新 Permission 时 Service 层 MUST 校验 parent_id 的有效性。

#### Scenario: 拥有父权限自动拥有子权限
- **WHEN** 角色关联了父权限 P（parent_id=0），子权限 P1（parent_id=P.permission_id）
- **THEN** 拥有 P 的用户自动拥有 P1 的访问权限

#### Scenario: 权限层级最大深度 2 级
- **WHEN** 系统中创建权限层级
- **THEN** 不允许出现 3 级或更深的嵌套（子权限的 parent_id 指向另一个子权限）

#### Scenario: 仅有子权限不继承父权限
- **WHEN** 角色仅关联子权限 P1（parent_id=P.permission_id），未关联父权限 P
- **THEN** 该用户拥有 P1 但不拥有 P 的访问权限

### Requirement: ROLE_ 前缀与权限模型映射
Spring Security 的 `hasRole()` 和 `hasAuthority()` 使用不同的匹配规则。系统 MUST 遵循以下约定：

- **角色（Role）**：Role 表的 `role_name` 字段存储不含前缀的角色名（如 `ADMIN`、`USER`）。`UserDetailServiceImpl` 将角色名映射为 `GrantedAuthority` 时 MUST 添加 `ROLE_` 前缀（即 `ROLE_ADMIN`、`ROLE_USER`），使 `hasRole("ADMIN")` 生效。
- **权限（Permission）**：Permission 表的 `perm_key` 直接作为 `GrantedAuthority`，不添加前缀（如 `file:upload`、`admin:user-manage`），使用 `hasAuthority('file:upload')` 检查。

**SecurityFilterChain 中的用法**：
- URL 级控制用 `hasRole("ADMIN")`（底层匹配 `ROLE_ADMIN`）
- 方法级控制用 `hasAuthority('resource:action')`（匹配 perm_key 原值）

#### Scenario: hasRole 匹配角色名
- **WHEN** 用户角色为 `ADMIN`，SecurityFilterChain 配置 `hasRole("ADMIN")`
- **THEN** Spring Security 内部比较 `ROLE_ADMIN`（GrantedAuthority）与 `ROLE_ADMIN`（hasRole 自动添加前缀），匹配成功

#### Scenario: hasAuthority 匹配权限编码
- **WHEN** 用户权限包含 `admin:user-manage`，方法注解 `@PreAuthorize("hasAuthority('admin:user-manage')")`
- **THEN** 直接比较 GrantedAuthority 值，匹配成功

### Requirement: 无角色用户处理
当用户未绑定任何角色（user_role 表无记录）或所有绑定角色的 available 均为 0 时，系统 MUST 正常处理认证请求。

#### Scenario: 无角色用户可登录但权限为空
- **WHEN** 用户未绑定任何角色，调用登录端点
- **THEN** 登录成功（密码正确即可），返回空 roles 列表和空 permissions 列表，用户只能访问 permitAll 端点

#### Scenario: 无角色用户访问受保护端点
- **WHEN** 无角色用户访问 `authenticated()` 保护的端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`（已认证但无权限）

### Requirement: 管理员用户管理端点
系统 SHALL 提供以下管理员端点，统一在 `/api/v1/admin/` 路径下，受 `hasRole("ADMIN")` URL 级控制：

| 端点 | 方法 | 权限注解 | 说明 |
|------|------|---------|------|
| `/api/v1/admin/users` | GET | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 分页查询用户列表 |
| `/api/v1/admin/users/{userId}` | GET | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 查询用户详情（含角色、权限） |
| `/api/v1/admin/users/{userId}/roles` | PUT | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 更新用户角色绑定，操作后 MUST 发布 PermissionChangedEvent |
| `/api/v1/admin/users/{userId}/password` | PUT | `@PreAuthorize("hasAuthority('admin:user-manage')")` | 重置用户密码（已在 password-management spec 中定义） |
| `/api/v1/admin/roles` | GET | `@PreAuthorize("hasAuthority('admin:role-manage')")` | 查询所有角色列表 |
| `/api/v1/admin/roles/{roleId}/permissions` | PUT | `@PreAuthorize("hasAuthority('admin:role-manage')")` | 更新角色权限绑定，操作后 MUST 发布 PermissionChangedEvent |

#### Scenario: 管理员更新用户角色后缓存失效
- **WHEN** 管理员调用 `PUT /api/v1/admin/users/{userId}/roles` 更新用户的角色绑定
- **THEN** 系统更新 user_role 表 → 发布 PermissionChangedEvent（目标 userId）→ 返回 200 OK

#### Scenario: 管理员更新角色权限后缓存失效
- **WHEN** 管理员调用 `PUT /api/v1/admin/roles/{roleId}/permissions` 更新角色的权限绑定
- **THEN** 系统更新 role_permission 表 → 查询所有拥有该角色的 userId → 对每个 userId 发布 PermissionChangedEvent → 返回 200 OK

#### Scenario: 无 admin:user-manage 权限被拒绝
- **WHEN** 用户拥有 ROLE_ADMIN 但不拥有 `admin:user-manage` 权限，调用用户管理端点
- **THEN** URL 级 `hasRole("ADMIN")` 通过，但 `@PreAuthorize` 拒绝，返回 403
