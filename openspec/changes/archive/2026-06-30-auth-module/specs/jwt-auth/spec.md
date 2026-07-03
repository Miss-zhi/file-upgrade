## ADDED Requirements

### Requirement: JWT access token 生成
系统 SHALL 使用 jjwt 0.12.x 生成 HS256 签名的 JWT access token。Token payload MUST 包含 `sub`（userId）、`type`（固定为 `"access"`）、`jti`（UUID）、`roles`（角色列表）、`iat`（签发时间戳，秒）、`exp`（过期时间戳，秒）。签名密钥从环境变量 `JWT_SECRET` 读取，Base64 解码后 MUST ≥ 32 字节。有效期 MUST 为 900 秒（15 分钟），可通过配置调整。

#### Scenario: 成功生成 access token
- **WHEN** 用户登录成功，系统为其生成 access token
- **THEN** 返回的 JWT 解析后 payload 包含正确的 sub、type="access"、jti、roles、iat、exp=iat+900

#### Scenario: 密钥长度不足时启动失败
- **WHEN** 环境变量 `JWT_SECRET` Base64 解码后长度 < 32 字节
- **THEN** 应用启动时 MUST 抛出 `IllegalStateException` 并阻止启动

### Requirement: JWT refresh token 生成
系统 SHALL 生成 refresh token，payload 包含 `sub`（userId）、`type`（固定为 `"refresh"`）、`jti`（UUID）、`iat`、`exp`。有效期 MUST 为 604800 秒（7 天）。生成后 MUST 将 jti 注册到 Redis（key: `refresh:{jti}`，value: userId，TTL: 7 天）。

#### Scenario: 成功生成并注册 refresh token
- **WHEN** 用户登录成功，系统为其生成 refresh token
- **THEN** JWT 解析后 payload 包含 type="refresh" 和唯一 jti，且 Redis 中存在 key `refresh:{jti}` 值为 userId

### Requirement: JWT 签名与过期验证
系统 SHALL 在每次请求时验证 access token 的签名和过期时间。签名验证使用 `Jwts.parser().verifyWith(key).build()` 新 API。时钟偏移容忍度 MUST 为可配置值，默认 30 秒。

#### Scenario: 有效 token 通过验证
- **WHEN** 请求携带签名有效且未过期的 access token
- **THEN** 系统解析 JWT 成功并提取 claims

#### Scenario: 签名无效的 token 被拒绝
- **WHEN** 请求携带签名被篡改的 access token
- **THEN** JWT 解析失败，系统不设置 SecurityContext，请求由后续 AuthorizationFilter 处理

#### Scenario: 过期 token 被拒绝
- **WHEN** 请求携带已过期的 access token
- **THEN** JWT 解析失败（ExpiredJwtException），系统不设置 SecurityContext

### Requirement: Token 黑名单机制
系统 SHALL 通过 Redis SET 实现 token 黑名单。登出时将 access token 和 refresh token 的 jti 加入黑名单（key: `blacklist:{jti}`，value: `"1"`）。TTL MUST 等于 token 剩余有效期（`exp` - 当前时间），不浪费 Redis 内存。JwtAuthenticationFilter MUST 在每次请求时检查黑名单。

#### Scenario: 登出后 token 被加入黑名单
- **WHEN** 用户登出，其 access token 的 jti 被加入 Redis 黑名单
- **THEN** 使用该 token 访问任何受保护端点返回 401

#### Scenario: 黑名单 key 自动过期
- **WHEN** token 的剩余有效期到期
- **THEN** Redis 中对应的 `blacklist:{jti}` key 自动删除，不占用内存

### Requirement: 全局 Token 撤销
系统 SHALL 支持通过时间戳机制全局撤销某用户的所有 token。在 Redis 中记录 `revoke:all:{userId}` = 当前 Unix 时间戳（秒），TTL 7 天。JwtAuthenticationFilter MUST 比较 token 的 `iat` 与撤销时间戳，若 `iat` < 撤销时间则视为无效。

#### Scenario: 修改密码后所有旧 token 失效
- **WHEN** 用户修改密码，系统记录 `revoke:all:{userId}` = 当前时间戳
- **THEN** 所有 `iat` 早于该时间戳的 token 在后续请求中被拒绝

#### Scenario: 撤销记录自动过期
- **WHEN** 撤销时间戳记录后 7 天
- **THEN** Redis 中 `revoke:all:{userId}` key 自动删除

### Requirement: Refresh token rotation 与重用检测
系统 SHALL 实现 refresh token rotation：每次刷新时消费旧 refresh token 的 jti（从 Redis 删除），签发新的 refresh token（新 jti）并注册到 Redis。当检测到旧 jti 被重用时（Redis 中不存在该 jti），系统 MUST 触发重用检测：删除该用户所有 refresh token（通过 Redis 通配符匹配 userId），返回 401 `AUTH_TOKEN_REVOKED`。

#### Scenario: 正常刷新触发 rotation
- **WHEN** 客户端发送有效的 refresh token 请求刷新
- **THEN** 旧 jti 从 Redis 删除，新 refresh token（新 jti）注册到 Redis，返回新 token pair

#### Scenario: 重用已消费的 refresh token
- **WHEN** 客户端发送已被消费的旧 refresh token（jti 不在 Redis 中）
- **THEN** 系统检测到重用，删除该用户所有 refresh token，返回 401 `AUTH_TOKEN_REVOKED`

### Requirement: Token 传输通过 httpOnly Cookie
系统 SHALL 通过 httpOnly cookie 传输 token。Access token cookie：`Path=/`，`HttpOnly`，`Secure`（生产环境），`SameSite=Lax`，`Max-Age=900`。Refresh token cookie：`Path=/api/v1/auth/refresh`，`HttpOnly`，`Secure`（生产环境），`SameSite=Lax`，`Max-Age=604800`。系统 MUST 同时支持从 `Authorization: Bearer <token>` header 读取 access token，兼容 API 调用场景。

#### Scenario: 登录成功后 cookie 自动设置
- **WHEN** 用户登录成功
- **THEN** 响应包含 `Set-Cookie: access_token=<jwt>; Path=/; HttpOnly; SameSite=Lax; Max-Age=900` 和 `Set-Cookie: refresh_token=<jwt>; Path=/api/v1/auth/refresh; HttpOnly; SameSite=Lax; Max-Age=604800`

#### Scenario: 通过 Authorization header 读取 token
- **WHEN** API 客户端在请求头中发送 `Authorization: Bearer <access_token>`
- **THEN** 系统从 header 提取 token 并正常验证
