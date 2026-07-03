## Context

奇文网盘认证模块当前基于 Spring Boot 2 + Spring Security 5 + jjwt 0.9.x 构建。Spring Boot 3 升级带来多项破坏性变更：`WebSecurityConfigurerAdapter` 被移除、jjwt API 全面重构、`javax.*` 迁移至 `jakarta.*`。同时，旧系统在安全领域存在多个架构短板：MD5 密码算法、无 token 刷新机制、无登出黑名单、localStorage 存储 token 存在 XSS 风险。

本次升级利用 Spring Boot 3 迁移窗口，一次性解决认证模块的技术债和安全短板。涉及的利益相关方：后端开发（全部重写 Security 层）、前端开发（token 传输方式变更、Axios 拦截器重写）、运维（新增 Redis 依赖、密钥管理）。

核心约束：
- 现有用户数据必须兼容（密码透明迁移）
- API 路径全面变更，前后端必须同步上线
- Redis 为新增强依赖（token 黑名单、登录失败计数、refresh token 注册）

## Goals / Non-Goals

**Goals:**
- 完成 Spring Security 5 → 6 迁移，采用 SecurityFilterChain Bean 配置方式
- 完成 jjwt 0.9.x → 0.12.x 升级，重构所有 JWT 操作 API
- 实现双 token 机制（access 15min + refresh 7d），支持无感刷新
- 实现 token 黑名单和全局撤销机制，支持安全的登出和密码变更
- 实现 refresh token rotation + 重用检测，防止 token 窃取
- 密码算法从 MD5 迁移到 BCrypt，首次登录透明升级
- Token 传输从 localStorage 迁移到 httpOnly cookie
- API 路径统一到 `/api/v1/auth/` 前缀
- 权限编码统一为 `resource:action` 格式，权限变更通过事件驱动缓存失效

**Non-Goals:**
- OAuth2 / 社交登录（二期）
- 多因素认证 MFA（二期）
- SSO 单点登录（二期）
- 微信登录集成（移除旧端点，二期统一规划）
- 密码过期策略（二期）
- 用户注册邮箱验证（本期只做手机号注册）

## Decisions

### D1: Security 配置采用 SecurityFilterChain Bean 方式

**选择**: `@Bean SecurityFilterChain` + `@EnableMethodSecurity`
**替代方案**: 保留 `WebSecurityConfigurerAdapter` 的子类方式（不可行，Security 6 已移除）

**理由**: Spring Security 6 唯一推荐方式。组合式配置更灵活，支持多 FilterChain 按 request 匹配。

### D2: JWT 库选择 jjwt 0.12.x

**选择**: jjwt 0.12.x（`Jwts.builder().signWith(key, Jwts.SIG.HS256)` 新 API）
**替代方案**: 
- `nimbus-jose-jwt`：功能更全但学习成本高，团队已有 jjwt 经验
- `java-jwt`（Auth0）：API 更简洁但不支持复杂 claims 操作

**理由**: 保持与旧系统同一 JWT 库，降低迁移认知负担。0.12.x 的 type-safe API 比 0.9.x 更安全（编译期检查算法和密钥类型）。

### D3: 双 Token 方案 vs 单 Token + 滑动过期

**选择**: Access token（15min）+ Refresh token（7d），rotation 策略
**替代方案**: 单 access token + 滑动窗口续期

**理由**: 
- 双 token 分离了"短期认证"和"长期会话"两个关注点
- Access token 短有效期限制了泄露后的影响窗口
- Refresh token rotation + 重用检测是业界成熟的窃取检测方案
- Refresh token 存 Redis 支持主动撤销（单 token 方案难以实现）

### D4: Token 传输采用 httpOnly Cookie

**选择**: httpOnly cookie（`Set-Cookie`）+ 保留 Authorization header 兼容
**替代方案**: 继续用 localStorage

**理由**: 
- httpOnly cookie 对 XSS 攻击天然免疫（JS 无法读取）
- 配合 `SameSite=Lax` 可防御 CSRF（主要攻击面减少）
- 保留 Authorization header 读取能力，兼容非浏览器 API 调用场景
- 前端不再手动管理 token，降低出错概率

