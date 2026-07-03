## ADDED Requirements

### Requirement: 系统参数表结构
系统 SHALL 维护 `system_config` 表，字段包括：id（自增主键）、config_key（参数键名，唯一）、config_value（参数值）、description（参数描述）、create_time（创建时间）、update_time（更新时间）。config_key MUST 唯一约束。

#### Scenario: 表结构完整
- **WHEN** 创建系统参数记录
- **THEN** system_config 表包含 config_key（UNIQUE）、config_value、description、create_time、update_time 字段

### Requirement: 系统参数查询
系统 SHALL 提供分页查询系统参数列表的端点，支持按 key 名称模糊搜索。

#### Scenario: 分页查询所有参数
- **WHEN** 管理员请求 `GET /api/v1/admin/config?page=1&pageSize=20`
- **THEN** 系统返回分页系统参数列表

#### Scenario: 按 key 搜索
- **WHEN** 管理员请求 `GET /api/v1/admin/config?keyword=quota`
- **THEN** 系统返回 config_key 或 description 包含 "quota" 的参数列表

### Requirement: 系统参数新增
系统 SHALL 提供新增系统参数的端点。config_key 不能与已有记录重复。

#### Scenario: 新增参数成功
- **WHEN** 管理员请求 `POST /api/v1/admin/config`，请求体包含 configKey、configValue、description
- **THEN** 系统插入新记录，返回 200

#### Scenario: config_key 重复
- **WHEN** 管理员请求新增已存在的 configKey
- **THEN** 系统返回 400，错误码 `CONFIG_KEY_DUPLICATE`

### Requirement: 系统参数修改
系统 SHALL 提供修改系统参数的端点。更新后 MUST 删除对应 Redis 缓存。

#### Scenario: 修改参数成功
- **WHEN** 管理员请求 `PUT /api/v1/admin/config/{id}`，请求体包含 configValue 和/或 description
- **THEN** 系统更新记录，删除 Redis 缓存 key `sys:config:{configKey}`，返回 200

#### Scenario: 参数不存在
- **WHEN** 管理员请求修改不存在的 id
- **THEN** 系统返回 404，错误码 `CONFIG_NOT_FOUND`

### Requirement: 系统参数删除
系统 SHALL 提供删除系统参数的端点。删除后 MUST 删除对应 Redis 缓存。

#### Scenario: 删除参数成功
- **WHEN** 管理员请求 `DELETE /api/v1/admin/config/{id}`
- **THEN** 系统删除记录，删除 Redis 缓存 key `sys:config:{configKey}`，返回 200

#### Scenario: 删除不存在的参数
- **WHEN** 管理员请求删除不存在的 id
- **THEN** 系统返回 404，错误码 `CONFIG_NOT_FOUND`

### Requirement: 系统参数缓存读取
系统 SHALL 提供根据 key 获取参数值的服务方法，优先从 Redis 读取（key: `sys:config:{configKey}`，TTL 10 分钟），miss 时查 DB 并回填缓存。

#### Scenario: 从缓存读取参数
- **WHEN** 应用代码调用 SystemConfigService.getConfigValue(key)，Redis 中存在缓存
- **THEN** 系统从 Redis 读取值，不查询数据库

#### Scenario: 缓存 miss 从 DB 加载
- **WHEN** Redis 中无对应缓存
- **THEN** 系统查询 DB，将值写入 Redis（TTL 10 分钟），返回值

#### Scenario: 参数不存在
- **WHEN** 查询不存在的 configKey
- **THEN** 系统返回 null

### Requirement: 系统配置端点权限控制
系统配置管理端点 MUST 受 `@PreAuthorize("hasAuthority('admin:config-manage')")` 注解保护。

#### Scenario: 有权限的管理员可访问
- **WHEN** 拥有 `admin:config-manage` 权限的管理员请求配置管理端点
- **THEN** 请求正常处理

#### Scenario: 无权限被拒绝
- **WHEN** 不拥有 `admin:config-manage` 权限的用户请求配置管理端点
- **THEN** 返回 403，错误码 `ACCESS_DENIED`

## ADDED Requirements (Frontend)

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

### Requirement: Permission-gated access (Frontend)
系统配置管理页面 SHALL 要求当前用户持有 `admin:config-manage` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:config-manage` 权限
- **THEN** 侧边栏不显示"系统配置"菜单项，直接访问 `/admin/config` 路由展示 404 页面
