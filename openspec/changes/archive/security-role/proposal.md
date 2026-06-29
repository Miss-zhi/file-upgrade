# 角色权限：RBAC 授权 + 前端权限控制

## Why

当前所有认证用户拥有同等权限（管理员端点仅靠路由守卫保护，后端无强制校验）。需要实现基于角色的访问控制（RBAC），区分 ADMIN 和 USER 两种角色，确保管理端点只有管理员可访问，前端根据角色自动显示/隐藏管理功能入口。

## What Changes

### 后端

1. **User 实体**：新增 `role` 字段（varchar，默认 "USER"）
2. **SecurityConfig**：URL 规则区分角色
   - `/admin/**` → 仅 ADMIN
   - `/user/**` → 所有认证用户
   - `/anonymous/**` `/share/verify` `/onlyoffice/callback` → 匿名
3. **方法级授权**：`@PreAuthorize("hasRole('ADMIN')")` 保护 AdminController
4. **JwtAuthFilter**：JWT 中存入角色信息，SecurityContext 携带 GrantedAuthority
5. **JwtUtil**：生成 Token 时包含 role claim
6. **AdminController**：新增 `PUT /admin/user/{id}/role` 角色分配端点
7. **UserAdminVO**：返回 role 字段

### 前端

1. **路由守卫**：检查用户角色，无权限跳转 403
2. **Layout.vue / AppHeader.vue**：根据 `userStore.userInfo.role` 显示/隐藏管理入口
3. **Admin.vue**：用户列表增加角色列，支持切换角色
4. **api/admin.js**：新增 `updateUserRole` API

### 不涉及

- 不实现多角色组合（一个用户只有一个角色）
- 不实现细粒度权限（如"只读文件"）
- 不实现动态权限配置界面

## Impact

- **修改**：User.java, JwtUtil.java, JwtAuthFilter.java, SecurityConfig.java, AdminController.java, UserAdminVO.java
- **新增**：无新实体（复用 User.role 字段）
- **前端修改**：Layout.vue, Admin.vue, stores/user.js, router/guards.js, api/admin.js
