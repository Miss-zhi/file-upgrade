## 1. 基础设施：类型定义与 API 层

- [x] 1.1 创建 `types/admin.ts`，定义所有 admin 相关接口类型：`UserListVO`、`UserDetailVO`、`RoleVO`、`PermissionVO`、`AdminQuotaVO`、`OperationLogVO`、`SystemConfigVO`、`PageResult<T>` 等
- [x] 1.2 重写 `api/admin.ts`，对接全部 15 个新后端端点（`/api/v1/admin/users`、`/api/v1/admin/roles`、`/api/v1/admin/quota`、`/api/v1/admin/logs`、`/api/v1/admin/config`），所有函数带完整 TypeScript 类型签名
- [x] 1.3 创建 `utils/admin.ts`，添加配额单位转换工具函数（字节 ↔ MB/GB 可读格式）

## 2. 路由与布局

- [x] 2.1 创建 `layouts/AdminLayout.vue`：左侧侧边导航栏（用户管理、角色管理、配额管理、审计日志、系统配置）+ 右侧 `<router-view>` 内容区，根据 `authStore.hasPermission()` 条件渲染菜单项
- [x] 2.2 修改 `router/index.ts`：将 `/admin` 单路由改为嵌套子路由结构（`/admin/users`、`/admin/roles`、`/admin/quota`、`/admin/logs`、`/admin/config`），每个子路由在 `meta` 中声明所需 `permission` 码，使用 `AdminLayout` 作为父组件
- [x] 2.3 在路由守卫中增加 admin 子路由的权限校验：访问时检查 `authStore.hasPermission(meta.permission)`，无权限则展示 403 或重定向

## 3. 用户管理页面（admin-user-management）

- [x] 3.1 创建 `composables/useAdminUserList.ts`：封装用户列表数据获取、分页、搜索、启用/禁用、重置密码、修改配额逻辑
- [x] 3.2 创建 `views/admin/AdminUserList.vue`：搜索栏（用户名 + 手机号）、分页表格（用户名、手机号、邮箱、存储用量进度条、注册时间、状态标签、操作按钮）、用户详情对话框、修改配额对话框、重置密码对话框
- [x] 3.3 样式对齐旧项目 `AdminUserList.vue`：表格 border+stripe、搜索栏布局、分页居中、进度条颜色分段（绿/黄/红）

## 4. 角色权限管理页面（admin-role-management）

- [x] 4.1 创建 `composables/useAdminRoleList.ts`：封装角色列表获取、权限树数据构建、权限更新逻辑
- [x] 4.2 创建 `views/admin/AdminRoleList.vue`：角色表格（角色名称、权限数量、操作）、权限编辑对话框（使用 `el-tree` 展示两级权限树，支持勾选/取消）

## 5. 配额管理页面（admin-quota-management）

- [x] 5.1 创建 `composables/useAdminQuota.ts`：封装配额列表获取、单用户配额修改、批量配额设置逻辑
- [x] 5.2 创建 `views/admin/AdminQuota.vue`：用户配额表格（用户名、已用空间、总配额、可用空间、操作）、修改配额对话框、批量设置配额对话框（支持多选用户）

## 6. 审计日志页面（admin-audit-log）

- [x] 6.1 创建 `composables/useAdminAuditLog.ts`：封装日志列表获取、多条件筛选（模块、操作类型、用户名、时间范围）、分页逻辑
- [x] 6.2 创建 `views/admin/AdminAuditLog.vue`：筛选栏（模块下拉、操作类型下拉、用户名输入、日期范围选择器）、分页日志表格（操作人、模块、操作、描述、请求方法、路径、响应码、IP、耗时、时间）、日志详情对话框

## 7. 系统配置页面（admin-system-config）

- [x] 7.1 创建 `composables/useAdminSystemConfig.ts`：封装配置列表获取、搜索、新增、编辑、删除逻辑
- [x] 7.2 创建 `views/admin/AdminSystemConfig.vue`：搜索栏、分页配置表格（配置键、配置值、描述、创建时间、更新时间、操作）、新增/编辑配置对话框（含 configKey 唯一性校验提示）、删除确认对话框

## 8. 集成与验证

- [x] 8.1 更新 `layouts/AppAside.vue`：将原有单一"后台管理"链接替换为按权限码显隐的 admin 子菜单组
- [x] 8.2 运行 `vue-tsc --noEmit` 确保 TypeScript 零错误
- [x] 8.3 运行 `vite build` 确保生产构建通过
