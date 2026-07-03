## ADDED Requirements

### Requirement: 用户登出端点
系统 SHALL 提供 `POST /api/v1/auth/logout` 端点，允许已认证用户安全终止当前会话。该端点 MUST 从 httpOnly cookie 中提取 access token 和 refresh token，分别执行黑名单操作。

#### Scenario: 正常登出
- **WHEN** 已认证用户调用登出端点，且 cookie 中存在有效的 access token 和 refresh token
- **THEN** 系统将 access token 的 jti 加入 Redis 黑名单（TTL = token 剩余有效期），消费并黑名单 refresh token 的 jti，清除 cookie（`Set-Cookie: access_token=; Max-Age=0; Path=/; HttpOnly`），返回 200 OK

#### Scenario: 无 token 时登出（幂等）
- **WHEN** 未携带任何 cookie 调用登出端点
- **THEN** 系统返回 200 OK（幂等操作，不报错）

#### Scenario: access token 已过期时登出
- **WHEN** cookie 中的 access token 已过期但 refresh token 仍有效
- **THEN** 系统跳过 access token 黑名单（已过期无需黑名单），正常消费并黑名单 refresh token，清除 cookie，返回 200 OK

#### Scenario: 登出后旧 access token 被拒绝
- **WHEN** 用户登出后使用旧 access token 访问受保护端点
- **THEN** JwtAuthenticationFilter 检测到 jti 在黑名单中，返回 401

#### Scenario: 登出后旧 refresh token 被拒绝
- **WHEN** 用户登出后使用旧 refresh token 调用刷新端点
- **THEN** 系统检测到 refresh token jti 不在 Redis 注册表中（已被消费），返回 401

### Requirement: Token 刷新端点
系统 SHALL 提供 `POST /api/v1/auth/refresh` 端点，接受 httpOnly cookie 中的 refresh token，执行 rotation 策略后签发新的 token pair。

#### Scenario: 正常刷新
- **WHEN** 用户携带有效的 refresh token cookie 调用刷新端点
- **THEN** 系统验证 refresh token 签名和有效期 → 从 Redis 消费当前 refresh token jti（GET+DELETE 原子操作）→ 生成新 access token 和新 refresh token → 在 Redis 注册新 refresh token jti → 设置新 cookie → 返回 200 OK 及用户基本信息

#### Scenario: refresh token 过期
- **WHEN** cookie 中的 refresh token 已超过 7 天有效期
- **THEN** 返回 401，错误码 `REFRESH_TOKEN_EXPIRED`，消息"登录已过期，请重新登录"

#### Scenario: refresh token 重用检测
- **WHEN** 客户端使用已被消费的旧 refresh token 调用刷新端点（jti 在 Redis 中不存在）
- **THEN** 系统判定为 token 窃取，执行 `revokeAllRefreshTokens(userId)` 撤销该用户所有 refresh token，同时 `revokeAllTokens(userId)` 撤销所有 access token，返回 401，错误码 `TOKEN_REUSE_DETECTED`

#### Scenario: refresh token 签名无效
- **WHEN** cookie 中的 refresh token 签名验证失败（被篡改或密钥不匹配）
- **THEN** 返回 401，错误码 `INVALID_TOKEN`

#### Scenario: 并发刷新请求防重入
- **WHEN** 前端同时发起多个 refresh 请求（网络重试场景）
- **THEN** 第一个请求正常完成 rotation，后续请求因旧 jti 已被消费触发重用检测。前端 Axios 拦截器 MUST 保证同一时间只发起一个 refresh 请求（请求队列机制），避免误触发

### Requirement: 获取当前用户信息端点
系统 SHALL 提供 `GET /api/v1/auth/me` 端点，返回当前已认证用户的完整信息，包含角色和权限列表。

#### Scenario: 正常获取用户信息
- **WHEN** 已认证用户调用该端点
- **THEN** 系统从 SecurityContext 提取用户标识 → 查询 user 表 → 查询可用角色（available=1）→ 查询权限列表（优先从 Redis 缓存 `user:perms:{userId}` 读取）→ 返回 200 OK 及 UserInfoResponse

#### Scenario: UserInfoResponse 结构
- **WHEN** 返回用户信息
- **THEN** 响应体 MUST 包含以下字段：
  ```json
  {
    "code": 0,
    "data": {
      "userId": "<snowflake_id>",
      "username": "string",
      "telephone": "138****1234",
      "roles": ["USER"],
      "permissions": ["file:upload", "file:download", "file:delete"],
      "avatar": "string|null",
      "registerTime": "ISO-8601"
    }
  }
  ```
  手机号 MUST 脱敏（中间四位替换为 `****`）

#### Scenario: token 被黑名单后访问 /me
- **WHEN** 用户的 access token jti 已在黑名单中（已登出或密码已变更）
- **THEN** JwtAuthenticationFilter 拒绝请求，返回 401，前端引导用户重新登录

#### Scenario: 用户角色被禁用
- **WHEN** 用户的所有角色 available 字段均为 0
- **THEN** 返回 200 OK，roles 和 permissions 均为空数组 `[]`，前端根据空权限列表展示相应 UI

### Requirement: 登出 Cookie 清除规范
登出成功时系统 MUST 清除所有认证相关 cookie。`Set-Cookie` 响应头 MUST 设置 `access_token=; refresh_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax`。

#### Scenario: 登出后 cookie 被清除
- **WHEN** 登出响应返回
- **THEN** 浏览器中 `access_token` 和 `refresh_token` cookie 被删除（Max-Age=0）

### Requirement: 刷新端点公开访问配置
`POST /api/v1/auth/refresh` MUST 在 SecurityFilterChain 中配置为 `permitAll()`，因为调用该端点时用户的 access token 可能已过期，无法通过 JwtAuthenticationFilter 认证。

#### Scenario: 无 access token 时可调用 refresh
- **WHEN** 用户 access token 已过期，仅持有 refresh token cookie
- **THEN** 请求通过 SecurityFilterChain 的 permitAll 检查，到达 refresh 端点处理