### D5: 密码迁移策略 — 首次登录透明升级

**选择**: 登录时双算法验证（先 BCrypt 后 MD5），MD5 匹配成功后自动用 BCrypt 重新 hash
**替代方案**: 
- 批量迁移脚本（需要所有用户同时改密码，体验差）
- 保留 MD5 不迁移（安全风险）

**理由**: 零用户感知，渐进式迁移。BCrypt strength=10 在安全性和性能间取得平衡。旧 salt 列在迁移后清空（BCrypt 内置 salt）。

### D6: Redis 作为 token 状态存储

**选择**: Redis SET 存储黑名单、refresh token 注册、登录失败计数
**替代方案**: 数据库存储 token 状态

**理由**: 
- Token 验证是高频操作（每次请求），Redis 的 O(1) 查询性能远优于数据库
- TTL 自动过期机制与 token 有效期天然匹配，无需手动清理
- 登录失败计数的原子递增操作 Redis 原生支持（INCR + EXPIRE）
- 新增 Redis 依赖可复用于权限缓存、会话管理等其他模块

### D7: 全局 Token 撤销采用时间戳方案

**选择**: Redis 记录 `revoke:all:{userId}` = 时间戳，Filter 比较 token 的 `iat` 与撤销时间
**替代方案**: 逐个将用户所有 jti 加入黑名单

**理由**: 
- O(1) 写入 vs O(n) 写入（n = 该用户已签发的 token 数量）
- 不需要遍历 Redis 中的所有 token jti
- 时间戳比较在 Filter 中是轻量操作
- TTL 7 天自动清理，覆盖 refresh token 最长有效期

### D8: 权限缓存失效采用 Spring ApplicationEvent

**选择**: 权限变更时发布 `PermissionChangedEvent`，监听器删除 Redis 缓存
**替代方案**: 
- 直接在 Service 中调用 `redisTemplate.delete()`
- 使用 Redis keyspace notification

**理由**: 
- 事件驱动解耦了权限管理和缓存管理
- 未来扩展（如通知、审计）只需添加新监听器
- Spring 内置事件机制，无需额外依赖
- 缓存 TTL 5 分钟作为兜底，即使事件丢失也能在 5 分钟内自动恢复

### D9: 后端包结构 — 按功能域组织

**选择**: `com.qiwenshare.auth` 下按 config/filter/handler/service/controller/dto/vo/entity/repository/exception 分包
**替代方案**: 按 DDD 领域驱动分包（domain/application/infrastructure）

**理由**: 
- 与旧系统包结构保持一致性，降低迁移心智负担
- 认证模块边界清晰，功能域分包足够
- 团队对三层架构更熟悉
- DDD 分包在单模块中过度设计

## Risks / Trade-offs

### R1: Redis 成为认证流程的强依赖
**风险**: Redis 不可用时，token 黑名单检查、refresh token 验证、登录失败计数全部失效，导致无法登录或已登出用户仍可访问。
**缓解**: 
- Redis 配置哨兵/集群模式保证高可用
- Token 验证逻辑中 Redis 不可用时降级为仅验证 JWT 签名和过期时间（放弃黑名单检查，接受短暂安全风险窗口）
- 健康检查端点监控 Redis 连接状态

### R2: Refresh token 重用检测的误判
**风险**: 正常用户因网络问题收到新 refresh token 但客户端仍用旧 token 重试，触发重用检测导致所有 token 失效。
**缓解**: 
- 前端 Axios 拦截器确保同一时间只有一个 refresh 请求（请求队列）
- 重用检测后不立即全部撤销，而是返回 401 让客户端重新登录
- 日志记录重用检测事件，便于排查

### R3: MD5 透明迁移的并发安全
**风险**: 同一用户并发登录时，两个请求同时检测到 MD5 密码并尝试迁移，可能导致数据竞争。
**缓解**: 
- 使用数据库乐观锁（`UPDATE user SET password=? WHERE user_id=? AND password=?`，WHERE 条件包含旧密码 hash）
- 只有一个请求会成功更新，其他请求下次登录时走 BCrypt 路径

