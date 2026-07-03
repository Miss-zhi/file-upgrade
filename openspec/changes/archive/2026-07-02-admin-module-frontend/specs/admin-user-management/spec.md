## ADDED Requirements

### Requirement: User list page with search and pagination
系统 SHALL 提供用户管理页面，展示分页用户列表，支持按用户名关键词搜索和按状态（启用/禁用）筛选。

#### Scenario: 加载用户列表
- **WHEN** 管理员导航到 `/admin/users`
- **THEN** 系统调用 `GET /api/v1/admin/users`（page=1, pageSize=20），展示用户表格，包含列：用户名、手机号、邮箱、存储用量（进度条）、注册时间、状态（标签）、操作

#### Scenario: 按用户名搜索
- **WHEN** 管理员在搜索框输入关键词并按回车或点击搜索按钮
- **THEN** 系统调用 `GET /api/v1/admin/users?keyword=xxx`，刷新表格

#### Scenario: 翻页
- **WHEN** 管理员点击分页控件切换页码或修改每页条数
- **THEN** 系统以新参数重新请求用户列表并刷新表格

#### Scenario: 空结果
- **WHEN** 搜索无匹配结果
- **THEN** 表格展示"暂无数据"空状态

### Requirement: View user detail with roles and permissions
系统 SHALL 提供用户详情对话框，展示用户基本信息及其关联的角色和权限列表。

#### Scenario: 打开用户详情
- **WHEN** 管理员点击用户行的"详情"按钮
- **THEN** 系统调用 `GET /api/v1/admin/users/{userId}`，弹出详情对话框，展示：用户ID、用户名、手机号、邮箱、注册时间、存储用量/配额（进度条）、角色列表、权限码列表

#### Scenario: 关闭详情
- **WHEN** 管理员点击"关闭"按钮
- **THEN** 对话框关闭

### Requirement: Edit user storage quota
系统 SHALL 提供修改配额对话框，允许管理员修改单个用户的存储配额。

#### Scenario: 打开修改配额
- **WHEN** 管理员点击用户行的"修改配额"按钮
- **THEN** 弹出配额编辑对话框，预填当前配额值（MB），调用 `GET /api/v1/admin/quota/{userId}` 获取当前配额

#### Scenario: 提交配额修改
- **WHEN** 管理员输入新配额值并点击确定
- **THEN** 系统调用 `PUT /api/v1/admin/quota/{userId}`，成功后提示"修改成功"并刷新用户列表

#### Scenario: 配额值校验
- **WHEN** 管理员输入 ≤ 0 的值
- **THEN** 表单校验阻止提交，提示"配额必须大于 0"

### Requirement: Reset user password
系统 SHALL 提供重置密码对话框，允许管理员为用户设置新密码。

#### Scenario: 打开重置密码
- **WHEN** 管理员点击用户行的"重置密码"按钮
- **THEN** 弹出重置密码对话框，密码字段默认填入 `123456`

#### Scenario: 提交密码重置
- **WHEN** 管理员修改密码并点击确定
- **THEN** 系统调用 `PUT /api/v1/admin/users/{userId}/password`，成功后提示"重置密码成功"

#### Scenario: 密码格式校验
- **WHEN** 管理员输入不符合规则的密码（长度不在 6-20 之间或包含中文）
- **THEN** 表单校验阻止提交，显示对应错误提示

### Requirement: Toggle user enable/disable
系统 SHALL 允许管理员启用或禁用用户账户。

#### Scenario: 禁用用户
- **WHEN** 管理员点击启用状态用户的"禁用"按钮
- **THEN** 系统弹出确认对话框，确认后调用 `PUT /api/v1/admin/users/{userId}/disable`，成功后刷新列表

#### Scenario: 启用用户
- **WHEN** 管理员点击禁用状态用户的"启用"按钮
- **THEN** 系统弹出确认对话框，确认后调用 `PUT /api/v1/admin/users/{userId}/enable`，成功后刷新列表

#### Scenario: 禁止禁用自身
- **WHEN** 管理员尝试禁用自身账户
- **THEN** 后端返回 400 `CANNOT_DISABLE_SELF`，前端展示错误提示

### Requirement: Permission-gated access
用户管理页面的所有操作 SHALL 要求当前用户持有 `admin:user-manage` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:user-manage` 权限
- **THEN** 侧边栏不显示"用户管理"菜单项，直接访问 `/admin/users` 路由展示 403 页面
