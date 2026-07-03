## ADDED Requirements

### Requirement: AuditLog 自定义注解
系统 SHALL 提供 `@AuditLog` 注解，标注在 Controller 方法上，声明操作类型和操作描述。注解属性包括：`module`（模块名）、`action`（操作类型，如 CREATE/UPDATE/DELETE）、`description`（操作描述）。

#### Scenario: 注解标注在 Controller 方法上
- **WHEN** Controller 方法标注 `@AuditLog(module = "user", action = "UPDATE", description = "禁用用户")`
- **THEN** 方法执行后 AOP 切面自动记录操作日志

### Requirement: AOP 审计切面
系统 SHALL 实现 `AuditLogAspect` 切面，拦截所有标注 `@AuditLog` 的方法。方法成功执行后异步记录日志到 `operation_log` 表，记录内容包括：操作者 userId、模块名、操作类型、操作描述、请求方法、请求 URI、请求参数（脱敏）、响应状态码、客户端 IP、User-Agent、执行耗时。

#### Scenario: 方法执行成功记录日志
- **WHEN** 标注 `@AuditLog` 的方法成功执行
- **THEN** 切面异步写入 operation_log 表，记录操作者、模块、操作类型、描述、请求信息、响应状态码、IP、耗时

#### Scenario: 方法执行抛异常时记录失败日志
- **WHEN** 标注 `@AuditLog` 的方法抛出异常
- **THEN** 切面记录 operation_log，响应状态码为实际异常状态码，error_message 字段记录异常信息

#### Scenario: 请求参数脱敏
- **WHEN** 请求参数包含 password 字段
- **THEN** 日志中 password 字段值为 `******`

### Requirement: operation_log 表结构
系统 SHALL 维护 `operation_log` 表，字段包括：id（自增主键）、user_id（操作者）、username（操作者用户名）、module（模块名）、action（操作类型）、description（操作描述）、request_method（HTTP 方法）、request_uri（请求 URI）、request_params（请求参数 JSON）、response_code（响应状态码）、error_message（异常信息，可为空）、ip_address（客户端 IP）、user_agent（UA）、execution_time（执行耗时 ms）、create_time（记录时间）。

#### Scenario: 日志记录完整
- **WHEN** AOP 切面记录一条操作日志
- **THEN** operation_log 表新增一行，所有必填字段均有值

### Requirement: 管理员查询操作日志
系统 SHALL 提供分页查询操作日志的管理员端点。支持按模块名、操作类型、操作者用户名、时间范围过滤。

#### Scenario: 分页查询所有日志
- **WHEN** 管理员请求 `GET /api/v1/admin/logs?page=1&pageSize=20`
- **THEN** 系统返回分页操作日志列表，按 create_time 降序排列

#### Scenario: 按模块过滤
- **WHEN** 管理员请求 `GET /api/v1/admin/logs?module=user&page=1&pageSize=20`
- **THEN** 系统仅返回 module = "user" 的操作日志

#### Scenario: 按操作类型过滤
- **WHEN** 管理员请求 `GET /api/v1/admin/logs?action=DELETE&page=1&pageSize=20`
- **THEN** 系统仅返回 action = "DELETE" 的操作日志

#### Scenario: 按时间范围查询
- **WHEN** 管理员请求 `GET /api/v1/admin/logs?startTime=2026-06-01T00:00:00&endTime=2026-06-30T23:59:59`
- **THEN** 系统返回该时间范围内的操作日志

#### Scenario: 按操作者过滤
- **WHEN** 管理员请求 `GET /api/v1/admin/logs?username=admin`
- **THEN** 系统返回该用户执行的操作日志

### Requirement: 审计日志端点权限控制
操作日志查询端点 MUST 受 `@PreAuthorize("hasAuthority('admin:log-view')")` 注解保护。

#### Scenario: 有权限的管理员可查看日志
- **WHEN** 拥有 `admin:log-view` 权限的管理员请求日志查询端点
- **THEN** 请求正常处理

#### Scenario: 无权限被拒绝
- **WHEN** 不拥有 `admin:log-view` 权限的用户请求日志查询端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`
