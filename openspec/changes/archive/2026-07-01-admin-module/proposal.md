## Why

系统缺少管理员后台管理能力。当前 auth-module 提供了 RBAC 权限模型和用户管理端点骨架，file-module 提供了 StorageQuotaService，但没有统一的管理员控制台后端。需要构建 admin 模块，使管理员能够管理用户生命周期、调整存储配额、审计操作日志、配置系统参数。

## What Changes

- 新增 `admin/` 后端模块，路径前缀 `/api/v1/admin`，仅 `ROLE_ADMIN` 可访问
- 用户管理：分页查询用户列表、查看用户详情（含角色/权限）、启用/禁用用户、重置用户密码
- 存储配额管理：查看用户配额使用情况、设置/调整用户配额、批量设置配额
- 操作日志：基于 AOP 的审计日志记录（`@AuditLog` 注解），记录管理员关键操作，支持分页查询和条件过滤
- 系统参数 CRUD：管理系统级键值对配置（如默认配额、文件大小限制等），支持增删改查

## Capabilities

### New Capabilities
- `admin-user-management`: 管理员对用户的 CRUD 操作，包括列表查询、启用/禁用、重置密码。**扩展现有 auth-module 的 `AdminUserController`**（`com.qiwenshare.auth.controller`），在已有 `PUT /{userId}/password` 端点基础上新增 list/detail/enable/disable 端点。新增 `AdminUserService` 在 `com.qiwenshare.auth.service` 中提供业务逻辑。
- `admin-quota-management`: 管理员为用户设置和调整存储配额，调用已有 StorageQuotaService，提供管理员视角的配额查询和批量操作接口。
- `admin-audit-log`: 基于 AOP 的审计日志系统，通过 `@AuditLog` 注解自动记录管理员操作，支持分页查询和多条件过滤。
- `admin-system-config`: 系统参数的 CRUD 管理，维护 `system_config` 表，支持缓存加速读取。

### Modified Capabilities
- `rbac-permission`: 新增用户启用/禁用状态字段支持，admin 端点增加 enable/disable 接口，权限缓存需考虑用户禁用场景。
- `storage-quota`: 管理员配额管理端点从 admin 模块暴露，复用 StorageQuotaService 的底层能力。

## Impact

- **后端代码**：
  - 扩展 auth-module 的 `AdminUserController`（新增 list/detail/enable/disable 端点），新增 `AdminUserService` 在 `com.qiwenshare.auth.service`
  - 新增 `com.qiwenshare.admin` 包（controller/service/repository/entity/dto/vo），包含 AdminQuotaController、OperationLogController、SystemConfigController 及对应 Service
  - 新增 AOP 切面类 `AuditLogAspect` 在 `com.qiwenshare.admin.common`
- **数据库**：新增 `operation_log` 表（审计日志）、`system_config` 表（系统参数），`user` 表已有 `available` 字段无需新增
- **API**：新增 `/api/v1/admin/users`（扩展已有）、`/api/v1/admin/quota`、`/api/v1/admin/logs`、`/api/v1/admin/config` 四组端点
- **迁移**：从 file-module `QuotaController` 移除 `PUT /api/v1/admin/quota/{userId}` 管理员端点，统一收归 admin-module
- **依赖**：依赖 auth-module 的 UserService、RoleRepository、PermissionChangedEvent；依赖 file-module 的 StorageQuotaService
- **安全**：复用现有 SecurityFilterChain 的 `/api/v1/admin/**` → `hasRole("ADMIN")` 规则和 `@EnableMethodSecurity`，新增 `@PreAuthorize` 方法级控制
