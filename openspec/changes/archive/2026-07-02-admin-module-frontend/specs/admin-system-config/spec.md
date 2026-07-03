## ADDED Requirements

### Requirement: Config list with search and pagination
系统 SHALL 提供系统配置管理页面，展示配置项列表，支持按关键词搜索（匹配 configKey 和 description），分页展示。

#### Scenario: 加载配置列表
- **WHEN** 管理员导航到 `/admin/config`
- **THEN** 系统调用 `GET /api/v1/admin/config`（page=1, pageSize=20），展示配置表格，包含列：配置键、配置值、描述、创建时间、更新时间、操作

#### Scenario: 按关键词搜索
- **WHEN** 管理员输入关键词（如 `upload`）并搜索
- **THEN** 系统调用 `GET /api/v1/admin/config?keyword=upload`，刷新表格

#### Scenario: 翻页
- **WHEN** 管理员切换页码
- **THEN** 系统以新参数重新请求配置列表并刷新表格

### Requirement: Create config entry
系统 SHALL 允许管理员新增配置项。

#### Scenario: 新增配置
- **WHEN** 管理员点击"新增配置"按钮，填写 configKey、configValue、description 并提交
- **THEN** 系统调用 `POST /api/v1/admin/config`，成功后刷新列表

#### Scenario: configKey 重复
- **WHEN** 管理员输入的 configKey 已存在
- **THEN** 后端返回 400 `CONFIG_KEY_DUPLICATE`，前端提示"配置键已存在"

#### Scenario: 必填项校验
- **WHEN** 管理员未填写 configKey 或 configValue 就提交
- **THEN** 表单校验阻止提交

### Requirement: Update config entry
系统 SHALL 允许管理员修改配置项的值和描述。

#### Scenario: 编辑配置
- **WHEN** 管理员点击配置行的"编辑"按钮，修改 configValue 和/或 description 并提交
- **THEN** 系统调用 `PUT /api/v1/admin/config/{id}`，成功后刷新列表

### Requirement: Delete config entry
系统 SHALL 允许管理员删除配置项。

#### Scenario: 删除配置
- **WHEN** 管理员点击配置行的"删除"按钮
- **THEN** 系统弹出确认对话框，确认后调用 `DELETE /api/v1/admin/config/{id}`，成功后刷新列表

#### Scenario: 取消删除
- **WHEN** 管理员在确认对话框点击取消
- **THEN** 不执行删除操作

### Requirement: Permission-gated access
系统配置管理页面 SHALL 要求当前用户持有 `admin:config-manage` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:config-manage` 权限
- **THEN** 侧边栏不显示"系统配置"菜单项，直接访问 `/admin/config` 路由展示 403 页面
