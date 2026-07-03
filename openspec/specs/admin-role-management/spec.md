## ADDED Requirements

### Requirement: Role list display
系统 SHALL 提供角色管理页面，展示所有角色及其当前关联的权限。

#### Scenario: 加载角色列表
- **WHEN** 管理员导航到 `/admin/roles`
- **THEN** 系统调用 `GET /api/v1/admin/roles`，展示角色表格，包含列：角色名称、角色描述、权限数量、状态、操作

#### Scenario: 查看角色权限
- **WHEN** 管理员点击角色行的"编辑权限"按钮
- **THEN** 系统弹出权限编辑对话框，展示权限树（复选框形式），已关联的权限项为选中状态

### Requirement: Edit role permissions
系统 SHALL 允许管理员修改角色的权限集合。权限树 SHALL 支持两级结构（父权限 → 子权限），勾选父权限自动选中所有子权限。

#### Scenario: 更新角色权限
- **WHEN** 管理员在权限树中勾选/取消若干权限并点击确定
- **THEN** 系统调用 `PUT /api/v1/admin/roles/{roleId}/permissions`，成功后提示"权限更新成功"并刷新角色列表

#### Scenario: 权限树父子联动
- **WHEN** 管理员勾选一个父权限节点
- **THEN** 该节点下所有子权限自动勾选

#### Scenario: 取消子权限不影响父权限
- **WHEN** 管理员取消某个子权限
- **THEN** 父权限保持勾选状态（后端按实际提交的权限列表保存）

### Requirement: Permission-gated access
角色管理页面 SHALL 要求当前用户持有 `admin:role-manage` 权限码。

#### Scenario: 无权限访问
- **WHEN** 当前用户无 `admin:role-manage` 权限
- **THEN** 侧边栏不显示"角色管理"菜单项，直接访问 `/admin/roles` 路由展示 404 页面
