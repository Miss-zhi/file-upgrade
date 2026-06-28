# 用户管理 — CRUD + 分页搜索 + 状态切换

## Purpose
提供管理员用户管理能力：分页搜索列表、编辑用户、删除用户、启用/禁用用户，前后端完整实现。

## ADDED Requirements

### Requirement: 分页搜索 API
POST /admin/user/list SHALL accept page/size/keyword and return paginated user list via MyBatis-Plus Page plugin

#### Scenario: 分页搜索
- **WHEN** POST /admin/user/list { page:1, size:10, keyword:"admin" }
- **THEN** 返回 { records:[...], total:N, size:10, current:1 }

### Requirement: 用户更新 API
PUT /admin/user SHALL update user editable fields (email/phone/nickname) and return updated user

#### Scenario: 编辑用户
- **WHEN** PUT /admin/user { id, email, phone, nickname }
- **THEN** 数据库更新，返回成功

### Requirement: 状态切换 API
PUT /admin/user/{id}/status?enabled=true SHALL toggle user enabled/disabled status

#### Scenario: 启禁用户
- **WHEN** PUT /admin/user/123/status?enabled=false
- **THEN** 用户状态置为禁用

### Requirement: 前端管理页面
Admin.vue SHALL render user table with search bar, pagination, status toggle, edit dialog, and delete confirm

#### Scenario: 管理页面
- **GIVEN** 管理员登录
- **WHEN** 导航到 /admin
- **THEN** 显示用户表格 + 搜索框 + 分页器，可编辑/启禁/删除用户

### Requirement: CI 兼容
Backend SHALL pass mvn test, frontend SHALL pass vue-tsc + vite build

#### Scenario: 验证通过
- **WHEN** 执行测试和构建
- **THEN** 全部成功
