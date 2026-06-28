# AGENTS.md — 后端模块（qiwen-file）

> 本文件是后端 Java 代码的模块级约定，补充根目录 AGENTS.md 中的通用规则。

## 技术栈

- Java 17 + Spring Boot 3.2.x
- MyBatis-Plus 3.5.x + JPA/Hibernate（双 ORM）
- Spring Security 6（Lambda DSL）+ jjwt 0.12.x
- Elasticsearch Java Client 8.12.x
- Redis (Lettuce)
- UFOP 存储抽象（Local / Aliyun OSS / FastDFS / MinIO / Qiniu）
- OnlyOffice Document Server 集成

## 包结构

```
com.qiwenshare.file
├── FileApplication.java          ← 启动类
├── advice/                       ← 全局异常处理
├── aop/                          ← 日志切面（WebLogAspect + @MyLog）
├── api/                          ← Service 接口（IXxxService）
├── component/                    ← 业务组件
├── config/
│   ├── es/                       ← Elasticsearch 客户端配置
│   ├── jwt/                      ← JWT 配置（JwtProperties, JwtUtil）
│   ├── mybatisplus/              ← MyBatis-Plus 分页插件
│   ├── onlyoffice/               ← OnlyOffice 集成
│   ├── openapi/                  ← SpringDoc OpenAPI 3
│   ├── security/                 ← Spring Security 6 Lambda DSL
│   └── threadpool/               ← 异步线程池
├── constant/                     ← 枚举常量
├── controller/                   ← REST 控制器
├── domain/                       ← Entity
│   └── user/                     ← 用户域实体
├── dto/                          ← 入参对象，按领域分包
├── exception/                    ← QiwenException
├── mapper/                       ← MyBatis-Plus Mapper
├── office/                       ← OnlyOffice 文档服务
├── service/                      ← Service 实现
├── util/                         ← 工具类
└── vo/                           ← 出参对象，按领域分包

com.qiwenshare.ufop               ← 统一文件操作提供者
├── autoconfiguration/
├── config/
├── constant/
├── exception/
├── factory/
├── operation/                    ← 7 种操作 × 5 种存储实现
└── util/
```

## Spring Boot 3 要点

- 所有 `javax.*` 替换为 `jakarta.*`
- Spring Security 使用 Lambda DSL：
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
- `WebSecurityConfigurerAdapter` 已废弃
- SpringDoc 替代 Springfox

## 关键约定

### 新增 Service

1. `api/` 包创建接口 `IXxxService`，继承 `IService<T>`
2. `service/` 包创建实现类，继承 `ServiceImpl<XxxMapper, T>`
3. 构造器注入（`@RequiredArgsConstructor`），不用 `@Autowired`

### 新增 Controller

1. `@RestController` + `@RequestMapping("/xxx")`
2. `@Operation(summary = "xxx")` 标注方法
3. 入参 DTO + `@Valid`，出参 `RestResult<XxxVO>`

### 新增 Entity

1. 双注解：`@Entity` + `@Table` + `@TableName` + `@Data`
2. 主键 `@Id`，雪花算法生成
3. 日期字段用 `LocalDateTime`

### DTO/VO

- DTO 加 `@Schema` + Jakarta Bean Validation 注解
- VO 加 `@Schema(description, example)`
- 统一 `@Data`

### 异常处理

- 业务异常：`throw new QiwenException(code, message)`
- 全局处理器统一捕获
- UFOP 用操作异常子类

### 存储操作

- 通过 `UFOPFactory` 获取实例
- 不绕过 UFOP 直接操作存储
