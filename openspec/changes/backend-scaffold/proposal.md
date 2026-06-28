# 后端骨架搭建：Spring Boot 3.2 项目结构与基础设施

## Why

奇文网盘从 Spring Boot 2 / Java 8 升级到 Spring Boot 3.2 / Java 17，旧源码已清除。需要从零搭建完整的后端项目骨架，覆盖构建配置、分层架构、核心基础设施，为后续业务迁移提供坚实基础。

## What Changes

### 新增

1. **Maven 构建配置** (`qiwen-file/pom.xml`)：Spring Boot 3.2.5 parent，Java 17，所有必需依赖（spring-boot-starter-web、mybatis-plus、spring-boot-starter-data-jpa、spring-security、jjwt、elasticsearch-java、springdoc-openapi、hutool、lombok），以及 CI 友好的 test profile
2. **启动类** (`FileApplication.java`)：`@SpringBootApplication` + `@EnableScheduling` + `@EnableAsync`
3. **分层包结构**：Controller → Service(api+service) → Mapper → Entity，完整包目录树
4. **核心配置**：
   - SecurityConfig — Spring Security 6 Lambda DSL，JWT 无状态认证
   - JWT 工具链 — JwtProperties + JwtUtil (jjwt 0.12.x)
   - MyBatisPlusConfig — 分页插件
   - ElasticsearchConfig — ElasticsearchClient bean
   - OpenApiConfig — SpringDoc OpenAPI 3
   - ThreadPoolConfig — 异步线程池
   - RedisConfig — Lettuce 连接配置
5. **全局基础设施**：
   - `RestResult<T>` — 统一响应体
   - `GlobalExceptionHandlerAdvice` — 全局异常处理
   - `QiwenException` — 业务异常类
   - `WebLogAspect` + `@MyLog` — AOP 操作日志
   - `DateUtil` — 时间工具类（Hutool 封装）
6. **UFOP 存储抽象模块** (`com.qiwenshare.ufop`)：完整包结构，UFOPFactory + 7 种操作接口 × 5 种存储实现的抽象层
7. **CI 验证**：`mvn compile -f qiwen-file/pom.xml -DskipTests` 通过

### 不涉及

- 不实现任何具体的业务 Controller/Service（留待后续 change）
- 不实现具体的 Entity/Mapper（留待后续 change）
- 不修改 CI 流水线（`.github/workflows/ci.yml` 已就绪）

## Capabilities

### New Capabilities
- `backend-build`：Maven 编译通过，所有依赖解析无冲突
- `backend-security`：Spring Security 6 无状态 JWT 认证框架就绪
- `backend-infra`：全局异常处理、AOP 日志、统一响应体、工具类就绪
- `backend-ufop`：UFOP 存储抽象模块包结构和接口定义就绪

### Modified Capabilities
- 无（新项目，无旧代码）

## Impact

- **新增文件**：`qiwen-file/pom.xml`、`FileApplication.java`、所有 config/*.java、advice/*.java、aop/*.java、exception/*.java、util/*.java、UFOP 模块完整包结构
- **依赖**：pom.xml 中导入所有必需依赖，无外部新增依赖
- **兼容性**：CI 流水线 `backend-compile` job 可直接通过
