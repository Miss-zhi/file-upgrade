## ADDED Requirements

### Requirement: Quota overview page
系统 SHALL 提供配额管理页面，展示用户配额列表，支持搜索和分页。

#### Scenario: 加载配额列表
- **WHEN** 管理员导航到 `/admin/quota`
- **THEN** 系统展示用户配额表格，包含列：用户名、已用空间、总配额、可用空间、操作。数据来源为用户列表接口附带配额信息，或逐个调用 `GET /api/v1/admin/quota/{userId}`

#### Scenario: 搜索用户配额
- **WHEN** 管理员输入用户名关键词搜索
- **THEN** 系统按关键词过滤用户列表并刷新配额表格

### Requirement: Edit single user quota
系统 SHALL 允许管理员修改单个用户的存储配额。

#### Scenario: 修改用户配额
- **WHEN** 管理员点击用户行的"修改配额"按钮，输入新配额值（MB）并确认
- **THEN** 系统调用 `PUT /api/v1/admin/quota/{userId}`，成功后刷新列表

#### Scenario: 配额值校验
- **WHEN** 管理员输入 ≤ 0 的值
- **THEN** 表单校验阻止提交，提示"配额必须大于 0"

### Requirement: Batch quota update
系统 SHALL 支持批量设置多个用户的存储配额。

#### Scenario: 批量修改配额
- **WHEN** 管理员勾选多个用户，点击"批量设置配额"，输入统一配额值并确认
- **THEN** 系统调用 `PUT /api/v1/admin/quota/batch`，请求体为 `[{ userId, totalQuota }, ...]`，成功后展示结果（含被跳过的不存在用户），刷新列表

#### Scenario: 批量操作无选择
- **WHEN** 管理员未勾选任何用户就点击"批量设置配额"
- **THEN** 提示"请至少选择一个用户"

### Requirement: Permission-gated access
配额管理页面 SHALL 要求当前用户持有 `admin:quota-manage` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:quota-manage` 权限
- **THEN** 侧边栏不显示"配额管理"菜单项，直接访问 `/admin/quota` 路由展示 403 页面
