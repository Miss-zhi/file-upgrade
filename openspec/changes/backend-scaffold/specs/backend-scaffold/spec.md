# 后端骨架 — 项目结构与基础设施

## Purpose

定义奇文网盘后端模块（qiwen-file）的技术骨架：Maven 构建、Spring Boot 3.2 分层架构、核心基础设施（安全/JWT/搜索/缓存/存储抽象/API 文档），确保编译通过、CI 兼容。

## ADDED Requirements

### Requirement: Maven 构建配置
pom.xml SHALL use Spring Boot 3.2.5 parent + Java 17，包含所有必需依赖且版本兼容

#### Scenario: 依赖解析无冲突

- **WHEN** 执行 mvn dependency:tree -f qiwen-file/pom.xml
- **THEN** 所有依赖解析成功，无版本冲突警告

#### Scenario: 编译通过

- **WHEN** 执行 mvn compile -f qiwen-file/pom.xml -DskipTests
- **THEN** BUILD SUCCESS，无编译错误

### Requirement: Spring Boot 启动类
FileApplication.java SHALL 作为 Spring Boot 入口，启用异步和定时任务，可被 CI 编译

#### Scenario: 启动类可编译

- **GIVEN** FileApplication.java 存在且有 @SpringBootApplication
- **WHEN** 执行 mvn compile
- **THEN** FileApplication.class 生成到 target/ 目录

### Requirement: 统一响应体与全局异常处理
RestResult<T> SHALL 封装成功/失败响应，GlobalExceptionHandlerAdvice SHALL 统一捕获异常，QiwenException SHALL 提供业务异常

#### Scenario: RestResult 结构正确

- **GIVEN** RestResult 类存在
- **THEN** 包含 success/code/message/data 字段，有静态工厂方法 success() 和 fail()

#### Scenario: GlobalExceptionHandlerAdvice 捕获 QiwenException

- **GIVEN** GlobalExceptionHandlerAdvice 存在，标注 @RestControllerAdvice
- **THEN** 包含 @ExceptionHandler(QiwenException.class) 方法，返回 RestResult

### Requirement: AOP 操作日志
@MyLog 注解 SHALL 标注方法，WebLogAspect 切面 SHALL 自动记录操作日志

#### Scenario: 切面定义正确

- **GIVEN** WebLogAspect 类存在，标注 @Aspect @Component
- **THEN** 包含 @Around("@annotation(com.qiwenshare.file.aop.MyLog)") 切点

### Requirement: Spring Security 6 JWT 认证
SecurityConfig SHALL 使用 Lambda DSL，JWT SHALL 无状态认证，jjwt 0.12.x 工具链

#### Scenario: SecurityConfig Lambda DSL

- **GIVEN** SecurityConfig 类存在
- **THEN** 使用 SecurityFilterChain Bean + Lambda DSL 配置，不继承 WebSecurityConfigurerAdapter

#### Scenario: JWT 工具链可编译

- **GIVEN** JwtProperties 和 JwtUtil 类存在
- **WHEN** 执行 mvn compile
- **THEN** 使用 io.jsonwebtoken (jjwt 0.12.x) API，编译通过

### Requirement: Elasticsearch 8.x 客户端配置
ElasticsearchConfig SHALL 创建 ElasticsearchClient bean

#### Scenario: ES 配置可编译

- **GIVEN** ElasticsearchConfig 类存在，标注 @Configuration
- **THEN** @Bean ElasticsearchClient 方法使用 co.elastic.clients.elasticsearch 包（非旧版 RestHighLevelClient）

### Requirement: MyBatis-Plus 分页插件
MyBatisPlusConfig SHALL 配置分页拦截器

#### Scenario: 分页插件 Bean

- **GIVEN** MyBatisPlusConfig 类存在
- **THEN** @Bean MybatisPlusInterceptor 包含 PaginationInnerInterceptor

### Requirement: OpenAPI 3 文档配置
OpenApiConfig SHALL 使用 SpringDoc 配置 API 文档

#### Scenario: OpenAPI Bean

- **GIVEN** OpenApiConfig 类存在
- **THEN** @Bean OpenAPI 方法，title 为 '奇文网盘 API'

### Requirement: UFOP 存储抽象模块
com.qiwenshare.ufop 包结构 SHALL 完整，UFOPFactory 工厂类 SHALL 可用

#### Scenario: UFOP 模块编译

- **GIVEN** UFOP 模块所有包和类存在
- **WHEN** 执行 mvn compile
- **THEN** UFOP 相关 .class 文件生成

#### Scenario: UFOPFactory 可实例化

- **GIVEN** UFOPFactory 类存在
- **THEN** 包含 getUFOPInstance() 方法返回操作接口

#### Scenario: 本地存储操作实现

- **GIVEN** LocalStorageUploader 等实现类存在
- **THEN** 实现对应操作接口（Uploader/Downloader/Deleter/Reader/Writer/Renamer/Copier）
