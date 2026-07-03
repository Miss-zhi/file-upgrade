## Why

旧系统（Spring Boot 2 + Spring Security 5）的认证模块存在多个架构短板：WebSecurityConfigurerAdapter 在 Security 6 中被移除导致整个配置需要重写；JWT 库 jjwt 0.9→0.12 API 全面变化；密码算法使用不安全的 MD5；无 token 刷新机制和黑名单机制；前端 token 存储在 localStorage 存在 XSS 风险。Spring Boot 3 升级是解决这些技术债的最佳时机。

## What Changes

- **BREAKING** Spring Security 配置从 WebSecurityConfigurerAdapter 迁移到 SecurityFilterChain Bean 方式
- **BREAKING** JWT 库从 jjwt 0.9.x 升级到 0.12.x，API 全面重构
- **BREAKING** 密码算法从 MD5+salt 迁移到 BCrypt（首次登录透明迁移）
- **BREAKING** 所有 API 路径统一到 `/api/v1/auth/` 前缀（原为 `/user/`）
- **BREAKING** javax.* 命名空间迁移到 jakarta.*
- 新增双 token 机制：access token（15min）+ refresh token（7d），rotation 策略
- 新增 token 黑名单（Redis SET），支持用户登出时立即失效
- 新增 refresh token 重用检测，防止 token 窃取后的持续访问
- 新增登录失败锁定机制（5 次失败锁定 15 分钟）
- Token 传输从 localStorage 迁移到 httpOnly cookie
- 权限编码统一为 `resource:action` 格式
- 权限变更通过 Spring ApplicationEvent 驱动 Redis 缓存失效

## Capabilities

### New Capabilities
- `jwt-auth`: JWT 双 token（access + refresh）的生成、验证、刷新（rotation）、黑名单、全局撤销机制
- `user-registration`: 用户注册（BCrypt 密码、参数校验、默认角色绑定）
- `user-login`: 用户登录（凭证验证、MD5→BCrypt 透明迁移、失败锁定、cookie 设置）
- `rbac-permission`: RBAC 权限控制（五表模型、权限编码规范、缓存与失效机制、方法级注解）
- `password-management`: 密码管理（修改密码、管理员重置、强度校验、全局 token 撤销）

### Modified Capabilities
<!-- 无现有 OpenSpec specs，全部为新能力 -->

## Impact

- **后端代码**：SecurityConfig、JwtComp、JwtAuthenticationTokenFilter、UserService、UserController 全部重写
- **数据库**：user 表移除 salt 列，新增 login_fail_count/lock_until 列（或存 Redis）；Flyway 迁移脚本
- **前端代码**：登录/登出流程重写（httpOnly cookie 替代 localStorage）、Axios 拦截器增加 401 自动 refresh
- **外部依赖**：新增 Redis 依赖（token 黑名单、登录失败计数、权限缓存、refresh token 注册）
- **API 兼容性**：所有端点路径变更，前端必须同步更新
- **数据迁移**：现有用户密码需在首次登录时透明迁移（MD5→BCrypt）
