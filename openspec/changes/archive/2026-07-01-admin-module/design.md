## Context

当前系统已完成 auth-module（RBAC 五表模型、JWT 认证、SecurityFilterChain URL 级权限控制）和 file-module（文件上传/下载/分享/恢复、StorageQuotaService 配额管理）。User 实体有 `available` 字段（0=禁用，1=启用），SecurityFilterChain 已配置 `/api/v1/admin/**` → `hasRole("ADMIN")`。

file-module 中的 AuditLogService 仅记录文件下载审计，不覆盖管理员操作审计。需要新建 admin 模块提供完整的管理员后台能力。

## Goals / Non-Goals

**Goals:**
- 用户管理：分页列表、详情查询、启用/禁用、重置密码
- 配额管理：查看用户配额、设置/调整配额、批量设置
- 操作审计：AOP 切面 + `@AuditLog` 注解自动记录管理员操作，支持分页查询和条件过滤
- 系统配置：键值对形式的系统参数 CRUD，Redis 缓存加速
- 所有端点 `/api/v1/admin/**`，仅 ROLE_ADMIN 可访问

**Non-Goals:**
- 前端管理界面（后续 admin-module-frontend 变更处理）
- 角色/权限的 CRUD（已在 rbac-permission spec 中定义）
- 文件级别审计日志（属于 file-module）
- 实时通知/告警系统

## Decisions

### D1: 混合包结构——auth 扩展 + admin 新包

**决定**：用户管理端点（list/detail/enable/disable/password）扩展现有 auth-module 的 `AdminUserController`（`com.qiwenshare.auth.controller`），新增 `AdminUserService` 在 `com.qiwenshare.auth.service`。配额管理、审计日志、系统参数三个子模块新建 `com.qiwenshare.admin` 包，包含 controller/service/repository/entity/dto/vo/common 子包。

**理由**：auth-module 已有 `AdminUserController` 映射 `/api/v1/admin/users` 且包含密码重置端点，新建同名 Controller 会导致 Spring Bean 冲突和端点路径冲突。用户管理与认证同属身份域，放在 auth 包内合理。配额/日志/配置是独立关注点，放入 admin 包。

**替代方案**：全部放入 admin 包并删除 auth 的 AdminUserController → 密码重置需跨模块调用 AuthService，增加耦合。

### D2: 操作审计使用 AOP + 自定义注解

**决定**：定义 `@AuditLog` 注解（标注在 Controller 方法上），通过 `@Aspect` 切面在方法执行后异步记录操作日志到 `operation_log` 表。

**理由**：
- 声明式审计，Controller 方法无需手动调用日志写入
- 与 file-module 的 AuditLogService（文件下载审计）职责分离
- 异步写入不影响接口性能

**替代方案**：Spring Event → 可行但增加间接层，AOP 更简洁直接。

### D3: 系统参数使用键值对表 + Redis 缓存

**决定**：`system_config` 表存储 `config_key` / `config_value` / `description`，读取时先查 Redis（key: `sys:config:{configKey}`，TTL 10 分钟），miss 时查 DB 并回填。写入时更新 DB 并删除缓存。

**理由**：系统参数读多写少，缓存加速且保证最终一致性。键值对形式灵活，无需为每个参数定义表字段。

**替代方案**：Nacos/Apollo 配置中心 → 引入额外基础设施依赖，当前阶段不需要。

### D4: 用户启用/禁用复用 User.available 字段

**决定**：复用 User 实体已有的 `available` 字段（0=禁用，1=启用），不新增字段。禁用用户时设置 `available=0`，同时发布 PermissionChangedEvent 清除权限缓存。

**理由**：User 表已有此字段且 rbac-permission spec 中角色可用性检查也使用 `available` 概念。禁用用户后，UserDetailServiceImpl 加载用户时可检查此字段拒绝登录。

### D5: Flyway 迁移脚本 V6

**决定**：新增 `V6__create_admin_tables.sql`，创建 `operation_log` 表和 `system_config` 表，插入初始系统参数和 admin 模块权限数据（`admin:quota-manage`、`admin:log-view`、`admin:config-manage`）及角色关联。

**理由**：遵循项目数据迁移规范，生产环境禁用 `ddl-auto=update`，所有 schema 变更通过 Flyway 管理。`admin:user-manage` 已在 auth-module V1 中创建，此处仅补增其余 admin 权限。

## Risks / Trade-offs

