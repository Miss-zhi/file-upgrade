## 1. Tasks

- [x] 1.1 创建 User Entity（domain/user/User.java）— @Entity + @TableName + @Data
- [x] 1.2 创建 UserMapper（继承 BaseMapper<User>）
- [x] 1.3 创建 IUserService 接口（api/IUserService.java）+ UserService 实现（登录/注册/查询）
- [x] 1.4 创建 LoginDTO + RegisterDTO（dto/user/）+ UserVO（vo/user/）
- [x] 1.5 创建 UserController（POST /user/login, POST /user/register, GET /user/info）
- [x] 1.6 更新 SecurityConfig（注入 PasswordEncoder Bean + UserDetailsService）
- [x] 1.7 创建测试配置文件 application-test.yml（H2 + JPA ddl-auto）
- [x] 1.8 创建 UserServiceTest（验证登录/注册逻辑）
- [x] 1.9 验证后端 mvn compile + mvn test 通过
- [x] 1.10 重写前端 Login.vue（表单 + ElMessage + Pinia userStore.login）
- [x] 1.11 新建前端 Register.vue（表单 + userStore.register + 跳转登录）
- [x] 1.12 重写前端 stores/user.js（对接后端 API）
- [x] 1.13 完善前端 router/guards.js（已登录→首页，未登录→登录）
- [x] 1.14 完善前端 api/user.js（对接后端端点）
- [x] 1.15 验证前端 npx vite build 成功