### R4: Cookie 跨域问题
**风险**: 前后端不同域部署时，httpOnly cookie 可能因第三方 cookie 限制被浏览器拦截。
**缓解**: 
- 开发环境用 Vite proxy 代理 API 请求（同源）
- 生产环境同域部署（前端和 API 在同一域名下，通过 Nginx 路由）
- 保留 Authorization header 作为 fallback

### R5: 前后端必须同步上线
**风险**: API 路径全部变更（`/user/` → `/api/v1/auth/`），前后端不能独立部署。
**缓解**: 
- 后端可提供临时兼容层（旧路径转发到新路径），但增加维护成本
- 推荐方案：前后端同时发布，使用蓝绿部署减少停机时间
- 新系统上线前在 staging 环境完成全量 E2E 测试

### R6: JWT 密钥管理
**风险**: 密钥泄露导致所有 token 可被伪造。
**缓解**: 
- 密钥从环境变量 `JWT_SECRET` 读取，禁止硬编码
- 启动时校验密钥长度 ≥ 256 bit
- 生产环境通过密钥管理服务（KMS）注入
- 密钥轮换需要所有用户重新登录（通过全局撤销机制）

---

## Supplement: 类清单

### 包结构总览
```
com.qiwenshare.auth
├── config/          # 配置类
├── filter/          # Security Filter
├── handler/         # 认证/授权异常处理
├── service/         # 业务逻辑
├── controller/      # REST 端点
├── dto/             # 请求体 DTO（record 类型）
├── vo/              # 响应体 VO（record 类型）
├── entity/          # JPA Entity
├── repository/      # JpaRepository 接口
├── exception/       # 自定义异常
└── event/           # Spring ApplicationEvent
```

### 类清单表

| 包 | 类名 | 类型 | 职责 |
|---|------|------|------|
| config | `SecurityConfig` | @Configuration | SecurityFilterChain Bean、PasswordEncoder、CORS |
| config | `RedisConfig` | @Configuration | RedisTemplate<String, String> Bean 序列化配置 |
| config | `JwtProperties` | @ConfigurationProperties record | JWT 配置（secret、TTL、clockSkew） |
| config | `AuthProperties` | @ConfigurationProperties record | 认证配置（登录失败锁定次数、锁定时间） |
| filter | `JwtAuthenticationFilter` | OncePerRequestFilter | Token 解析 → 黑名单/撤销检查 → 设置 SecurityContext |
| handler | `AuthEntryPoint` | AuthenticationEntryPoint | 401 响应处理（RestResult 格式） |
| handler | `AccessDeniedHandlerImpl` | AccessDeniedHandler | 403 响应处理（RestResult 格式） |
| service | `TokenService` | @Service | JWT 生成/解析/黑名单/撤销/refresh rotation |
| service | `AuthService` | @Service | 注册/登录/登出/刷新/修改密码/重置密码 |
| service | `UserDetailServiceImpl` | @Service | UserDetailsService 实现，权限加载与缓存 |
| controller | `AuthController` | @RestController | `/api/v1/auth/*` 端点 |
| controller | `AdminUserController` | @RestController | `/api/v1/admin/users/*` 端点 |
| controller | `AdminRoleController` | @RestController | `/api/v1/admin/roles/*` 端点 |
| dto | `RegisterRequest` | record | 注册请求体 |
| dto | `LoginRequest` | record | 登录请求体 |
| dto | `ChangePasswordRequest` | record | 修改密码请求体 |
| dto | `ResetPasswordRequest` | record | 重置密码请求体 |
| dto | `UpdateRolesRequest` | record | 更新角色绑定请求体 |
| dto | `UpdatePermissionsRequest` | record | 更新权限绑定请求体 |
| vo | `LoginResponse` | record | 登录响应（userId、roles、permissions） |
| vo | `UserInfoResponse` | record | 用户信息响应（含脱敏手机号） |
| vo | `TokenPair` | record | access + refresh token 对 |
| entity | `User` | @Entity | 用户表实体 |
| entity | `Role` | @Entity | 角色表实体 |
| entity | `Permission` | @Entity | 权限表实体 |
| entity | `UserRole` | @Entity @IdClass | 用户-角色关联实体 |
| entity | `RolePermission` | @Entity @IdClass | 角色-权限关联实体 |
| repository | `UserRepository` | JpaRepository | findByTelephone/findByUserId/exists* |
| repository | `RoleRepository` | JpaRepository | findByRoleId/findByAvailable |
| repository | `PermissionRepository` | JpaRepository | findByPermKeyIn |
| repository | `UserRoleRepository` | JpaRepository | findByUserId/deleteByUserId |
| repository | `RolePermissionRepository` | JpaRepository | findByRoleId/deleteByRoleId |
| exception | `AuthException` | RuntimeException | 认证模块业务异常 |
| exception | `AuthErrorCode` | enum | 错误码枚举（含 httpStatus + message） |
| event | `PermissionChangedEvent` | ApplicationEvent | 权限变更事件（含 userId 列表） |
| event | `PermissionChangeEventPublisher` | @Component | 事件发布器 |
| event | `PermissionCacheInvalidator` | @EventListener | 监听事件 → 删除 Redis 缓存 |
| common | `SnowflakeIdGenerator` | @Component | Snowflake ID 生成器（基于 Twitter Snowflake） |
| common | `RestResult<T>` | record | 统一响应包装（code/message/data） |
| common | `GlobalExceptionHandler` | @RestControllerAdvice | 全局异常处理（AuthException + MethodArgumentNotValidException + AccessDeniedException） |
| common | `CookieUtils` | utility class | httpOnly cookie 构建与清除 |

