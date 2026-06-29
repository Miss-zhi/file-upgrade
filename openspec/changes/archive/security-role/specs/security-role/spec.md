# 角色权限 — 详细需求规格

## ADDED Requirements

### Requirement: 用户角色字段
User entity SHALL include a `role` field with default value "USER", supporting values "ADMIN" and "USER"

#### Scenario: 新注册用户默认角色
- **GIVEN** 用户注册
- **WHEN** 调用 register API
- **THEN** role 字段自动设为 "USER"

#### Scenario: 角色存储
- **GIVEN** User 实体编译
- **WHEN** 执行 mvn compile
- **THEN** role 字段存在，类型为 String

### Requirement: JWT 携带角色
JWT Token SHALL include role claim, so the server can identify user role without database query

#### Scenario: Token 包含角色
- **GIVEN** 用户登录，role = "ADMIN"
- **WHEN** JwtUtil.generateToken(userId, role) 生成 Token
- **THEN** Token payload 包含 "role": "ADMIN"

#### Scenario: 解析角色
- **GIVEN** 有效 Token
- **WHEN** JwtAuthFilter 解析 Token
- **THEN** SecurityContext 的 Authentication 包含 ROLE_ADMIN GrantedAuthority

### Requirement: URL 级别角色控制
SecurityConfig SHALL restrict /admin/** endpoints to ADMIN role only, and allow /user/** for all authenticated users

#### Scenario: ADMIN 访问管理端点
- **GIVEN** Token 包含 role=ADMIN
- **WHEN** 请求 POST /admin/user/list
- **THEN** 返回 200 OK

#### Scenario: USER 访问管理端点被拒
- **GIVEN** Token 包含 role=USER
- **WHEN** 请求 POST /admin/user/list
- **THEN** 返回 403 Forbidden

### Requirement: 方法级授权
AdminController SHALL use @PreAuthorize("hasRole('ADMIN')") on class level as defense-in-depth

#### Scenario: 方法级保护
- **GIVEN** AdminController 注解 @PreAuthorize
- **WHEN** USER 角色用户调用管理端点
- **THEN** Spring Security 拦截并返回 403

### Requirement: 角色分配 API
PUT /admin/user/{id}/role SHALL allow ADMIN to change another user's role between "USER" and "ADMIN"

#### Scenario: 提升用户为管理员
- **GIVEN** 管理员已认证
- **WHEN** PUT /admin/user/123/role { role: "ADMIN" }
- **THEN** 用户 123 的 role 更新为 "ADMIN"

### Requirement: 前端角色感知
Layout.vue SHALL conditionally render admin/dashboard menu items based on userStore.userInfo.role

#### Scenario: 管理员看到管理入口
- **GIVEN** userStore.userInfo.role = "ADMIN"
- **WHEN** 渲染侧边栏/头部导航
- **THEN** 显示"管理后台"和"管理面板"菜单项

#### Scenario: 普通用户看不到管理入口
- **GIVEN** userStore.userInfo.role = "USER"
- **WHEN** 渲染侧边栏/头部导航
- **THEN** 不显示管理相关菜单项

### Requirement: CI 兼容
Backend SHALL pass mvn test (including role-based auth tests), frontend SHALL pass vue-tsc + vite build

#### Scenario: 测试通过
- **WHEN** 执行 mvn test + vue-tsc + vite build
- **THEN** 全部成功
