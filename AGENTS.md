# AGENTS.md — 奇文网盘项目工作手册

> 本文件是 AI Agent 在本项目中工作的权威指南。所有代码生成必须遵守以下约定。
> 本项目是从旧版（Java 8 / Vue 2）全面升级的新代码库，AI 根据 Spec 从零生成。

## 项目概述

奇文网盘（qiwen-file）是一个前后端分离的文件管理系统，提供文件上传下载、在线预览编辑、文件分享、全文检索、用户管理等功能。

## 技术栈（目标版本）

| 层 | 技术 | 版本 |
|---|---|---|
| JDK | Java | 17 (LTS) |
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus 3.5.x + JPA/Hibernate（双 ORM） | — |
| 安全 | Spring Security 6 + JWT（jjwt 0.12.x） | — |
| 搜索 | Elasticsearch Java Client | 8.12.x |
| 缓存 | Redis (Lettuce) | — |
| 存储抽象 | UFOP（自研，支持 Local/Aliyun OSS/FastDFS/MinIO/Qiniu） | — |
| 文档编辑 | OnlyOffice Document Server | — |
| 前端框架 | Vue 3 + Composition API | 3.4.x |
| UI 组件库 | Element Plus | 2.5.x |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | 2.x |
| 样式 | Stylus（保持） | — |
| 部署 | Docker Compose + Nginx | — |

## 目录结构

```
E:\file-upgrade\
├── AGENTS.md                    ← 本文件（项目级约定）
├── specs/                       ← SDD Spec 文件（驱动 AI 生成代码）
├── qiwen-file/                  ← 后端 Spring Boot 模块
│   ├── AGENTS.md                ← 后端模块级约定
│   ├── pom.xml
│   └── src/main/java/com/qiwenshare/
│       ├── file/                ← 主业务模块
│       └── ufop/                ← 存储抽象模块
├── qiwen-file-web/              ← 前端 Vue 3 模块
│   ├── AGENTS.md                ← 前端模块级约定
│   ├── package.json
│   └── src/
├── docker-compose.yml           ← OnlyOffice + MySQL + Redis 容器编排
└── .github/workflows/ci.yml    ← CI/CD 流水线
```

## 架构约束

### 后端分层

```
Controller → Service(api接口 + service实现) → Mapper(BaseMapper)
                ↓
            Domain(Entity) ← DTO(入参) / VO(出参)
```

- Controller 只做参数校验（`@Valid`）和响应封装，不写业务逻辑
- Service 接口定义在 `api/` 包，命名 `IXxxService`；实现类在 `service/` 包
- Mapper 继承 `BaseMapper<T>`，复杂 SQL 写在 `resources/mapper/*.xml`
- Entity 同时使用 JPA 注解（`@Entity`、`@Table`）和 MyBatis-Plus 注解（`@TableName`）
- 所有接口返回 `RestResult<T>` 统一响应体
- Spring Security 6 使用 Lambda DSL 风格配置，不用旧版 `WebSecurityConfigurerAdapter`

### 前端分层

```
View(页面组件) → Component(可复用组件) → api/(API调用) → stores/(Pinia状态管理)
```

- 页面组件在 `views/`，可复用组件在 `components/`
- API 调用按领域拆分到 `api/` 下的独立文件
- Pinia Store 在 `stores/` 目录，使用 Setup Store 风格
- 弹窗和浮层使用命令式服务模式（`createApp` + Promise）
- 全部使用 Composition API（`<script setup>`），不用 Options API

### 禁止事项

- **不要**使用 `javax.*` 包，Spring Boot 3 统一使用 `jakarta.*`
- **不要**使用 Options API（`data()`、`methods`、`computed`），统一用 `<script setup>` + Composition API
- **不要**引入 pom.xml / package.json 中不存在的新依赖，必须先说明理由
- **不要**删除或重命名任何已有的 API 端点路径
- **不要**修改 UFOP 存储抽象层的接口定义，除非同步更新所有 5 种存储实现
- **不要**在 Controller 中直接操作数据库，必须通过 Service 层
- **不要**在前端组件中直接操作 DOM，使用 `ref` 或自定义指令

## 编码规范

### 后端

- **ID 生成**：使用 Hutool 雪花算法 `IdUtil.getSnowflakeNextIdStr()`，不用自增 ID
- **日期处理**：使用 `java.time.LocalDateTime` + `DateTimeFormatter`，不再用 varchar 存日期
- **异常处理**：业务异常抛 `QiwenException(code, message)`，由全局 `GlobalExceptionHandlerAdvice` 统一捕获
- **日志**：使用 `@MyLog` 注解 + AOP 切面记录操作日志，不在业务代码中手动写日志
- **DTO/VO 命名**：DTO 按领域分包（`dto.file`、`dto.user`、`dto.sharefile`），VO 同理
- **Lombok**：Entity 和 DTO/VO 统一使用 `@Data`，Service 注入用构造器注入（`@RequiredArgsConstructor`），不用 `@Autowired`
- **JWT**：使用 `jjwt 0.12.x`（io.jsonwebtoken），不用旧的 `prime-jwt`

### 前端

- **缩进**：Tab（2 空格宽度）
- **引号**：单引号
- **分号**：不使用行尾分号
- **路径别名**：`@/` → `src/`，`_c/` → `components/`，`_v/` → `views/`，`_a/` → `assets/`，`_api/` → `api/`
- **组件命名**：PascalCase 文件名，kebab-case 模板引用
- **样式**：所有组件使用 `<style lang="stylus" scoped>`，Element Plus 覆盖用 `:deep()`
- **API 调用**：在 `api/` 目录定义，组件中 import 使用，使用 axios 实例
- **响应判断**：统一用 `if (res.success)` 判断请求成功
- **组合式函数**：可复用逻辑抽成 `useXxx()` 函数，放在 `composables/` 目录

## 测试要求

- 每个 Service 方法必须有对应的单元测试
- 测试类命名：`XxxServiceTest`，放在 `src/test/java` 对应包下
- 使用 `@SpringBootTest` + `@Transactional` 保证测试隔离
- 前端：组件测试使用 Vitest + Vue Test Utils，至少保证 `npm run lint` 和 `npm run build` 通过

## CI/CD 检查项

每次提交自动执行（见 `.github/workflows/ci.yml`），两条流水线并行运行：

**前端流水线**：lint → typecheck → build

1. ESLint 检查：`npx eslint src/ --ext .vue,.js,.ts`
2. 类型检查：`npx vue-tsc --noEmit`
3. 生产构建：`npx vite build`

**后端流水线**：compile → test → package

1. 编译：`mvn compile -f qiwen-file/pom.xml -B -DskipTests`
2. 单元测试：`mvn test -f qiwen-file/pom.xml`（CI 自动拉起 MySQL + Redis 容器）
3. 打包：`mvn package -f qiwen-file/pom.xml -DskipTests`

AI 生成代码后必须确保以上全部通过，否则自行修复后再提交。

## 错误→规则闭环

当 AI 在本项目犯错时，将根因沉淀为本文件的新规则。格式：

```
- **已犯错误**：[简述错误]
- **规则**：[新增约束]
- **日期**：[YYYY-MM-DD]
```

### 已积累规则

（初始为空，随使用逐步积累）