## Supplement: 数据库 Schema

### Flyway V1__create_auth_tables.sql

```sql
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`     VARCHAR(32)  NOT NULL COMMENT 'Snowflake 业务 ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `telephone`   VARCHAR(20)  NOT NULL COMMENT '手机号',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt hash',
  `old_password` VARCHAR(64) DEFAULT NULL COMMENT 'MD5 旧密码（迁移完成后清空）',
  `salt`        VARCHAR(64)  DEFAULT NULL COMMENT '旧 MD5 salt（迁移完成后清空）',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `register_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `available`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_telephone` (`telephone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `role` (
  `role_id`     INT          NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `role_name`   VARCHAR(30)  NOT NULL COMMENT '角色名称（不含 ROLE_ 前缀）',
  `role_desc`   VARCHAR(100) DEFAULT NULL COMMENT '角色描述',
  `available`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `permission` (
  `permission_id` INT        NOT NULL AUTO_INCREMENT COMMENT '权限 ID',
  `perm_key`    VARCHAR(50)  NOT NULL COMMENT '权限编码 resource:action',
  `perm_name`   VARCHAR(50)  NOT NULL COMMENT '权限名称',
  `parent_id`   INT          NOT NULL DEFAULT 0 COMMENT '父权限 ID，0 表示顶级',
  `perm_type`   TINYINT      NOT NULL DEFAULT 1 COMMENT '1-菜单 2-按钮 3-API',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_perm_key` (`perm_key`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `user_role` (
  `user_id`     BIGINT       NOT NULL COMMENT 'user.id',
  `role_id`     INT          NOT NULL COMMENT 'role.role_id',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `role_permission` (
  `role_id`       INT        NOT NULL COMMENT 'role.role_id',
  `permission_id` INT        NOT NULL COMMENT 'permission.permission_id',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
```

### Flyway V2__init_auth_data.sql

```sql
INSERT INTO `role` (`role_id`, `role_name`, `role_desc`, `available`) VALUES
  (1, 'ADMIN', '系统管理员', 1),
  (2, 'USER', '普通用户', 1);

INSERT INTO `permission` (`perm_key`, `perm_name`, `parent_id`, `perm_type`) VALUES
  ('admin:user-manage', '用户管理', 0, 2),
  ('admin:role-manage', '角色管理', 0, 2),
  ('file:upload', '文件上传', 0, 3),
  ('file:download', '文件下载', 0, 3),
  ('file:delete', '文件删除', 0, 3),
  ('file:move', '文件移动', 0, 3),
  ('file:rename', '文件重命名', 0, 3),
  ('file:share', '文件分享', 0, 3),
  ('file:recycle', '回收站操作', 0, 3);

-- ADMIN 拥有所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `permission_id` FROM `permission`;

-- USER 拥有文件操作权限（不含 admin:*）
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, `permission_id` FROM `permission` WHERE `perm_key` NOT LIKE 'admin:%';
```

## Supplement: Redis Key 总表

| Key 模式 | 数据类型 | TTL | 用途 | 写入方 | 读取方 |
|----------|---------|-----|------|--------|--------|
| `token:blacklist:{jti}` | STRING (值="1") | = token 剩余有效期 | access token 黑名单 | TokenService.blacklist() | JwtAuthenticationFilter |
| `token:refresh:{jti}` | STRING (值=userId) | 7d | refresh token 注册表 | TokenService.registerRefreshToken() | TokenService.consumeRefreshToken() |
| `revoke:all:{userId}` | STRING (值=Unix 时间戳) | 7d | 全局撤销时间戳 | TokenService.revokeAllTokens() | JwtAuthenticationFilter |
| `login:fail:{telephone}` | STRING (值=失败次数) | 15min | 登录失败计数 | AuthService.login() | AuthService.login() |
| `user:perms:{userId}` | STRING (值=权限 JSON 数组) | 5min | 用户权限缓存 | UserDetailServiceImpl | UserDetailServiceImpl |
| `revoke:refresh:all:{userId}` | STRING (值=Unix 时间戳) | 7d | 全局撤销 refresh token | TokenService.revokeAllRefreshTokens() | TokenService.consumeRefreshToken() |

**Key 命名规范**：
- 所有认证相关 key 以 `token:` 或 `login:` 或 `user:` 或 `revoke:` 为前缀
- 生产环境追加应用名前缀 `qw:auth:` 避免多应用冲突
- TTL 策略：与业务生命周期匹配，无需手动清理

**降级策略**：
- Redis 不可用时，`token:blacklist` 和 `revoke:all` 检查跳过（接受最多 15min 的安全窗口）
- `token:refresh` 不可用时拒绝刷新（返回 401，用户重新登录）
- `user:perms` 不可用时回退到数据库查询
- `login:fail` 不可用时跳过锁定检查（接受暴力攻击风险窗口）

## Supplement: Security Filter Chain 顺序

```
HTTP Request
    │
    ▼
┌─────────────────────────────────────┐
│  CorsFilter (Spring 内置)            │  ← CorsConfigurationSource 配置
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  SecurityFilterChain 匹配            │  ← requestMatchers 选择 FilterChain
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  JwtAuthenticationFilter             │  ← addFilterBefore(UsernamePasswordAuthenticationFilter)
│  ① Cookie/Header 提取 token          │
│  ② 解析 JWT (jjwt 0.12.x)           │
│  ③ 检查 type == "access"            │
│  ④ 黑名单检查 (token:blacklist:{jti})│
│  ⑤ 全局撤销检查 (iat vs revoke:all) │
│  ⑥ 加载 UserDetails                 │
│  ⑦ 设置 SecurityContext             │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  AuthorizationFilter                 │  ← Spring Security 6 内置
│  ① URL 级：hasRole("ADMIN") for /api/v1/admin/**
│  ② 方法级：@PreAuthorize             │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  ExceptionTranslationFilter          │
│  ① AuthenticationException → AuthEntryPoint (401)
│  ② AccessDeniedException → AccessDeniedHandlerImpl (403)
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Controller                          │
└─────────────────────────────────────┘
```

**permitAll 端点白名单**（不经过 JwtAuthenticationFilter 的验证链，但 Filter 仍执行，仅不要求 token）：
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `GET /actuator/health`
- `GET /swagger-ui/**`, `GET /v3/api-docs/**`
