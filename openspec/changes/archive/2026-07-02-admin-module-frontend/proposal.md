## Why

后端 admin 模块已完成（15 个端点，覆盖用户管理、角色权限、配额、审计日志、系统配置），但前端仍停留在旧项目的单页 `AdminUserList.vue`（Vue 2 Options API + Element UI + Stylus）。旧项目只有用户列表一个页面，角色管理、审计日志、系统配置等功能完全没有前端界面。本次变更为 admin 模块构建完整的 Vue 3 前端，重写用户管理并新增 4 个全新管理页面。

## What Changes

- 新增 admin 子路由布局（`/admin/users`、`/admin/roles`、`/admin/quota`、`/admin/logs`、`/admin/config`），通过侧边导航或 Tab 切换 5 个管理页面
- 重写用户管理页面：Vue 2 Options API → Vue 3 `<script setup lang="ts">`，Element UI → Element Plus，Stylus → SCSS，对接新 API（`/api/v1/admin/users`），新增角色权限展示
- 新增角色权限管理页面：角色列表 + 权限树编辑，对接 `/api/v1/admin/roles`
- 新增配额管理页面：支持单用户配额编辑和批量配额设置，对接 `/api/v1/admin/quota`
- 新增审计日志查看页面：多条件筛选（模块、操作、用户名、时间范围），对接 `/api/v1/admin/logs`
- 新增系统配置管理页面：配置项 CRUD 表格，对接 `/api/v1/admin/config`
- 侧边栏管理入口增加细粒度权限控制（按 `admin:user-manage`、`admin:role-manage` 等权限码显隐菜单项）
- 新增 `api/admin.ts` API 层和 `types/admin.ts` 类型定义

## Capabilities

### New Capabilities

- `admin-user-management`: 用户管理页面——搜索、分页表格、查看详情（含角色权限）、修改配额、重置密码、启用/禁用用户
- `admin-role-management`: 角色权限管理页面——角色列表、权限树编辑、角色权限更新
- `admin-quota-management`: 配额管理页面——用户配额查看与编辑、批量配额设置
- `admin-audit-log`: 审计日志查看页面——多条件筛选、分页日志表格
- `admin-system-config`: 系统配置管理页面——配置项 CRUD、关键词搜索

### Modified Capabilities

（无。admin 前端为全新能力，不修改已有 spec。）

## Impact

- **路由**：`router/index.ts` 的 `/admin` 路由从单组件改为嵌套子路由
- **布局**：新增 `layouts/AdminLayout.vue`，包含侧边导航和 `<router-view>`
- **侧边栏**：`layouts/AppAside.vue` 的管理入口从单一链接改为按权限码显隐多个子菜单项
- **API 层**：新增 `api/admin.ts`，封装 15 个 admin 端点的 Axios 调用
- **类型定义**：新增 `types/admin.ts`，定义 `UserListVO`、`UserDetailVO`、`RoleVO`、`PermissionVO`、`AdminQuotaVO`、`OperationLogVO`、`SystemConfigVO` 等接口
- **Store**：可能需要扩展 `stores/auth.ts` 以暴露当前用户的权限码列表，供菜单权限控制使用
- **依赖**：无新增 npm 依赖，全部使用 Element Plus 现有组件
- **后端 API**：全部依赖已有端点，无需后端变更
