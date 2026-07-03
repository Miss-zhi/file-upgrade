# 安全与认证规则

## JWT 密钥管理

签名密钥 ≥ 256 bit（32 字节），Base64 编码，从环境变量 `JWT_SECRET` 读取，禁止硬编码在代码或配置文件中。

```yaml
jwt:
  secret: ${JWT_SECRET}
```

算法固定为 HS256（项目启动时选定，不允许运行时可配）。在 `@PostConstruct` 中校验密钥长度：

```java
@PostConstruct
public void validate() {
    byte[] key = Base64.getDecoder().decode(secret);
    if (key.length < 32) {
        throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
    }
}
```

时钟偏移容忍度可配置，默认 30 秒。过期时间单位明确为秒，在启动时校验合理性。

## 双 Token 机制

- **Access Token**：15 分钟有效期，payload 包含 `sub=userId`、`type=access`、`jti=UUID`、`roles`
- **Refresh Token**：7 天有效期，payload 包含 `sub=userId`、`type=refresh`、`jti=UUID`（不携带 roles，减少 token 大小）

### Token 传输

通过 httpOnly cookie 发送，不再用 localStorage。Cookie 设置：
- `HttpOnly`（JS 不可读）
- `Secure`（HTTPS，prod profile 启用）
- `SameSite=Lax`
- Access token cookie：`Path=/`，`Max-Age=900`
- Refresh token cookie：`Path=/api/v1/auth/refresh`，`Max-Age=604800`（限定路径，减少发送范围）

同时保留从 `Authorization: Bearer <token>` header 读取的能力，兼容 API 调用场景。

### Token 刷新（Rotation 策略）

1. 客户端发送 refresh token（cookie 自动携带）
2. 验证 JWT 签名和过期时间
3. 检查 `type` claim 必须为 `"refresh"`
4. 提取 `jti`，查 Redis key `refresh:{jti}`
5. 若 key 存在：删除旧 jti，签发新 refresh token（新 jti），注册到 Redis → 正常刷新
6. 若 key 不存在：**重用检测触发** → 删除该用户所有 refresh token → 返回 401 `AUTH_TOKEN_REVOKED`

### Token 黑名单（登出）

登出时将 access token 和 refresh token 的 jti 加入 Redis SET：
- Key: `blacklist:{jti}`
- Value: `"1"`
- TTL: token 剩余有效期（不浪费 Redis 内存）

JwtAuthenticationFilter 每次验证 token 后检查黑名单。

### 全局 Token 撤销（修改密码 / 管理员重置）

修改密码或管理员重置密码时，需要使该用户所有已签发的 token 立即失效：
- 在 Redis 中记录 `revoke:all:{userId}` = 当前 Unix 时间戳
- JwtAuthenticationFilter 检查 token 的 `iat`（签发时间）是否早于此时间戳
- 早于则视为无效
- TTL 7 天（覆盖 refresh token 的最长有效期）

## 密码安全

### BCrypt 编码

使用 Spring Security 内置 `BCryptPasswordEncoder`，strength = 10。不再使用旧系统的 MD5 + salt 方案。

### MD5 → BCrypt 透明迁移

旧用户首次登录时自动升级：
1. 先尝试 BCrypt matches
2. 若不匹配，尝试旧 MD5 hash（`HashUtils.hashHex("MD5", password, salt, iterations)`）
3. 若 MD5 匹配成功：用 BCrypt 重新 hash 密码，更新 user.password，清除 salt 字段
4. 后续登录只走 BCrypt

### 密码强度校验

- 最少 8 位，最多 30 位
- 必须包含大写字母、小写字母和数字
- 正则：`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,30}$`
- 新密码不能与旧密码相同（修改密码时校验）

## RBAC 权限模型

五表结构：User → UserRole → Role → RolePermission → Permission

### 权限编码规范

遵循 `resource:action` 命名格式：

```
file:read, file:upload, file:download, file:delete, file:share
share:create, share:manage
admin:user-manage, admin:system-config
```

### 权限加载与缓存

