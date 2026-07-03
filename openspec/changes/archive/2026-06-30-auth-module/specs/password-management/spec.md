## ADDED Requirements

### Requirement: 用户修改密码端点
系统 SHALL 提供 `PUT /api/v1/auth/password` 端点，允许已认证用户修改密码。请求体 MUST 包含 `oldPassword`（`@NotBlank`）和 `newPassword`（`@NotBlank @Size(min=8,max=30) @Pattern`）。

#### Scenario: 修改密码成功
- **WHEN** 用户提交正确的旧密码和符合强度要求的新密码，且新旧密码不同
- **THEN** 返回 200 OK，消息"密码修改成功，请重新登录"，user.password 更新为 BCrypt hash

#### Scenario: 旧密码错误
- **WHEN** 用户提交的旧密码与当前密码不匹配
- **THEN** 返回 401，错误码 `AUTH_OLD_PASSWORD_WRONG`，消息"原密码错误"

#### Scenario: 新密码与旧密码相同
- **WHEN** 用户提交的新密码与旧密码相同
- **THEN** 返回 400，错误码 `PASSWORD_SAME`，消息"新密码不能与旧密码相同"

#### Scenario: 新密码强度不足
- **WHEN** 用户提交的新密码不满足强度要求
- **THEN** 返回 400，错误码 `PASSWORD_WEAK`，消息"密码需包含大小写字母和数字，长度8-30位"

### Requirement: 修改密码后全局撤销 Token
修改密码成功后系统 MUST 使该用户所有已签发的 token 立即失效。通过在 Redis 中记录 `revoke:all:{userId}` = 当前 Unix 时间戳实现。

#### Scenario: 修改密码后旧 token 立即失效
- **WHEN** 用户修改密码成功
- **THEN** Redis 中写入 `revoke:all:{userId}` = 当前时间戳，所有 `iat` 早于该时间戳的 token 被拒绝

#### Scenario: 修改密码后需重新登录
- **WHEN** 用户修改密码后使用旧 cookie 访问受保护端点
- **THEN** 返回 401，前端引导用户重新登录

### Requirement: 管理员重置密码端点
系统 SHALL 提供 `PUT /api/v1/admin/users/{userId}/password` 端点，允许管理员重置指定用户的密码。请求体 MUST 包含 `newPassword`（`@NotBlank @Size @Pattern`）。操作者 MUST 拥有 `admin:user-manage` 权限（`@PreAuthorize`）。

#### Scenario: 管理员重置密码成功
- **WHEN** 拥有 `admin:user-manage` 权限的管理员提交合法的新密码
- **THEN** 返回 200 OK，消息"密码已重置"，目标用户密码更新，目标用户所有 token 失效

#### Scenario: 目标用户不存在
- **WHEN** 管理员指定的 userId 在数据库中不存在
- **THEN** 返回 404，错误码 `USER_NOT_FOUND`，消息"用户不存在"

#### Scenario: 无权限用户尝试重置
- **WHEN** 不拥有 `admin:user-manage` 权限的用户调用该端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`

### Requirement: 管理员重置密码记录操作日志
管理员重置密码操作 MUST 记录审计日志，包含操作人 userId、目标用户 userId、操作时间。

#### Scenario: 重置密码操作被记录
- **WHEN** 管理员成功重置用户密码
- **THEN** 系统记录操作日志，包含操作人、目标用户、时间戳

### Requirement: 密码强度校验规则
所有密码相关端点（注册、修改密码、重置密码）MUST 使用统一的密码强度校验规则：最少 8 位，最多 30 位，必须包含大写字母、小写字母和数字。正则：`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,30}$`。

#### Scenario: 符合强度要求的密码通过校验
- **WHEN** 提交密码 `Abc12345`
- **THEN** 校验通过

#### Scenario: 缺少大写字母的密码被拒绝
- **WHEN** 提交密码 `abc12345`
- **THEN** 校验失败，返回 `PASSWORD_WEAK`

#### Scenario: 缺少数字的密码被拒绝
- **WHEN** 提交密码 `Abcdefgh`
- **THEN** 校验失败，返回 `PASSWORD_WEAK`

#### Scenario: 超长密码被拒绝
- **WHEN** 提交密码超过 30 位
- **THEN** 校验失败，返回 `PASSWORD_WEAK`