- **[操作日志增长]** → 管理员操作频繁时 operation_log 表可能膨胀。缓解：设置合理的分页查询限制，后续可增加日志归档策略。
- **[系统配置缓存一致性]** → Redis 缓存与 DB 之间短暂不一致。缓解：TTL 10 分钟足够短，写操作主动删缓存。
- **[禁用用户体验]** → 用户被禁用后已建立的会话不会立即失效。缓解：禁用时清除该用户的 Redis 会话和权限缓存，下次请求时 JWT 验证会因用户状态检查失败而拒绝。

## Supplement: API 端点表

### auth-module 扩展端点（AdminUserController）

| Controller | Method | Path | Auth | 说明 |
|---|---|---|---|---|
| **AdminUserController** (auth, 已有) | PUT | `/api/v1/admin/users/{userId}/password` | `admin:user-manage` | 管理员重置密码（已有） |
| | GET | `/api/v1/admin/users` | `admin:user-manage` | 分页用户列表（keyword/available 过滤） |
| | GET | `/api/v1/admin/users/{userId}` | `admin:user-manage` | 用户详情（含角色/权限） |
| | PUT | `/api/v1/admin/users/{userId}/enable` | `admin:user-manage` | 启用用户 |
| | PUT | `/api/v1/admin/users/{userId}/disable` | `admin:user-manage` | 禁用用户 |

### admin 包新增端点

| Controller | Method | Path | Auth | 说明 |
|---|---|---|---|---|
| **AdminQuotaController** | GET | `/api/v1/admin/quota/{userId}` | `admin:quota-manage` | 查询用户配额 |
| | PUT | `/api/v1/admin/quota/{userId}` | `admin:quota-manage` | 设置用户配额 |
| | PUT | `/api/v1/admin/quota/batch` | `admin:quota-manage` | 批量设置配额 |
| **OperationLogController** | GET | `/api/v1/admin/logs` | `admin:log-view` | 分页查询操作日志（module/action/username/时间范围过滤） |
| **SystemConfigController** | GET | `/api/v1/admin/config` | `admin:config-manage` | 分页查询系统参数（keyword 搜索） |
| | POST | `/api/v1/admin/config` | `admin:config-manage` | 新增系统参数 |
| | PUT | `/api/v1/admin/config/{id}` | `admin:config-manage` | 修改系统参数 |
| | DELETE | `/api/v1/admin/config/{id}` | `admin:config-manage` | 删除系统参数 |

**总计：13 个端点**（5 auth 扩展 + 8 admin 新增）

## Supplement: 类清单

### auth 包新增（com.qiwenshare.auth）

| 子包 | 类名 | 类型 | 职责 |
|---|---|---|---|
| service | `AdminUserService` | @Service | 用户列表/详情/启用/禁用业务逻辑 |
| dto | `UserListQuery` | record | 用户列表查询参数（keyword, available, page, pageSize） |
| vo | `UserListVO` | record | 用户列表响应项（userId, username, telephone, available, registerTime, roles） |
| vo | `UserDetailVO` | record | 用户详情响应（含角色列表和权限列表） |

### admin 包新增（com.qiwenshare.admin）

| 子包 | 类名 | 类型 | 职责 |
|---|---|---|---|
| controller | `AdminQuotaController` | @RestController | 管理员配额管理端点 |
| controller | `OperationLogController` | @RestController | 操作日志查询端点 |
| controller | `SystemConfigController` | @RestController | 系统参数 CRUD 端点 |
| service | `AdminQuotaService` | @Service | 调用 StorageQuotaService 实现配额操作 |
| service | `OperationLogService` | @Service | 操作日志分页查询和条件过滤 |
| service | `SystemConfigService` | @Service | 系统参数 CRUD + Redis 缓存 |
| repository | `OperationLogRepository` | JpaRepository | 操作日志数据访问 |
| repository | `SystemConfigRepository` | JpaRepository | 系统参数数据访问 |
| entity | `OperationLog` | @Entity | 操作日志实体 |
| entity | `SystemConfig` | @Entity | 系统参数实体 |
| dto | `SetQuotaDTO` | record | 设置配额请求体 |
| dto | `BatchSetQuotaDTO` | record | 批量设置配额请求体 |
| dto | `CreateConfigDTO` | record | 新增系统参数请求体 |
| dto | `UpdateConfigDTO` | record | 修改系统参数请求体 |
| vo | `AdminQuotaVO` | record | 配额信息响应 |
| vo | `OperationLogVO` | record | 操作日志响应 |
| vo | `ConfigVO` | record | 系统参数响应 |
| common | `AuditLog` | @interface | 审计日志注解（module, action, description） |
| common | `AuditLogAspect` | @Aspect @Component | 审计日志 AOP 切面 |
| common | `AdminErrorCode` | enum | 错误码枚举 |
| common | `AdminModuleException` | RuntimeException | 模块业务异常 |
| common | `AdminGlobalExceptionHandler` | @RestControllerAdvice | 处理 AdminModuleException，返回 RestResult 错误响应 |
| common | `OperationLogAsyncWriter` | @Component | 审计日志异步写入器（独立组件，确保 @Async 代理生效） |
| vo | `PageResponse` | record | 通用分页响应包装，解决 Page 接口 Jackson 序列化问题 |

