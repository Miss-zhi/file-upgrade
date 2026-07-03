## ADDED Requirements

### Requirement: 用户登录端点
系统 SHALL 提供 `POST /api/v1/auth/login` 端点，接受手机号和密码进行登录。请求体 MUST 包含 `telephone`（`@NotBlank`）和 `password`（`@NotBlank`）。

#### Scenario: 登录成功
- **WHEN** 用户提交正确的手机号和密码
- **THEN** 返回 200 OK，响应体包含 `userId`、`username`、`roles`、`permissions`，并通过 Set-Cookie 设置 access_token 和 refresh_token

#### Scenario: 手机号或密码错误
- **WHEN** 用户提交错误的手机号或密码
- **THEN** 返回 401，错误码 `AUTH_INVALID_CREDENTIALS`，消息"手机号或密码错误"（不区分"用户不存在"和"密码错误"，防枚举）

#### Scenario: 账户已被禁用
- **WHEN** 用户凭证正确但 user.available = 0
- **THEN** 返回 403，错误码 `AUTH_USER_DISABLED`，消息"账户已被禁用"

#### Scenario: 账户被锁定
- **WHEN** 用户连续登录失败 ≥ 5 次后再次尝试登录
- **THEN** 返回 423，错误码 `AUTH_ACCOUNT_LOCKED`，消息"账户已锁定，请15分钟后重试"

### Requirement: 登录失败计数与锁定
系统 SHALL 使用 Redis 记录登录失败次数（key: `login:fail:{telephone}`，TTL: 15 分钟）。每次失败计数 +1（用户不存在时也计数，防枚举）。连续失败 ≥ 5 次时返回锁定错误。登录成功后 MUST 清除失败计数。

#### Scenario: 连续失败 5 次后账户锁定
- **WHEN** 同一手机号连续登录失败 5 次
- **THEN** 第 5 次返回 423 `AUTH_ACCOUNT_LOCKED`，Redis 中 `login:fail:{telephone}` 值为 5

#### Scenario: 登录成功后清除失败计数
- **WHEN** 用户之前有失败记录，本次登录成功
- **THEN** Redis 中 `login:fail:{telephone}` key 被删除

#### Scenario: 锁定 15 分钟后自动解锁
- **WHEN** 账户被锁定后等待 15 分钟
- **THEN** Redis key 因 TTL 过期自动删除，用户可再次尝试登录

### Requirement: MD5 到 BCrypt 透明迁移
系统 SHALL 在登录时支持旧 MD5 密码的透明迁移。验证顺序：先尝试 BCrypt matches，若不匹配则尝试旧 MD5 hash（使用 `HashUtils.hashHex("MD5", password, salt, iterations)`）。若 MD5 匹配成功，系统 MUST 用 BCrypt 重新 hash 密码并更新 user.password，清空 salt 字段。

#### Scenario: 旧用户首次登录自动迁移
- **WHEN** 使用旧 MD5 密码的用户首次登录
- **THEN** 登录成功，user.password 被更新为 BCrypt hash，salt 字段被清空

#### Scenario: 已迁移用户后续登录只走 BCrypt
- **WHEN** 已完成迁移的用户再次登录
- **THEN** BCrypt matches 直接成功，不触发 MD5 验证路径

### Requirement: 登录成功生成双 Token
登录成功后系统 MUST 生成 access token（15 分钟）和 refresh token（7 天），通过 httpOnly cookie 发送，并在响应体中返回用户信息。Refresh token 的 jti MUST 注册到 Redis。

#### Scenario: 登录成功返回双 token 和用户信息
- **WHEN** 用户登录成功
- **THEN** 响应 Set-Cookie 包含 access_token 和 refresh_token，响应体包含 userId、username、roles、permissions

### Requirement: 登录端点为公开端点
`POST /api/v1/auth/login` MUST 在 SecurityFilterChain 中配置为 permitAll，无需认证即可访问。

#### Scenario: 未认证用户可访问登录端点
- **WHEN** 未携带任何 token 的请求访问 `/api/v1/auth/login`
- **THEN** 请求正常到达 Controller 处理，不被 Security Filter 拦截
