# 技术迁移规则

## javax → jakarta 迁移

Spring Boot 3 基于 Jakarta EE 10，所有 `javax.*` 命名空间迁移到 `jakarta.*`。

| 旧包名 | 新包名 |
|--------|--------|
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.annotation.*` | `jakarta.annotation.*` |
| `javax.transaction.*` | `jakarta.transaction.*` |
| `javax.mail.*` | `jakarta.mail.*` |

CI pipeline 中必须包含 javax 残留扫描：

```bash
! grep -r "javax\.\(persistence\|servlet\|validation\|annotation\|transaction\)" src/main/java/
```

应为零结果。

## Spring Security 6 迁移

| 旧 API (Security 5) | 新 API (Security 6) |
|---------------------|---------------------|
| `extends WebSecurityConfigurerAdapter` | `@Bean SecurityFilterChain` |
| `http.antMatchers("/path")` | `http.requestMatchers("/path")` |
| `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` |
| `authorizeRequests()` | `authorizeHttpRequests()` |
| `http.csrf().disable()` | `http.csrf(csrf -> csrf.disable())` |
| `http.cors()` | `http.cors(cors -> cors.configurationSource(...))` |
| `http.sessionManagement().sessionCreationPolicy(...)` | `http.sessionManagement(sm -> sm.sessionCreationPolicy(...))` |
| `FilterSecurityInterceptor` + `ObjectPostProcessor` | `AuthorizationFilter`（Spring Security 6 内置） |

SecurityConfig 模板见 [SECURITY.md](SECURITY.md) 和 `specs/auth/03-design.md`。

URL 级动态权限控制的迁移：旧系统用 `UrlFilterInvocationSecurityMetadataSource` + `UrlAccessDecisionManager` 配合 `ObjectPostProcessor` 注入到 `FilterSecurityInterceptor`。Security 6 中 `FilterSecurityInterceptor` 被 `AuthorizationFilter` 替代，动态权限改用 `@PreAuthorize` 方法级注解或自定义 `AuthorizationManager`。

## JWT 库迁移（jjwt 0.9.x → 0.12.x）

### 依赖变化

```xml
<!-- 旧 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.1</version>
</dependency>

<!-- 新 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### API 变化

```java
// 旧 (0.9.x)
String token = Jwts.builder()
    .setSubject(userId)
    .setExpiration(new Date(exp))
    .signWith(SignatureAlgorithm.HS256, key)
    .compact();

Claims claims = Jwts.parser()
    .setSigningKey(key)
    .parseClaimsJws(token)
    .getBody();

// 新 (0.12.x)
String token = Jwts.builder()
    .subject(userId)
    .expiration(new Date(exp))
    .signWith(key)  // 自动选择算法
    .compact();

Claims claims = Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

关键区别：
- `setSubject` → `subject`，`setExpiration` → `expiration`
- `signWith(alg, key)` → `signWith(key)`（算法从 key 类型推断）
- `setSigningKey` → `verifyWith`
- `parseClaimsJws` → `parseSignedClaims`
- `getBody` → `getPayload`
- `SecretKey` 构建方式变化：`Keys.hmacShaKeyFor(keyBytes)` 替代手动 `new SecretKeySpec`

## MyBatis-Plus 3.5.x 迁移

- Spring Boot 3 starter：`mybatis-plus-spring-boot3-starter`（不是 `mybatis-plus-boot-starter`）
- 分页插件 API 基本兼容
- 代码生成器 API 有变化（如果项目中使用）

## UFOP Starter 迁移

旧系统使用 `spring.factories` 注册 auto-configuration：

```
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.qiwenshare.ufop.config.UFOPAutoConfiguration
```

Spring Boot 3 改用新机制：

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.qiwenshare.ufop.config.UFOPAutoConfiguration
```

`spring.factories` 文件可以删除（Spring Boot 3 不再读取）。

## Vue 3 迁移

详细规范见 [FRONTEND.md](FRONTEND.md)。关键变化：

| Vue 2 | Vue 3 |
|-------|-------|
| Options API | Composition API + `<script setup>` |
| Vuex | Pinia |
| Vue CLI / Webpack | Vite 5 |
| Element UI | Element Plus |
| Stylus | SCSS |
| `Vue.extend` 动态组件 | composable 函数 |
| `require.context` | `import.meta.glob` |
| `beforeDestroy` / `destroyed` | `onBeforeUnmount` / `onUnmounted` |
| `el-dialog :visible.sync` | `el-dialog v-model` |

Element UI → Element Plus 组件 API 差异较大的：
- `el-dialog`：`visible` → `model-value`（`v-model`）
- `el-table`：事件名统一为 kebab-case
- `el-form`：校验方法从 `validate(callback)` 改为 `validate(): Promise`
- `el-message` / `el-message-box`：从 `this.$message` 改为 `import { ElMessage } from 'element-plus'`
