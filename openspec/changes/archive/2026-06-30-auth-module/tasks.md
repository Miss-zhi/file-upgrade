## 1. 项目骨架与基础设施

- [x] 1.1 创建后端 Maven 模块结构（`qiwenshare-backend/pom.xml`），添加 Spring Boot 3.2.x parent 及核心依赖（spring-boot-starter-web、spring-boot-starter-security、spring-boot-starter-data-jpa、spring-boot-starter-data-redis、spring-boot-starter-validation、spring-boot-starter-actuator）
- [x] 1.2 添加 jjwt 0.12.x（jjwt-api、jjwt-impl、jjwt-jackson）、mybatis-plus-spring-boot3-starter、mysql-connector-j、lombok、resilience4j-spring-boot3 依赖
- [x] 1.3 添加测试依赖（spring-boot-starter-test、spring-security-test、testcontainers mysql/redis）
- [x] 1.4 创建 `application.yml`（dev profile，含 jwt 和 redis 配置）和 `application-test.yml`（Testcontainers 配置）
- [x] 1.5 创建 `application-prod.yml`（prod profile，JWT_SECRET 从环境变量读取，Redis 连接指向生产集群，cookie Secure=true，SameSite=None）
- [x] 1.6 创建主类 `QiwenshareApplication.java`，验证 `mvn compile` 通过
- [x] 1.7 创建 `RedisConfig.java`：@Configuration，@Bean RedisTemplate<String, String>（StringRedisSerializer key + GenericJackson2JsonRedisSerializer value），@Bean StringRedisTemplate
- [x] 1.8 创建 `CorsConfig.java` 或在 SecurityConfig 中配置 CorsConfigurationSource：dev profile 允许 localhost:5173（Vite），prod profile 仅允许生产域名，allowedMethods GET/POST/PUT/DELETE，allowCredentials=true

## 2. 数据库 Schema 与 Flyway 迁移

- [x] 2.1 添加 Flyway 依赖（flyway-core + flyway-mysql），创建 `V1__create_auth_tables.sql`：user、role、permission、user_role、role_permission 五张表，含索引和约束（DDL 参考 design.md Supplement: 数据库 Schema）
- [x] 2.2 创建 `V2__init_auth_data.sql`：插入默认角色（管理员 roleId=1、普通用户 roleId=2）和基础权限数据（`resource:action` 格式）
- [x] 2.3 创建 `V3__migrate_existing_users.sql`：迁移脚本，从旧数据库（如有）导入已有用户数据，保留 old_password 和 salt 字段供 MD5→BCrypt 透明迁移使用
- [x] 2.4 启动应用验证 Flyway 自动执行，`SHOW TABLES` 确认 5 张表存在

## 3. JPA Entity 与 Repository

- [x] 3.1 创建 `User.java` Entity（@Entity @Table，自增主键 + user_id 业务字段，@Getter @Setter，禁止 @Data）
- [x] 3.2 创建 `Role.java`、`Permission.java` Entity（含 available 字段，Permission 含 parentId 自引用关系）
- [x] 3.3 创建 `UserRole.java`、`RolePermission.java` Entity（@IdClass 联合主键）
- [x] 3.4 创建 5 个 JpaRepository 接口，UserRepository 添加 findByTelephone/findByUserId/existsByTelephone/existsByUsername 自定义方法，PermissionRepository 添加 findByPermKeyIn，UserRoleRepository 添加 findByUserId/deleteByUserId，RolePermissionRepository 添加 findByRoleId/deleteByRoleId
- [x] 3.5 编写 @DataJpaTest 测试验证 CRUD 操作通过

## 4. 公共基础设施

- [x] 4.1 创建 `SnowflakeIdGenerator.java`：@Component，基于 Twitter Snowflake 算法（workerId 从环境变量或配置读取，datacenterId 默认 0），线程安全，generate() 返回 String 类型 ID
- [x] 4.2 创建 `RestResult<T>` record：统一响应包装（int code, String message, T data），静态工厂方法 success(data)、error(code, message)
- [x] 4.3 创建 `CookieUtils.java` utility class：buildCookie(name, value, maxAge) 返回 ResponseCookie（httpOnly、Secure by profile、SameSite=Lax、Path=/），clearCookie(name) 返回 Max-Age=0 的 ResponseCookie
- [x] 4.4 创建 `GlobalExceptionHandler.java`：@RestControllerAdvice，处理 AuthException（→RestResult.error）、MethodArgumentNotValidException（→参数校验错误）、AccessDeniedException（→403）、ConstraintViolationException（→400）

## 5. JWT 基础设施 — JwtProperties + TokenService

