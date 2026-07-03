## Context

当前前端项目（Vue 3 + Element Plus + TypeScript）已有一个基础的 `AdminView.vue`，仅包含简单的用户列表功能，使用旧的 API 路径（`/admin/user/list` 等）。后端已完成 admin 模块的全部 15 个端点，涵盖用户管理、角色权限管理、配额管理、审计日志和系统配置。前端需要全面升级以覆盖所有后端能力，并引入子路由布局实现多页面管理后台。

现有基础设施：
- `stores/auth.ts` 已提供 `hasPermission()` / `hasRole()` 方法，可直接用于菜单权限控制
- `api/admin.ts` 已有旧版 API 封装，需全部重写对接新端点
- 路由中 `/admin` 已注册为单一路由，需改为嵌套子路由
- 项目约定使用 `<script setup lang="ts">`、SCSS、composable 模式

## Goals / Non-Goals

**Goals:**
- 构建完整的 admin 管理前端，覆盖后端全部 5 个功能域（用户、角色、配额、日志、配置）
- 采用侧边导航子路由布局，各管理页面独立且可深链接
- 样式与旧项目保持视觉一致（相同的表格样式、间距、配色），同时遵循 Element Plus 组件规范
- 所有 API 调用对接新后端 `/api/v1/admin/*` 端点，类型安全
- 基于权限码控制菜单项和页面访问

**Non-Goals:**
- 不实现超级管理员之外的细粒度 RBAC 动态权限分配 UI（角色权限编辑仅面向超级管理员，以简单权限树形式呈现）
- 不实现操作日志的实时推送或 WebSocket 通知
- 不改造现有非 admin 页面的任何功能
- 不引入新的 npm 依赖

## Decisions

### 1. 路由结构：嵌套子路由 + AdminLayout

**选择**：将 `/admin` 改为嵌套路由，使用 `AdminLayout.vue` 作为父布局，内含侧边导航栏和 `<router-view>`。

```
/admin                → AdminLayout（重定向到 /admin/users）
/admin/users          → AdminUserList
/admin/roles          → AdminRoleList
/admin/quota          → AdminQuota
/admin/logs           → AdminAuditLog
/admin/config         → AdminSystemConfig
```

**理由**：5 个管理页面如果平铺为顶级路由，侧边栏导航需要额外实现。嵌套路由天然支持布局复用，且 URL 结构清晰，便于深链接和权限守卫。

**替代方案**：单页面 + Tab 切换 — 简单但不支持深链接，URL 无法直接定位到具体管理页面。

### 2. 权限控制：路由 meta + 侧边栏条件渲染

**选择**：在路由 `meta` 中声明所需权限码（如 `meta: { permission: 'admin:user-manage' }`），在 `AdminLayout` 中根据 `authStore.hasPermission()` 控制侧边栏菜单项的显隐。路由守卫中做二次校验。

**理由**：复用现有 `authStore.hasPermission()` 方法，无需新增状态管理逻辑。路由 meta 声明式配置直观且易于维护。

### 3. API 层：重写 api/admin.ts，按功能域拆分函数

**选择**：保留单文件 `api/admin.ts`，按功能域组织函数（用户管理、角色管理、配额管理、审计日志、系统配置），全部对接新后端端点。新增 `types/admin.ts` 定义所有接口类型。

**理由**：admin 模块 API 数量适中（15 个），单文件便于查找。类型定义集中在 `types/admin.ts` 符合项目现有模式（如 `types/file.ts`）。

**替代方案**：拆分为 `api/adminUser.ts`、`api/adminRole.ts` 等多文件 — 文件过多，增加维护成本。

### 4. 页面组件结构：每页独立 composable

**选择**：每个管理页面对应一个 composable（如 `useAdminUserList`、`useAdminRoleList`），封装数据获取、分页、搜索、操作逻辑。页面组件只负责模板渲染。

**理由**：与现有 preview 模块的 composable 模式一致（`useImagePreview`、`useVideoPreview` 等），保持项目风格统一。逻辑与视图分离，便于测试。

### 5. 对话框模式：内联对话框而非独立页面

**选择**：用户详情、修改配额、重置密码、编辑角色权限等操作使用 `el-dialog` 内联对话框，不跳转新页面。

**理由**：与旧项目行为一致（旧项目也是对话框模式）。管理操作均为轻量级交互，对话框足够且不打断用户上下文。

### 6. 样式策略：沿用旧项目视觉风格 + Element Plus 组件

**选择**：表格使用 `el-table` + `border` + `stripe`，搜索栏使用 `el-input` + `el-button`，分页使用 `el-pagination`。颜色、间距、字号参照旧项目 AdminUserList.vue 的 Stylus 样式转换为 SCSS。

**理由**：用户要求样式布局与旧项目 1:1 一致。旧项目 admin 页面结构简单，直接转换即可。

## Risks / Trade-offs

- **[权限数据依赖]** 侧边栏权限控制依赖 `authStore.fetchMe()` 返回的 permissions 列表。如果后端 `/auth/me` 接口未返回完整权限码，菜单显隐会异常。→ 缓解：在 `fetchMe` 中增加权限数据校验，缺失时记录警告。
- **[配额单位不一致]** 旧项目前端以 MB 为单位展示配额，后端 `AdminQuotaVO` 可能使用字节。→ 缓解：在 `types/admin.ts` 中明确单位约定，前端做单位转换工具函数。
- **[权限树复杂度]** 角色权限编辑需要展示权限树（父子两级），如果权限数量很多，对话框可能过长。→ 缓解：使用 `el-tree` 组件，支持折叠和搜索过滤。
- **[审计日志性能]** 日志表数据量可能很大，前端分页 + 后端分页配合。→ 缓解：限制默认 pageSize=20，时间范围筛选必选（避免全表扫描）。
