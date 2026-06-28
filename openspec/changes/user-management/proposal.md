# 用户管理：CRUD + 分页搜索 + 状态切换

## Why

管理员需要对用户进行增删改查管理。已有用户认证和 User 实体，需要补充管理端 CRUD API（带分页搜索）和前端管理页面。

## What Changes

### 后端
1. **IUserService + UserService**：新增 `listUsers(page, size, keyword)` 分页搜索、`updateUser(dto)`、`deleteUser(id)`、`toggleStatus(id)`
2. **UserController**：新增 POST /admin/user/list、PUT /admin/user、DELETE /admin/user/{id}、PUT /admin/user/{id}/status
3. **UserUpdateDTO、UserQueryDTO**：更新入参 + 查询入参
4. **UserAdminVO**：管理端用户展示（含状态字段）
5. **UserServiceTest**：新增分页/更新/状态测试

### 前端
1. **Admin.vue**（重写占位页）：用户表格 + 搜索框 + 分页器
2. **UserEditDialog.vue**（新建）：编辑用户信息对话框（用户名/邮箱/昵称/状态）
3. **stores/user.js**（完善）：新增管理端 actions
4. **api/admin.js**（完善）：对接管理端 API

## Impact
- **后端新增**：UserUpdateDTO、UserQueryDTO、UserAdminVO，Controller 新增 4 个端点
- **后端修改**：IUserService、UserService、User、UserServiceTest
- **前端新增/修改**：Admin.vue、UserEditDialog.vue、stores/user.js、api/admin.js
