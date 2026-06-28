# 用户认证模块 — 登录/注册/JWT

## Purpose

实现奇文网盘用户认证的完整前后端流程：后端提供登录/注册/用户信息 API（Spring Security + JWT + BCrypt），前端提供登录/注册页面（Element Plus + Pinia），路由守卫根据 JWT 登录状态自动跳转。

## ADDED Requirements

### Requirement: 用户实体与持久化
User Entity SHALL use both JPA and MyBatis-Plus annotations, with snowflake ID, BCrypt-encrypted password, and standard timestamp fields

#### Scenario: User 实体可编译
- **GIVEN** 数据库连接配置就绪
- **WHEN** 执行 mvn compile
- **THEN** User.java 实体编译通过，双注解无冲突

#### Scenario: 密码加密存储
- **GIVEN** 用户注册提交密码
- **WHEN** UserService.register() 执行
- **THEN** 存储的密码为 BCrypt 加密后的密文

### Requirement: 登录 API
POST /user/login SHALL accept username+password, validate credentials via BCrypt, and return a JWT token valid for 7 days

#### Scenario: 登录成功
- **GIVEN** 数据库中已存在用户
- **WHEN** POST /user/login 携带正确的 username 和 password
- **THEN** 返回 RestResult { success: true, data: "eyJ..." }

#### Scenario: 登录失败
- **GIVEN** 密码不匹配
- **WHEN** POST /user/login
- **THEN** 返回 RestResult { success: false, message: "用户名或密码错误" }

### Requirement: 注册 API
POST /user/register SHALL accept username+password+email, check username uniqueness, encrypt password with BCrypt, and persist the user

#### Scenario: 注册成功
- **GIVEN** 用户名未被占用
- **WHEN** POST /user/register 携带完整信息
- **THEN** 返回 RestResult { success: true }，数据库新增一条 User 记录

#### Scenario: 用户名重复
- **GIVEN** 用户名已存在
- **WHEN** POST /user/register 携带相同用户名
- **THEN** 返回 RestResult { success: false, message: "用户名已存在" }

### Requirement: 用户信息 API
GET /user/info SHALL return current user details based on JWT token in Authorization header

#### Scenario: 获取用户信息
- **GIVEN** 请求头包含有效 JWT Token
- **WHEN** GET /user/info
- **THEN** 返回 RestResult { success: true, data: { id, username, email, nickname, avatar } }

#### Scenario: 未认证访问
- **GIVEN** 请求头无 Token
- **WHEN** GET /user/info
- **THEN** 返回 401 Unauthorized

### Requirement: 前端登录页面
Login.vue SHALL render a form with username/password fields, call Pinia userStore.login() on submit, and display error messages via ElMessage

#### Scenario: 登录表单提交
- **GIVEN** 用户在登录页输入用户名密码
- **WHEN** 点击登录按钮
- **THEN** 调用 userStore.login()，成功跳转 /home，失败显示错误

### Requirement: 前端注册页面
Register.vue SHALL render a registration form, call userStore.register(), and redirect to login page on success

#### Scenario: 注册表单提交
- **GIVEN** 用户填写完整注册信息
- **WHEN** 点击注册按钮
- **THEN** 调用 userStore.register()，成功后跳转 /login

### Requirement: 前端路由守卫
Router guards SHALL redirect authenticated users from /login to /home, and unauthenticated users from protected routes to /login

#### Scenario: 已登录用户访问登录页
- **GIVEN** localStorage 中有 token
- **WHEN** 导航到 /login
- **THEN** 自动跳转到 /home

#### Scenario: 未登录用户访问文件页
- **GIVEN** localStorage 中无 token
- **WHEN** 导航到 /file
- **THEN** 自动跳转到 /login

### Requirement: CI 兼容
Backend SHALL pass mvn compile + mvn test, frontend SHALL pass npx vite build

#### Scenario: 后端编译测试通过
- **WHEN** 执行 mvn test -f qiwen-file/pom.xml -Dspring.profiles.active=test
- **THEN** BUILD SUCCESS, UserServiceTest 全部通过

#### Scenario: 前端构建通过
- **WHEN** 执行 npx vite build
- **THEN** dist/ 目录生成，无编译错误
