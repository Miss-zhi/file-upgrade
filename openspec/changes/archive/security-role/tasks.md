## 1. Tasks

- [x] 1.1 后端: User 实体添加 role 字段（默认 USER）
- [x] 1.2 后端: JwtUtil.generateToken 增加 role 参数 + claim
- [x] 1.3 后端: JwtAuthFilter 解析 role → GrantedAuthority
- [x] 1.4 后端: SecurityConfig 添加 hasRole("/admin/**") + @EnableMethodSecurity
- [x] 1.5 后端: AdminController 添加 @PreAuthorize + updateRole 端点
- [x] 1.6 后端: IUserService/UserService 添加 updateRole 方法
- [x] 1.7 后端: UserService.login/register 适配 role
- [x] 1.8 后端: UserAdminVO 添加 role 字段
- [x] 1.9 后端: 创建 UserRoleTest 权限测试
- [x] 1.10 后端: mvn test 验证全量 0 失败
- [x] 1.11 前端: stores/user.js + router/guards.js 适配角色
- [x] 1.12 前端: Admin.vue 添加角色列 + 角色切换按钮
- [x] 1.13 前端: Layout/AppHeader 根据角色显示/隐藏管理入口
- [x] 1.14 前端: api/admin.js 添加 updateUserRole
- [x] 1.15 前端: vue-tsc + vite build 验证
