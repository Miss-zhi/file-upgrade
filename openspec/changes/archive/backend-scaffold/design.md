# Design: backend-scaffold

## 架构决策

### 1. Maven 单模块结构

**决策**：使用单模块 Maven 项目（`qiwen-file/`），不拆分子模块。

**理由**：
- 旧项目是单模块，保持结构一致性降低迁移成本
- UFOP 作为子包（`com.qiwenshare.ufop`）在同一模块内，无需独立 artifact
- CI 流水线已按单模块配置（`mvn compile -f qiwen-file/pom.xml`）

### 2. 构造器注入替代 @Autowired

**决策**：Service 层统一使用 Lombok `@RequiredArgsConstructor` 构造器注入。

**理由**：
- Spring 官方推荐，便于单元测试 mock
- 符合 AGENTS.md 约定
- 避免 field injection 的不可变性缺陷

### 3. Spring Security Lambda DSL

**决策**：使用 `SecurityFilterChain` Bean + Lambda DSL，不继承 `WebSecurityConfigurerAdapter`（Spring Security 6 中已废弃）。

**关键配置**：
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationMode(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/user/login", "/user/register").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

### 4. jjwt 0.12.x API 变更

**决策**：使用 jjwt 0.12.x 新 API。

**API 变化**：
- `Jwts.parserBuilder()` → `Jwts.parser()`
- `setSigningKey(key)` on parser builder → `verifyWith(key)` on parser
- `parser.parseClaimsJws(token).getBody()` → `parser.parseSignedClaims(token).getPayload()`

### 5. Elasticsearch Java Client 8.x

**决策**：使用 `co.elastic.clients:elasticsearch-java:8.12.2`（新版 Java Client），不是旧版 `RestHighLevelClient`。

**关键点**：
```java
@Bean
public ElasticsearchClient elasticsearchClient() {
    RestClient restClient = RestClient.builder(new HttpHost(host, port)).build();
    ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
}
```

### 6. SpringDoc 替代 Springfox

**决策**：使用 `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0`。

**理由**：Springfox 不维护且不支持 Spring Boot 3。

### 7. UFOP 工厂模式

**决策**：UFOP 使用工厂模式 + 策略模式，UFOPFactory 根据配置选择存储实现。

**操作接口（7 种）**：
- Uploader — 上传
- Downloader — 下载
- Deleter — 删除
- Reader — 读取
- Writer — 写入
- Renamer — 重命名
- Copier — 复制

**存储实现（5 种）**：
- Local — 本地文件系统
- AliyunOSS — 阿里云对象存储
- FastDFS — FastDFS 分布式文件系统
- MinIO — MinIO 对象存储
- Qiniu — 七牛云存储

## 依赖版本矩阵

| 依赖 | GroupId:ArtifactId | 版本 |
|---|---|---|
| Spring Boot Parent | org.springframework.boot:spring-boot-starter-parent | 3.2.5 |
| MyBatis-Plus | com.baomidou:mybatis-plus-boot-starter | 3.5.6 |
| SpringDoc OpenAPI | org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.5.0 |
| jjwt (api) | io.jsonwebtoken:jjwt-api | 0.12.5 |
| jjwt (impl) | io.jsonwebtoken:jjwt-impl | 0.12.5 |
| jjwt (jackson) | io.jsonwebtoken:jjwt-jackson | 0.12.5 |
| Elasticsearch Java | co.elastic.clients:elasticsearch-java | 8.12.2 |
| Hutool | cn.hutool:hutool-all | 5.8.28 |
| Lombok | org.projectlombok:lombok | provided |

## 文件列表（预计新增）

```
qiwen-file/
├── pom.xml
└── src/main/java/com/qiwenshare/
    ├── file/
    │   ├── FileApplication.java
    │   ├── advice/GlobalExceptionHandlerAdvice.java
    │   ├── aop/
    │   │   ├── MyLog.java
    │   │   └── WebLogAspect.java
    │   ├── config/
    │   │   ├── es/ElasticsearchConfig.java
    │   │   ├── jwt/
    │   │   │   ├── JwtProperties.java
    │   │   │   └── JwtUtil.java
    │   │   ├── mybatisplus/MyBatisPlusConfig.java
    │   │   ├── openapi/OpenApiConfig.java
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── JwtAuthFilter.java
    │   │   └── threadpool/ThreadPoolConfig.java
    │   ├── constant/（空目录占位）
    │   ├── exception/
    │   │   └── QiwenException.java
    │   └── util/
    │       ├── DateUtil.java
    │       └── RestResult.java
    └── ufop/
        ├── UFOPFactory.java
        ├── autoconfiguration/
        ├── config/
        ├── constant/
        │   └── StorageType.java
        ├── exception/
        │   └── UFOPException.java
        ├── factory/
        ├── operation/
        │   ├── Uploader.java
        │   ├── Downloader.java
        │   ├── Deleter.java
        │   ├── Reader.java
        │   ├── Writer.java
        │   ├── Renamer.java
        │   └── Copier.java
        └── util/
```
