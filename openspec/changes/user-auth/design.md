# Design: user-auth

## 后端数据流

```
POST /user/login  { username, password }
    → UserController.login(LoginDTO)
    → UserService.login(username, password)
        → 查询 User by username
        → BCrypt 密码比对
        → 生成 JWT Token
    ← RestResult{ token, userInfo }

POST /user/register  { username, password, email }
    → UserController.register(RegisterDTO)
    → UserService.register(dto)
        → 检查用户名唯一性
        → BCrypt 加密密码
        → 保存 User
    ← RestResult{ success }

GET /user/info
    → UserController.getUserInfo()
    → JwtAuthFilter 解析 JWT 获取 userId
    → UserService.getUserById(userId)
    ← RestResult{ userInfo }
```

## 密码加密

使用 `PasswordEncoder`（BCryptPasswordEncoder）。

## JWT Token 结构

```
{
  "sub": "雪花用户ID",
  "iat": 签发时间,
  "exp": 过期时间（7天）
}
```

## SecurityConfig 更新

```java
// 新增 Bean
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 新增 UserDetailsService（临时用匿名类，后续可提取）
@Bean
public UserDetailsService userDetailsService(IUserService userService) {
    return username -> {
        User user = userService.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException(username);
        return new org.springframework.security.core.userdetails.User(
            user.getId().toString(), user.getPassword(), Collections.emptyList());
    };
}
```

## 前端页面流程

```
登录页 → 输入用户名密码 → 点击登录
    → userStore.login()
    → POST /api/user/login
    → 成功：存储 token 到 localStorage，跳转 /home
    → 失败：ElMessage.error 显示错误信息

注册页 → 输入信息 → 点击注册
    → userStore.register()
    → POST /api/user/register
    → 成功：跳转登录页
    → 失败：ElMessage.error
```

## 文件清单

### 后端新增
```
qiwen-file/src/main/java/com/qiwenshare/file/
├── domain/user/User.java
├── mapper/UserMapper.java
├── api/IUserService.java
├── service/UserService.java
├── controller/UserController.java
├── dto/user/LoginDTO.java
├── dto/user/RegisterDTO.java
└── vo/user/UserVO.java
```

### 后端修改
```
qiwen-file/src/main/java/com/qiwenshare/file/config/security/SecurityConfig.java
```

### 前端新增/修改
```
qiwen-file-web/src/
├── views/Register.vue              （新建）
├── views/Login.vue                 （重写）
├── stores/user.js                  （重写）
├── api/user.js                     （完善）
└── router/guards.js                （完善）
```
