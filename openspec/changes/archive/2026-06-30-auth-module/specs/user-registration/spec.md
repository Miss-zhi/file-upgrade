## ADDED Requirements

### Requirement: 用户注册端点
系统 SHALL 提供 `POST /api/v1/auth/register` 端点，接受用户名、手机号和密码进行注册。请求体 MUST 使用 jakarta.validation 注解校验：`username`（`@NotBlank @Size(max=50)`）、`telephone`（`@NotBlank @Pattern(regexp="^1[3-9]\\d{9}$")`）、`password`（`@NotBlank @Size(min=8,max=30) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")`）。

**密码校验策略**：`@Size(min=8,max=30)` 负责长度约束，`@Pattern` 负责字符复杂度约束（必须包含至少一个大写字母、一个小写字母和一个数字）。统一校验规则的正则表达式为 `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,30}$`（含长度），在 Service 层统一执行，所有密码端点（注册、修改密码、重置密码）共用同一校验逻辑。

#### Scenario: 注册成功
- **WHEN** 提交合法的用户名、手机号和密码，且用户名和手机号均未被注册
- **THEN** 返回 201 Created，响应体包含 `{"code": 0, "message": "注册成功", "data": {"userId": "<snowflake_id>"}}`

#### Scenario: 用户名已存在
- **WHEN** 提交的用户名已被其他用户注册
- **THEN** 返回 400，错误码 `USERNAME_EXISTS`，消息"用户名已存在"

#### Scenario: 手机号已注册
- **WHEN** 提交的手机号已被其他用户注册
- **THEN** 返回 400，错误码 `TELEPHONE_EXISTS`，消息"手机号已注册"

#### Scenario: 手机号格式不正确
- **WHEN** 提交的手机号不符合中国大陆手机号格式
- **THEN** 返回 400，错误码 `TELEPHONE_INVALID`，消息"手机号格式不正确"

#### Scenario: 密码强度不足
- **WHEN** 提交的密码不满足 8-30 位且包含大小写字母和数字的要求
- **THEN** 返回 400，错误码 `PASSWORD_WEAK`，消息"密码需包含大小写字母和数字，长度8-30位"

### Requirement: 注册密码 BCrypt 编码
系统 SHALL 使用 `BCryptPasswordEncoder`（strength=10）对用户密码进行 hash 存储。禁止使用 MD5 或明文存储。

#### Scenario: 密码以 BCrypt hash 存储
- **WHEN** 用户注册成功
- **THEN** 数据库中 user.password 字段为 BCrypt hash 格式（以 `$2a$10$` 开头）

### Requirement: 注册时生成 Snowflake userId
系统 SHALL 为每个新注册用户生成唯一的 Snowflake 字符串 ID，存储在 `user.user_id` 字段中。

#### Scenario: 每个注册用户获得唯一 userId
- **WHEN** 两个用户几乎同时注册
- **THEN** 两个用户的 `user_id` 值 MUST 不同且为有效的 Snowflake ID 格式

### Requirement: 注册时绑定默认角色
系统 SHALL 在用户注册成功后自动为其绑定普通用户角色（roleId=2）。

#### Scenario: 新用户自动绑定普通用户角色
- **WHEN** 用户注册成功
- **THEN** user_role 表中存在该用户的记录，role_id = 2
