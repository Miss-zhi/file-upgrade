# AGENTS.md — 后端模块（qiwen-file）

> 本文件是后端 Java 代码的模块级约定，补充根目录 AGENTS.md 中的通用规则。

## 包结构

```
com.qiwenshare.file
├── FileApplication.java          ← 启动类，不要修改
├── advice/                       ← 全局异常处理（GlobalExceptionHandlerAdvice）
├── aop/                          ← 日志切面（WebLogAcpect + @MyLog 注解）
├── api/                          ← Service 接口定义（IXxxService）
├── component/                    ← 业务组件（FileDealComp 等，含 ES 索引逻辑）
├── config/
│   ├── es/                       ← Elasticsearch 客户端配置
│   ├── jwt/                      ← JWT 属性绑定（JwtProperties, JwtHeader, JwtPayload, RegisterdClaims）
│   ├── mybitisplus/              ← MyBatis-Plus 分页插件配置
│   ├── onlyoffice/               ← OnlyOffice 集成配置
│   ├── openapi/                  ← Swagger/OpenAPI 配置
│   ├── security/                 ← Spring Security 配置
│   │   ├── SecurityConfig.java   ← 核心安全配置，修改需 Spec 授权
│   │   ├── entrypoint/           ← 401 处理
│   │   ├── filter/               ← JWT 过滤器 + URL 安全元数据
│   │   ├── handle/               ← 403 处理
│   │   └── manager/              ← URL 访问决策管理器
│   └── threadpool/               ← 异步线程池配置
├── constant/                     ← 枚举常量
├── controller/                   ← REST 控制器（11 个）
├── domain/                       ← Entity 实体类
│   └── user/                     ← 用户域实体（UserBean, Role, Permission, UserRole, RolePermission）
├── dto/                          ← 入参对象，按领域分包
├── io/                           ← QiwenFile 路径抽象
├── mapper/                       ← MyBatis-Plus Mapper 接口（继承 BaseMapper<T>）
├── office/                       ← OnlyOffice 文档服务集成（独立子模块）
│   ├── documentserver/           ← 回调、管理器、模型、序列化、存储、工具
│   ├── dto/                      ← Office 专用 DTO
│   ├── entities/                 ← Office 实体
│   ├── mappers/                  ← Office Mapper
│   ├── repositories/             ← JPA Repository
│   └── services/                 ← Office 服务 + 配置器
├── service/                      ← Service 实现类
├── util/                         ← 工具类
└── vo/                           ← 出参对象，按领域分包

com.qiwenshare.ufop               ← 统一文件操作提供者（存储抽象）
├── autoconfiguration/            ← 自动配置 + 属性类
├── config/                       ← 各存储后端配置（Aliyun, Minio, Qiniu）
├── constant/                     ← 存储类型枚举（StorageTypeEnum）
├── domain/                       ← 存储域对象
├── exception/                    ← UFOP 异常体系（基类 + 操作级子类）
├── factory/                      ← UFOPFactory 抽象工厂
├── operation/                    ← 7 种操作（copy/delete/download/preview/read/upload/write）
│   └── <op>/
│       ├── domain/               ← 操作域对象
│       └── product/              ← 5 种存储实现（Local/Aliyun/FastDFS/MinIO/Qiniu）
└── util/                         ← UFOP 工具类
```

## 关键约定

### 新增 Service

1. 在 `api/` 包创建接口 `IXxxService`，继承 `IService<T>`
2. 在 `service/` 包创建实现类 `XxxService`，继承 `ServiceImpl<XxxMapper, T>` 并实现 `IXxxService`
3. 方法加 `@Override`，需要事务的加 `@Transactional`

### 新增 Controller

1. 类注解：`@RestController` + `@RequestMapping("/xxx")`
2. 方法注解：`@GetMapping` / `@PostMapping` + `@Operation(summary = "xxx")`
3. 入参用 DTO，出参用 `RestResult<XxxVO>`
4. 不在 Controller 写业务逻辑，只做参数校验（`@Valid`）和 Service 调用

### 新增 Entity

1. 同时使用 JPA 和 MyBatis-Plus 注解：
   ```java
   @Entity
   @Table(name = "xxx")
   @TableName("xxx")
   @Data
   public class Xxx { ... }
   ```
2. 主键使用 `@Id`，不用 `@GeneratedValue`（ID 由雪花算法生成）
3. 日期字段使用 `String` 类型 + `DateUtil.getCurrentTime()`

### 新增 Mapper

1. 接口继承 `BaseMapper<T>`
2. 复杂 SQL 写在 `src/main/resources/mapper/XxxMapper.xml`
3. XML 中 namespace 必须与接口全限定名一致

### DTO/VO 规范

- DTO 用于接收前端参数，字段加 `@Schema(description = "xxx")` 注解
- VO 用于返回前端数据，字段加 `@Schema(description = "xxx", example = "xxx")` 注解
- 统一使用 `@Data` 生成 getter/setter
- DTO 中需要校验的字段加 `@NotNull` / `@NotBlank` 等 Bean Validation 注解

### 异常处理

- 业务异常：`throw new QiwenException(ResultCodeEnum.XXX)` 或 `throw new QiwenException(code, message)`
- 不要 catch 异常后返回错误码，让全局处理器统一处理
- UFOP 模块内的异常使用对应的操作异常子类（`UploadException`、`DownloadException` 等）

### 存储操作（UFOP）

- 通过 `UFOPFactory` 获取操作实例，不直接调用具体存储实现
- 新增存储类型需要实现全部 7 种操作（copy/delete/download/preview/read/upload/write）
- 不要绕过 UFOP 直接操作存储

## Mapper XML 位置

`src/main/resources/mapper/` 下的 XML 文件，文件名与 Mapper 接口对应。修改时注意 namespace 和 resultType 的正确性。

## 配置文件

- 主配置：`src/main/resources/config/application.properties`
- MyBatis-Plus 配置：`mybatis-plus.mapper-locations`、`mybatis-plus.type-aliases-package`
- 不要修改安全相关配置（JWT secret、数据库密码等）