1. JwtAuthenticationFilter 验证 token 后，从 Redis 缓存加载用户权限（key: `user:perms:{userId}`，TTL: 5 分钟）
2. 缓存 miss 时查数据库，回填缓存
3. 构建 `UsernamePasswordAuthenticationToken` 写入 SecurityContext

### 权限变更缓存失效

角色或权限变更时，通过 Spring ApplicationEvent 驱动缓存失效：

```java
// 发布事件
eventPublisher.publishEvent(new PermissionChangedEvent(userId));

// 监听并清除缓存
@EventListener
public void onPermissionChanged(PermissionChangedEvent event) {
    redisTemplate.delete("user:perms:" + event.getUserId());
}
```

Auth 相关缓存 TTL ≤ 5 分钟。

### 角色可用性

角色的 `available` 字段在认证时必须检查。`available = 0` 的角色不参与权限计算。

### 权限继承

Permission 表有 `parent_id` 字段支持层级继承。权限继承链必须有集成测试覆盖。

## 资源级权限校验（IDOR 防护）

所有涉及资源访问的端点必须显式调用权限服务校验当前用户对目标资源的操作权限。仅通过认证（登录）不等于有权访问任意资源。

```java
// 禁止 — 仅认证，未校验用户对具体资源的权限
@GetMapping("/{userFileId}/history")
public RestResult<List<DocumentVersionVO>> history(@PathVariable Long userFileId,
                                                    Authentication auth) {
    return RestResult.success(service.listVersions(userFileId)); // ❌ 任何登录用户可访问
}

// 正确 — 显式校验资源权限
@GetMapping("/{userFileId}/history")
public RestResult<List<DocumentVersionVO>> history(@PathVariable Long userFileId,
                                                    Authentication auth) {
    Long userId = parseUserId(auth);
    if (!filePermissionService.canView(userId, userFileId)) { // ✅ 资源级权限校验
        throw new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED);
    }
    return RestResult.success(service.listVersions(userFileId));
}
```

## OnlyOffice 回调 JWT 验证

OnlyOffice Document Server 使用独立的 JWT secret 签名回调请求，与应用级签名密钥（`JWT_SECRET`）不同。回调鉴权必须使用 OnlyOffice 配置的 secret 验证，禁止复用应用级 token 解析方法。

```java
// 禁止 — 用应用级密钥验证 OnlyOffice JWT，secret 不同时全部失败
Claims claims = tokenService.parseCallbackToken(onlyOfficeJwt); // ❌ 应用级密钥

// 正确 — 用 OnlyOffice 独立 secret 验证
Claims claims = documentTokenService.verifyOnlyOfficeJwt(onlyOfficeJwt); // ✅ OnlyOffice 密钥
```

## CORS 配置

显式白名单前端域名，生产环境禁止通配符 `*`：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://yourdomain.com"));  // 从配置读取
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

使用 cookie 认证时必须设置 `Access-Control-Allow-Credentials: true`。

## 公开端点白名单

Security Filter Chain 中显式 permit 的端点：

```
/api/v1/auth/login
/api/v1/auth/register
/api/v1/auth/refresh
/api/v1/share/check
/api/v1/document/callback
/actuator/health
/v3/api-docs/**
/swagger-ui/**
```

其余端点 deny-by-default。OPTIONS 预检请求全局放行。

## 登录失败锁定

连续登录失败 ≥ 5 次，锁定账户 15 分钟：
- Redis key: `login:fail:{telephone}`，value 为失败次数，TTL 15 分钟
- 每次登录失败计数 +1
- 登录成功后清除计数
- 计数 ≥ 5 时返回 423 `AUTH_ACCOUNT_LOCKED`

## Redis Key 规格

| Key 模式 | Value | TTL | 用途 |
|----------|-------|-----|------|
| `login:fail:{telephone}` | 失败次数 | 15 分钟 | 登录失败计数 |
| `refresh:{jti}` | userId | 7 天 | Refresh token 注册 |
| `blacklist:{jti}` | "1" | token 剩余有效期 | Token 黑名单 |
| `revoke:all:{userId}` | Unix 时间戳 | 7 天 | 全局 token 撤销 |
| `user:perms:{userId}` | 权限列表 JSON | 5 分钟 | 权限缓存 |
