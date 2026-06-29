# Design: security-role — 技术方案

## 1. User 实体变更

**文件**：`com.qiwenshare.file.domain.user.User`

```java
// 新增字段
@Column(name = "role")
private String role = "USER";  // "ADMIN" | "USER"
```

## 2. JWT 工具链变更

### 2.1 JwtUtil.generateToken 签名变更

**文件**：`com.qiwenshare.file.config.jwt.JwtUtil`

```java
// 旧签名
public String generateToken(String userId)

// 新签名
public String generateToken(String userId, String role)
```

**实现**：
```java
public String generateToken(String userId, String role) {
    return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration() * 1000))
            .signWith(getSigningKey())
            .compact();
}
```

### 2.2 调用方更新

**文件**：`com.qiwenshare.file.service.UserService`

```java
// login 方法中
User user = findByUsername(username);
// ... 密码验证 ...
return jwtUtil.generateToken(user.getId(), user.getRole());
```

**文件**：`com.qiwenshare.file.service.UserService.register`

```java
// register 方法中
user.setRole("USER");  // 默认角色
```

## 3. JwtAuthFilter 变更

**文件**：`com.qiwenshare.file.config.security.JwtAuthFilter`

```java
@Override
protected void doFilterInternal(...) {
    // ... 解析 Token 获取 Claims ...
    String userId = claims.getSubject();
    String role = claims.get("role", String.class);  // 新增

    // 构造 Authority
    List<GrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_" + role)
    );

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

## 4. SecurityConfig 变更

**文件**：`com.qiwenshare.file.config.security.SecurityConfig`

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/user/login", "/user/register").permitAll()
    .requestMatchers("/anonymous/**", "/share/verify", "/onlyoffice/callback").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/doc.html").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")       // 新增：仅管理员
    .anyRequest().authenticated()
)
```

**新增 Bean**：
```java
@Bean
public MethodSecurityExpressionHandler expressionHandler() {
    return new DefaultMethodSecurityExpressionHandler();
}
```

## 5. 方法级授权

### 5.1 全局启用

**文件**：`com.qiwenshare.file.config.security.SecurityConfig`

在类上添加：
```java
@EnableMethodSecurity  // 新增注解
```

### 5.2 AdminController

**文件**：`com.qiwenshare.file.controller.AdminController`

在类上添加：
```java
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // 类级别保护
@RequiredArgsConstructor
public class AdminController {
```

## 6. AdminController 新增端点

```java
@Operation(summary = "分配用户角色")
@PutMapping("/user/{id}/role")
public RestResult<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
    String newRole = body.get("role");
    userService.updateRole(id, newRole);
    return RestResult.success();
}
```

## 7. IUserService + UserService 新增方法

**文件**：`com.qiwenshare.file.api.IUserService`

```java
void updateRole(String userId, String role);
```

**文件**：`com.qiwenshare.file.service.UserService`

```java
@Override
@Transactional
public void updateRole(String userId, String role) {
    User user = userMapper.selectById(userId);
    if (user == null) throw new QiwenException(404, "用户不存在");
    if (!"ADMIN".equals(role) && !"USER".equals(role))
        throw new QiwenException(400, "无效角色");
    user.setRole(role);
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
}
```

## 8. UserAdminVO 变更

**文件**：`com.qiwenshare.file.vo.user.UserAdminVO`

```java
// 新增字段
private String role;

// fromEntity 中增加
.role(user.getRole())
```

## 9. 前端变更

### 9.1 stores/user.js

```javascript
// fetchUserInfo 返回的 userInfo 包含 role 字段
async function fetchUserInfo() {
  const res = await getUserInfo()
  if (res.success) {
    userInfo.value = res.data  // res.data.role
  }
}
```

### 9.2 Layout.vue / AppHeader.vue

```html
<!-- 仅 ADMIN 可见 -->
<template v-if="userStore.userInfo?.role === 'ADMIN'">
  <el-menu-item index="/admin">用户管理</el-menu-item>
  <el-menu-item index="/dashboard">管理面板</el-menu-item>
</template>
```

### 9.3 router/guards.js

```javascript
// 新增角色检查
if (to.meta.admin && userStore.userInfo?.role !== 'ADMIN') {
  return next('/403')
}
```

### 9.4 Admin.vue

表格新增角色列：
```html
<el-table-column prop="role" label="角色" width="100">
  <template #default="{ row }">
    <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
      {{ row.role }}
    </el-tag>
  </template>
</el-table-column>
```

角色切换按钮（操作列）：
```html
<el-button link type="warning" size="small" @click="handleToggleRole(row)">
  {{ row.role === 'ADMIN' ? '降为普通用户' : '提升为管理员' }}
</el-button>
```

### 9.5 api/admin.js

```javascript
export async function updateUserRole(id, role) {
  return http.put(`/admin/user/${id}/role`, { role })
}
```

## 10. 测试方案

### UserRoleTest（新建）

```java
@SpringBootTest @ActiveProfiles("test") @Transactional
class UserRoleTest {
    @Test void testDefaultRoleIsUser()
    @Test void testAdminAccessGranted()      // 使用 @WithMockUser(roles = "ADMIN")
    @Test void testUserAccessDenied()        // 使用 @WithMockUser(roles = "USER")
    @Test void testUpdateRole()
}
```

## 11. 文件清单

### 修改文件
| 文件 | 变更 |
|---|---|
| `domain/user/User.java` | +role 字段 |
| `config/jwt/JwtUtil.java` | generateToken 签名 +role claim |
| `config/security/JwtAuthFilter.java` | 解析 role → GrantedAuthority |
| `config/security/SecurityConfig.java` | +hasRole("/admin/**") + @EnableMethodSecurity |
| `service/UserService.java` | login/register/updateRole |
| `controller/AdminController.java` | +@PreAuthorize + updateRole 端点 |
| `vo/user/UserAdminVO.java` | +role 字段 |
| `api/IUserService.java` | +updateRole |

### 新增文件
| 文件 | 说明 |
|---|---|
| `test/.../UserRoleTest.java` | 角色权限测试 |

### 前端修改
| 文件 | 变更 |
|---|---|
| `stores/user.js` | userInfo 取角色 |
| `router/guards.js` | +角色检查 |
| `views/Admin.vue` | +角色列 + 切换按钮 |
| `api/admin.js` | +updateUserRole |
