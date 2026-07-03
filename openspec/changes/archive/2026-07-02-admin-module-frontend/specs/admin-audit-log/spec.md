## ADDED Requirements

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

### Requirement: Permission-gated access
审计日志页面 SHALL 要求当前用户持有 `admin:log-view` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:log-view` 权限
- **THEN** 侧边栏不显示"审计日志"菜单项，直接访问 `/admin/logs` 路由展示 403 页面
