# 用户认证模块：登录/注册/JWT

## Why

奇文网盘需要有用户认证系统。后端骨架已搭建了 SecurityConfig + JwtUtil，前端骨架已有 http.js 拦截器和 Pinia userStore。现在需要将这些骨架组件串联起来，实现完整的登录/注册流程。

## What Changes

### 后端（qiwen-file）

1. **User Entity** (`domain/user/User.java`)：双注解（@Entity + @TableName），主键雪花 ID，字段：username / password / email / phone / nickname / avatar / createTime / updateTime
2. **UserMapper** (`mapper/UserMapper.java`)：继承 `BaseMapper<User>`
3. **IUserService** (`api/IUserService.java`) + **UserService** (`service/UserService.java`)：登录（验证密码返回 JWT）、注册（BCrypt 加密存储）、获取当前用户信息
4. **UserController** (`controller/UserController.java`)：POST /user/login /user/register、GET /user/info，返回 RestResult
5. **LoginDTO / RegisterDTO** (`dto/user/`) + **UserVO** (`vo/user/`)
6. **SecurityConfig 更新**：注入 UserDetailsService（基于 User 查询），PasswordEncoder Bean

### 前端（qiwen-file-web）

1. **Login.vue**（重写占位页）：表单提交 → Pinia userStore.login() → JWT 存储
2. **Register.vue**（新建）：表单提交 → Pinia userStore.register()
3. **Pinia userStore**（重写占位骨架）：对接后端 API
4. **路由守卫**（完善 guards.js）：已登录跳首页，未登录跳登录

### 不涉及

- 不实现邮箱验证、短信验证
- 不实现 OAuth 第三方登录
- 不实现密码找回
- 不实现用户管理后台功能

## Impact

- **后端新增文件**：User.java, UserMapper.java, IUserService.java, UserService.java, UserController.java, LoginDTO.java, RegisterDTO.java, UserVO.java
- **后端修改文件**：SecurityConfig.java（注入 UserDetailsService + PasswordEncoder）
- **前端新增文件**：Register.vue
- **前端修改文件**：Login.vue（重写）、stores/user.js（重写）、router/guards.js（完善）