- [x] 5.1 创建 `JwtProperties.java`：@ConfigurationProperties(prefix = "jwt") record 类型，含 secret、accessTokenTtl、refreshTokenTtl、clockSkewSeconds，@PostConstruct 校验密钥长度 ≥ 32 字节
- [x] 5.2 创建 `TokenService.java`：实现 generateAccessToken（jjwt 0.12.x 新 API）、generateRefreshToken（含 Redis jti 注册）、parseAndValidate（Jwts.parser().verifyWith(key).build()）
- [x] 5.3 实现 TokenService 的黑名单方法：blacklist（Redis SET，TTL=剩余有效期）、isBlacklisted（Redis EXISTS）
- [x] 5.4 实现 TokenService 的全局撤销方法：revokeAllTokens（Redis SET revoke:all:{userId}=时间戳）、isRevoked（比较 iat 与撤销时间戳）
- [x] 5.5 实现 TokenService 的 refresh token 方法：registerRefreshToken、consumeRefreshToken（GET+DELETE 原子操作）、revokeAllRefreshTokens（SCAN+DELETE）
- [x] 5.6 编写 TokenService 单元测试：覆盖 token 生成/解析/过期/签名无效/黑名单/全局撤销/refresh rotation 场景

## 6. JwtAuthenticationFilter

- [x] 6.1 创建 `JwtAuthenticationFilter.java`：继承 OncePerRequestFilter，token 提取优先级 cookie access_token > Authorization header
- [x] 6.2 实现验证链：解析 JWT → 检查 type="access" → 黑名单检查 → 全局撤销检查（iat vs revoke:all 时间戳）→ 加载 UserDetails → 设置 SecurityContext
- [x] 6.3 无 token 或 token 无效时不抛异常，仅不设置 SecurityContext（放行给后续 AuthorizationFilter）
- [x] 6.4 编写集成测试：无 token→401、有效 token→200、过期 token→401、黑名单 token→401、全局撤销后旧 token→401

## 7. SecurityConfig + UserDetailsService