## Supplement: Modified Modules

### auth-module 变更

| 类 | 变更类型 | 说明 |
|---|---|---|
| `AdminUserController` | 扩展 | 新增 list/detail/enable/disable 4 个端点，注入 AdminUserService |
| `AdminUserService` | 新增 | 用户列表/详情/启用/禁用业务逻辑 |
| `UserDetailServiceImpl` | 修改 | loadUserByUsername 添加 available=0 检查，返回 USER_DISABLED |
| `AdminRoleController` | 修复 | 类级 `@PreAuthorize` 从 `admin:user-manage` 改为 `admin:role-manage` |

### file-module 变更

| 类 | 变更类型 | 说明 |
|---|---|---|
| `QuotaController` | 移除端点 | 删除 `PUT /api/v1/admin/quota/{userId}`，管理员配额统一由 admin-module 提供 |

## Supplement: 数据库 Schema

### Flyway V6__create_admin_tables.sql

```sql
CREATE TABLE `operation_log` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         VARCHAR(32)  NOT NULL COMMENT '操作者业务 ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '操作者用户名',
  `module`          VARCHAR(50)  NOT NULL COMMENT '模块名',
  `action`          VARCHAR(20)  NOT NULL COMMENT '操作类型 CREATE/UPDATE/DELETE',
  `description`     VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `request_method`  VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法',
  `request_uri`     VARCHAR(255) NOT NULL COMMENT '请求 URI',
  `request_params`  TEXT         DEFAULT NULL COMMENT '请求参数 JSON',
  `response_code`   INT          NOT NULL COMMENT '响应状态码',
  `error_message`   TEXT         DEFAULT NULL COMMENT '异常信息',
  `ip_address`      VARCHAR(50)  DEFAULT NULL COMMENT '客户端 IP',
  `user_agent`      VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `execution_time`  BIGINT       NOT NULL COMMENT '执行耗时 ms',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE `system_config` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `config_key`    VARCHAR(100) NOT NULL COMMENT '参数键名',
  `config_value`  VARCHAR(500) NOT NULL COMMENT '参数值',
  `description`   VARCHAR(200) DEFAULT NULL COMMENT '参数描述',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

-- 初始系统参数
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
  ('default.storage.quota', '10737418240', '默认存储配额（字节），10GB'),
  ('upload.max.size', '104857600', '单文件上传最大大小（字节），100MB'),
  ('upload.chunk.size', '5242880', '分片上传分片大小（字节），5MB'),
  ('share.default.expire.days', '7', '分享链接默认有效期（天）');

-- admin 模块权限数据（admin:user-manage 已在 V1 创建）
INSERT INTO `permission` (`perm_key`, `perm_name`, `parent_id`, `perm_type`) VALUES
  ('admin:quota-manage', '配额管理', 0, 3),
  ('admin:log-view', '日志查看', 0, 3),
  ('admin:config-manage', '系统配置管理', 0, 3);

-- 将新权限关联到 ADMIN 角色（role_id = 1）
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `permission_id` FROM `permission`
WHERE `perm_key` IN ('admin:quota-manage', 'admin:log-view', 'admin:config-manage');
```

## Supplement: Redis Key 表

| Key | 类型 | TTL | 用途 | 写入方 | 读取方 |
|---|---|---|---|---|---|
| `sys:config:{configKey}` | STRING | 10min | 系统参数缓存 | SystemConfigService | SystemConfigService |
| `user:session:{userId}` | STRING | 按 JWT TTL | 用户会话标记（禁用时删除） | AdminUserService.disable() | JwtAuthenticationFilter |
