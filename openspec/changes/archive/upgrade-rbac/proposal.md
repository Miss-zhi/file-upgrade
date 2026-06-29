# 升级完整 RBAC 权限体系

## Why

当前只有 `User.role` 简单枚举（admin/user），无法支持细粒度权限控制。原项目有完整的 Role/Permission/UserRole/RolePermission 四表关联模型。

## What Changes

### 后端
1. **Role Entity**：roleId/roleName/description/available
2. **Permission Entity**：permissionId/parentId/permissionName/resourceType/permissionCode/orderNum（树形）
3. **UserRole Entity**：userId ↔ roleId
4. **RolePermission Entity**：roleId ↔ permissionId
5. **Mapper × 4**
6. **RoleService + PermissionService**：CRUD + 树形查询 + 分配逻辑
7. **SecurityConfig**：动态从 DB 加载 Permission → URL 拦截规则
8. **AdminController**：角色管理 + 权限树 + 用户分配角色 + 角色分配权限
9. **初始化数据**：admin/editor/viewer 三种角色 + 默认权限树

### 前端
1. **RoleManage.vue**：角色列表 + 创建/编辑弹窗 + 分配权限对话框（树形勾选）
2. Admin.vue 用户列表：角色分配下拉框（替换当前 role 编辑）
3. **api/rbac.js**

### 默认数据
```
权限树:
├── 文件管理
│   ├── 文件上传
│   ├── 文件下载
│   ├── 文件删除
│   └── 文件分享
├── 用户管理
│   ├── 用户列表
│   └── 角色分配
├── 系统管理
│   └── 公告管理
└── 管理面板

角色:
- admin: 全部权限
- editor: 文件管理 + 公告管理
- viewer: 文件下载（只读）
```