- [x] 7.1 创建 `SecurityConfig.java`：@Bean SecurityFilterChain（CSRF disabled、CORS、STATELESS session、authorizeHttpRequests），permitAll 端点（login/register/refresh/actuator/health/swagger），admin/** 限制 hasRole("ADMIN")
- [x] 7.2 配置 AuthEntryPoint（401 响应处理）和 AccessDeniedHandlerImpl（403 响应处理），使用 RestResult 统一响应格式
- [x] 7.3 配置 @Bean PasswordEncoder（BCryptPasswordEncoder strength=10）和 CorsConfigurationSource
- [x] 7.4 创建 `UserDetailServiceImpl.java`：loadUserByUsername 查 user → 查可用 roles（available=1）→ 查 permissions（含父权限继承展开）→ 构建 UserDetails（角色加 ROLE_ 前缀，权限不加），优先从 Redis 缓存读取权限（TTL 5 分钟），miss 时查 DB 并回填
- [x] 7.5 编写集成测试验证 permitAll 端点不需 token、admin 端点需 ADMIN 角色、UserDetailsService 加载权限正确

## 8. AuthController — 注册 + 登录

- [x] 8.1 创建 DTO：RegisterRequest（@NotBlank @Size username、@NotBlank @Pattern telephone、@NotBlank @Size @Pattern password）、LoginRequest（@NotBlank telephone、@NotBlank password），使用 record 类型
- [x] 8.2 创建 VO：LoginResponse（userId、username、roles、permissions）、UserInfoResponse、TokenPair
- [x] 8.3 创建 `AuthException.java` + `AuthErrorCode.java` 枚举（含 httpStatus 和 message 字段，覆盖所有错误码）
- [x] 8.4 创建 `AuthService.java`：register 方法（校验唯一→BCrypt hash→Snowflake userId→插入 user+user_role→返回 userId）
- [x] 8.5 实现 AuthService.login 方法：检查失败计数→查 user→验证密码（先 BCrypt，不匹配再尝试 MD5 + salt，MD5 匹配则乐观锁迁移为 BCrypt）→清除计数→生成 token pair→注册 refresh token→设置 cookie→返回 LoginResponse
- [x] 8.6 创建 `AuthController.java`：@RestController @RequestMapping("/api/v1/auth")，register 端点（POST，返回 201）和 login 端点（POST，返回 200 + Set-Cookie）
- [x] 8.7 在 GlobalExceptionHandler 中添加 @ExceptionHandler(AuthException.class) 处理方法，返回 RestResult 格式
- [x] 8.8 编写测试：注册成功→201、重复用户名→400、登录成功→200+Set-Cookie、密码错误→401、连续 5 次失败→423

## 9. AuthController — 登出 + Token 刷新

- [x] 9.1 实现 AuthService.logout 方法：从 cookie 提取 token→黑名单 access token jti→消费并黑名单 refresh token jti→清除 cookie（Max-Age=0）→幂等返回 200
- [x] 9.2 实现 AuthService.refresh 方法：从 cookie 提取 refresh token→解析验证→consumeRefreshToken（rotation）→jti 不存在触发重用检测（revokeAllRefreshTokens + revokeAllTokens）→生成新 token pair→设置 cookie
- [x] 9.3 在 AuthController 中添加 logout 端点（POST /api/v1/auth/logout）和 refresh 端点（POST /api/v1/auth/refresh）
- [x] 9.4 实现 Cookie 设置工具方法：httpOnly、Secure（prod profile）、SameSite=Lax、正确 Path 和 Max-Age
- [x] 9.5 编写测试：登出后旧 token→401、refresh 成功→新 cookie 且旧 refresh 不可用、重用旧 refresh→401+全部 token 失效

## 10. AuthController — 获取当前用户 + 修改密码

- [x] 10.1 实现 AuthService.getCurrentUser 方法：从 SecurityContext 提取 userId→检查 token 黑名单→查 user+roles+permissions→检查 available→缓存权限到 Redis→返回 UserInfoResponse（手机号脱敏）
- [x] 10.2 实现 AuthService.updatePassword 方法：验证旧密码→校验新密码强度（统一规则 `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,30}$`）和差异→BCrypt hash 更新→全局撤销 token（revokeAllTokens）→发布密码变更事件
- [x] 10.3 在 AuthController 中添加 me 端点（GET /api/v1/auth/me）和 updatePassword 端点（PUT /api/v1/auth/password）
- [x] 10.4 编写测试：/me 返回完整用户信息、修改密码后旧 token 失效、旧密码错误→401、新旧密码相同→400

## 11. 管理员重置密码 + 权限变更事件

- [x] 11.1 创建 `AdminUserController.java`：@RestController @RequestMapping("/api/v1/admin/users")，resetPassword 端点（@PreAuthorize("hasAuthority('admin:user-manage')")）
- [x] 11.2 实现 AuthService.resetPassword 方法：校验目标用户存在→hash 新密码→更新→全局撤销 token→记录操作日志
- [x] 11.3 创建 `PermissionChangedEvent.java`（Spring ApplicationEvent，含 List<Long> userIds）、`PermissionChangeEventPublisher.java`（发布事件）、`PermissionCacheInvalidator.java`（@EventListener 监听，删除 Redis 缓存）
- [x] 11.4 创建 `AdminRoleController.java`：@RestController @RequestMapping("/api/v1/admin/roles")，含 GET 角色列表和 PUT 更新角色权限端点
- [x] 11.5 在 AdminUserController 和 AdminRoleController 的角色/权限变更方法中调用 publisher 发布事件
- [x] 11.6 编写测试：管理员重置密码→目标用户 token 失效、无权限→403、权限变更后缓存立即清除

## 12. 前端认证集成

- [x] 12.1 清理旧 localStorage token 存储：移除所有 localStorage 中 auth 相关 key（token、refreshToken 等），确保无残留
- [x] 12.2 创建 `api/client.ts`：Axios 实例，withCredentials: true，响应拦截器处理 401 自动 refresh（含请求队列防并发：pending refresh promise + 排队等待机制）
- [x] 12.3 创建 `api/auth.ts`：login()、logout()、fetchMe()、refresh() 方法
- [x] 12.4 创建 `stores/auth.ts`：Pinia store，管理 user 状态和 login/logout/fetchMe 逻辑，使用 Composition API + script setup
- [x] 12.5 创建 `composables/useAuthGuard.ts`：路由守卫 composable，检查登录状态，未登录跳转 /login
- [x] 12.6 创建 `views/LoginView.vue`：登录表单（telephone + password），Element Plus 组件，调用 authStore.login()
- [x] 12.7 在 `router/index.ts` 中配置路由守卫：/login 和 /register 为公开路由，其余需要认证
- [x] 12.8 验证：登录成功→cookie 设置→跳转首页、未登录→跳转登录页、token 过期→自动 refresh→重试、refresh 失败→跳转登录页

## 13. 集成测试套件

- [x] 13.1 配置 @SpringBootTest + MockMvc + Testcontainers（MySQL + Redis）测试基础设施
- [x] 13.2 编写完整流程测试：注册→登录→获取用户信息→修改密码→登出
- [x] 13.3 编写边界场景测试：注册重复用户名→400、登录错误密码→401、连续 5 次失败→423
- [x] 13.4 编写 token 机制测试：refresh rotation 成功、重用旧 refresh→401+全部失效、登出后旧 token→401
- [x] 13.5 编写管理员和权限测试：管理员重置密码→token 失效、无权限→403、权限变更后缓存失效、ROLE_ 前缀匹配正确
- [x] 13.6 编写 MD5→BCrypt 透明迁移测试：首次登录旧账户自动迁移、并发登录迁移（乐观锁）
- [x] 13.7 运行 `mvn test` 全部通过，验证覆盖率 > 80%
