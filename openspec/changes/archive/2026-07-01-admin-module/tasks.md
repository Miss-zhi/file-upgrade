## 1. 数据库迁移

- [x] 1.1 创建 Flyway 迁移脚本 `V6__create_admin_tables.sql`：创建 `operation_log` 表（id, user_id, username, module, action, description, request_method, request_uri, request_params, response_code, error_message, ip_address, user_agent, execution_time, create_time）、`system_config` 表（id, config_key UNIQUE, config_value, description, create_time, update_time），插入初始系统参数，插入 admin 权限数据（admin:quota-manage, admin:log-view, admin:config-manage）及 ADMIN 角色关联

## 2. Admin 模块骨架

- [x] 2.1 创建 `com.qiwenshare.admin` 包结构（配额/日志/配置三个子模块）：controller/、service/、repository/、entity/、dto/、vo/、common/
- [x] 2.2 创建 Admin 模块错误码枚举 `AdminErrorCode`（USER_NOT_FOUND, CANNOT_DISABLE_SELF, INVALID_QUOTA, CONFIG_KEY_DUPLICATE, CONFIG_NOT_FOUND, USER_DISABLED）
- [x] 2.3 创建 `AdminModuleException` 异常类，遵循项目统一异常处理规范

## 3. 系统参数管理

- [x] 3.1 创建 `SystemConfig` 实体类（JPA Entity，映射 system_config 表）
- [x] 3.2 创建 `SystemConfigRepository`（JPA Repository，含 findByConfigKey 方法）
- [x] 3.3 创建 `SystemConfigService`：CRUD 操作 + Redis 缓存读取（key: `sys:config:{configKey}`，TTL 10 分钟）
- [x] 3.4 创建 SystemConfig 相关 DTO/VO（CreateConfigDTO, UpdateConfigDTO, ConfigVO）
- [x] 3.5 创建 `SystemConfigController`：GET/POST/PUT/DELETE `/api/v1/admin/config`，标注 `@PreAuthorize("hasAuthority('admin:config-manage')")`

## 4. 审计日志系统

- [x] 4.1 创建 `OperationLog` 实体类（JPA Entity，映射 operation_log 表）
- [x] 4.2 创建 `OperationLogRepository`（JPA Repository，含分页查询和条件过滤方法）
- [x] 4.3 创建 `@AuditLog` 自定义注解（属性：module, action, description）
- [x] 4.4 创建 `AuditLogAspect` 切面：拦截 `@AuditLog` 注解方法，异步记录操作日志（含请求参数脱敏、执行耗时计算）
- [x] 4.5 创建 `OperationLogService`：分页查询 + 条件过滤（module, action, username, startTime, endTime）
- [x] 4.6 创建 OperationLog 相关 VO（OperationLogVO）
- [x] 4.7 创建 `OperationLogController`：GET `/api/v1/admin/logs`，标注 `@PreAuthorize("hasAuthority('admin:log-view')")`

## 5. 用户管理

- [x] 5.1 在 `com.qiwenshare.auth` 包中创建用户管理相关 DTO/VO：UserListQuery（查询参数 record）、UserListVO（列表响应项）、UserDetailVO（详情响应，含角色/权限列表）
- [x] 5.2 创建 `AdminUserService`：分页查询用户列表（支持 keyword、available 过滤）、查询用户详情（含角色/权限）
- [x] 5.3 在 `AdminUserService` 中实现启用/禁用用户逻辑（修改 available 字段，发布 PermissionChangedEvent，禁用时清除 Redis 会话缓存）
- [x] 5.4 确认现有 `AuthService.resetPassword()` 已满足管理员重置密码需求，`AdminUserController` 的 password 端点继续委托给 AuthService，无需在 AdminUserService 中重复实现
- [x] 5.5 扩展现有 auth-module 的 `AdminUserController`（`com.qiwenshare.auth.controller`），注入 `AdminUserService`，新增 GET list、GET /{userId}、PUT enable、PUT disable 四个端点，标注 `@PreAuthorize("hasAuthority('admin:user-manage')")`，关键方法标注 `@AuditLog`。已有 `PUT /{userId}/password` 端点保持不变
- [x] 5.6 在 UserDetailServiceImpl 中添加 available=0 用户登录拒绝逻辑（已存在）

## 6. 配额管理

- [x] 6.1 创建配额管理相关 VO（AdminQuotaVO, SetQuotaDTO, BatchSetQuotaDTO）
- [x] 6.2 创建 `AdminQuotaService`：调用 StorageQuotaService 实现查询/设置/批量设置配额
- [x] 6.3 创建 `AdminQuotaController`：GET/PUT `/api/v1/admin/quota/{userId}`, PUT `/api/v1/admin/quota/batch`，标注 `@PreAuthorize("hasAuthority('admin:quota-manage')")`，关键方法标注 `@AuditLog`

## 7. 权限数据初始化

> 权限数据的 INSERT 已包含在任务 1.1 的 V6 迁移脚本中。本节为验证任务。

- [x] 7.1 验证 V6 迁移脚本正确插入了 `admin:quota-manage`、`admin:log-view`、`admin:config-manage` 权限记录及 ADMIN 角色关联（`admin:user-manage` 已在 V1 中创建）

## 8. 集成测试

- [x] 8.1 编写 AdminUserController 集成测试（用户列表、启用/禁用、重置密码）
- [x] 8.2 编写 AdminQuotaController 集成测试（查询/设置/批量设置配额）
- [x] 8.3 编写 OperationLogController 集成测试（分页查询、条件过滤）
- [x] 8.4 编写 SystemConfigController 集成测试（CRUD + 缓存行为验证）
- [x] 8.5 编写 AuditLogAspect 单元测试（日志记录、参数脱敏、异常场景）

## 9. 跨模块迁移

- [x] 9.1 从 file-module `QuotaController` 移除管理员配额端点，QuotaController 仅保留用户侧 `GET /api/v1/quota/info`
- [x] 9.2 修复 auth-module `AdminRoleController` 类级 `@PreAuthorize` 注解：从 `admin:user-manage` 改为 `admin:role-manage`
