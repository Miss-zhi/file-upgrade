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

## ADDED Requirements (Frontend)

### Requirement: Audit log list with multi-condition filter
系统 SHALL 提供审计日志查看页面，支持按模块、操作类型、用户名、时间范围筛选，分页展示日志列表，按时间倒序排列。

#### Scenario: 加载日志列表
- **WHEN** 管理员导航到 `/admin/logs`
- **THEN** 系统调用 `GET /api/v1/admin/logs`（page=1, pageSize=20），展示日志表格，包含列：操作人、模块、操作类型、描述、请求方法、请求路径、响应码、IP 地址、耗时(ms)、操作时间

#### Scenario: 按模块筛选
- **WHEN** 管理员选择模块下拉框（如 auth、file、admin）
- **THEN** 系统调用 `GET /api/v1/admin/logs?module=xxx`，刷新表格

#### Scenario: 按操作类型筛选
- **WHEN** 管理员选择操作类型（CREATE/UPDATE/DELETE）
- **THEN** 系统调用 `GET /api/v1/admin/logs?action=xxx`，刷新表格

#### Scenario: 按用户名筛选
- **WHEN** 管理员输入用户名关键词
- **THEN** 系统调用 `GET /api/v1/admin/logs?username=xxx`，刷新表格

#### Scenario: 按时间范围筛选
- **WHEN** 管理员选择日期范围（startTime, endTime，ISO-8601 格式）
- **THEN** 系统调用 `GET /api/v1/admin/logs?startTime=xxx&endTime=xxx`，刷新表格

#### Scenario: 组合筛选
- **WHEN** 管理员同时设置多个筛选条件
- **THEN** 系统将所有条件作为查询参数一并发送，刷新表格

#### Scenario: 翻页
- **WHEN** 管理员切换页码或修改每页条数
- **THEN** 系统以新参数重新请求日志列表并刷新表格

### Requirement: Log detail view
系统 SHALL 允许管理员查看单条日志的详细信息。

#### Scenario: 查看日志详情
- **WHEN** 管理员点击日志行的"详情"按钮
- **THEN** 弹出详情对话框，展示完整信息：请求参数（password 字段已脱敏为 `******`）、错误信息（如有）、User-Agent 等

### Requirement: Permission-gated access (Frontend)
审计日志页面 SHALL 要求当前用户持有 `admin:log-view` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:log-view` 权限
- **THEN** 侧边栏不显示"审计日志"菜单项，直接访问 `/admin/logs` 路由展示 404 页面
