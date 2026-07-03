# AGENTS.md — 奇文网盘技术升级项目

> Harness Engineering 核心 Guide（前馈控制）。AI 编码代理生成代码前必须读取本文件。
> 详细规则按需查阅 `docs/` 目录下的专题文档。犯错后必须执行末尾的反馈闭环协议。

---

## 项目概述

奇文网盘（QiWen File）企业级文件管理系统，从 Spring Boot 2 + Vue 2 全栈重写到 Spring Boot 3 + Vue 3。
旧项目 `E:\file`，Qoder repo wiki 在 `E:\file\.qoder\repowiki\`（115 文档），理解旧系统行为时查阅。
新项目 `E:\file-upgrade`，只放 Harness 骨架和 AI 生成的新代码，不复制旧源码。
功能规格定义在 `specs/{module}/`，每个模块包含 proposal → specs → design → tasks 四份文档。

---

## 技术栈约束

### 后端

| 技术 | 版本 | 要点 |
|------|------|------|
| Java | 17+ | records / sealed classes / text blocks |
| Spring Boot | 3.2.x | Spring Framework 6.1 |
| Spring Security | 6.x | SecurityFilterChain，无 WebSecurityConfigurerAdapter |
| JPA (Hibernate) | 6.x | 主 ORM，领域模型映射 |
| MyBatis-Plus | 3.5.x | 辅 ORM，复杂查询和批量操作 |
| Elasticsearch | 8.x Java API Client | 禁止 RestHighLevelClient |
| JWT | jjwt 0.12.x | API 与 0.9.x 有重大变化 |
| MySQL | 8.x | 主数据库 |
| Redis | 7.x | 缓存 / 分布式锁 / 会话 |
| OnlyOffice | 文档服务 | 安全令牌 + SSL 控制 |

### 前端

| 技术 | 版本 | 要点 |
|------|------|------|
| Vue | 3.4+ | Composition API + `<script setup lang="ts">` |
| Element Plus | 最新 | 替代 Element UI |
| Vite | 5.x | 替代 Webpack |
| Pinia | 最新 | 替代 Vuex |
| TypeScript | 5.x | 强制，禁止 `any` |

---

## 架构边界

前后端分离，REST API 统一前缀 `/api/v1`，响应统一包装 `RestResult<T>`。
后端严格三层：Controller（参数校验 + 响应包装）→ Service（业务逻辑）→ Repository（数据访问）。禁止跨层调用。

```
后端模块：auth/ file/ transfer/ share/ recovery/ storage/ search/ document/ admin/ notice/ common/ config/
前端目录：api/ components/ composables/ layouts/ router/ stores/ types/ utils/ views/
```

UFOP 统一文件操作框架：工厂模式抽象 5 种存储后端（本地/MinIO/OSS/七牛/FastDFS），独立 Starter 模块，auto-config 注册在 `META-INF/spring/...AutoConfiguration.imports`（非 `spring.factories`）。
模块间通过 Service 接口调用，禁止 Controller 跨模块调用。共享 DTO/VO/Entity 放 `common/`。

---

## 硬性红线（完整规则见各 docs/ 文件）

| # | 禁止事项 | 详见 |
|---|---------|------|
| 1 | `javax.*` 命名空间（必须用 `jakarta.*`） | [MIGRATION.md](docs/MIGRATION.md) |
| 2 | `@Autowired` 字段注入 | [BACKEND.md](docs/BACKEND.md) |
| 3 | Lombok `@Data` | [BACKEND.md](docs/BACKEND.md) |
| 4 | Entity 暴露给 API 返回值 | [BACKEND.md](docs/BACKEND.md) |
| 5 | Controller 中 try-catch 吞异常 | [BACKEND.md](docs/BACKEND.md) |
| 6 | Controller 直接调 Repository/Mapper | [BACKEND.md](docs/BACKEND.md) |
| 7 | 生产环境 `ddl-auto=update` | [DATA.md](docs/DATA.md) |
| 8 | 同类内部调用 `@Transactional` 方法 | [DATA.md](docs/DATA.md) |
| 9 | 前端 `any` 类型 | [FRONTEND.md](docs/FRONTEND.md) |
| 10 | 前端 Options API / `this.$store` | [FRONTEND.md](docs/FRONTEND.md) |
| 11 | 硬编码密钥 / 密码 / 凭证 | [SECURITY.md](docs/SECURITY.md) |
| 12 | 无限阻塞的外部调用 | [RESILIENCE.md](docs/RESILIENCE.md) |
| 13 | 上传错误笼统包装为"超时" | [FILE-STORAGE.md](docs/FILE-STORAGE.md) |
| 14 | N+1 查询模式 | [BACKEND.md](docs/BACKEND.md) |
| 15 | 事务方法中执行外部 IO | [DATA.md](docs/DATA.md) |
| 16 | `@Async` / `@Transactional` 方法同类内部调用（绕过 AOP 代理） | [BACKEND.md](docs/BACKEND.md) |

---

## 详细文档索引

| 主题 | 文件 | 内容概要 |
|------|------|---------|
| 后端编码规范 | [docs/BACKEND.md](docs/BACKEND.md) | 依赖注入、Lombok、Entity/DTO 分离、异常处理、事务、Javadoc、日志 |
| 前端编码规范 | [docs/FRONTEND.md](docs/FRONTEND.md) | 组件风格、类型安全、API 请求层、Pinia 状态管理、样式 |
| 安全与认证 | [docs/SECURITY.md](docs/SECURITY.md) | JWT 密钥管理、双 token 机制、RBAC 权限模型、CORS、公开端点白名单 |
| 文件上传与存储 | [docs/FILE-STORAGE.md](docs/FILE-STORAGE.md) | 三层大小限制、配额校验、分片上传、文件去重、错误码、存储后端验证 |
| 数据完整性 | [docs/DATA.md](docs/DATA.md) | 查询性能、死锁预防、Schema 迁移（Flyway）、事务安全 |
| API 韧性 | [docs/RESILIENCE.md](docs/RESILIENCE.md) | 外部服务降级策略、超时/重试/熔断、健康检查、回调连通性 |
| 部署运维 | [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | 启动依赖验证、启动顺序、密钥同步、CI Pipeline、环境配置 |
| 技术迁移规则 | [docs/MIGRATION.md](docs/MIGRATION.md) | javax→jakarta 对照表、Security 6 迁移、Vue 3 迁移、UFOP starter 迁移 |
| 测试规范 | [docs/TESTING.md](docs/TESTING.md) | 后端/前端测试要求、覆盖率目标、命名规范、Testcontainers |
| 规则演进日志 | [docs/CHANGELOG.md](docs/CHANGELOG.md) | 每次 AI 犯错后追加：日期 + 错误 + 新增规则 |

---

## 参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 旧项目 Repo Wiki | `E:\file\.qoder\repowiki\` | 理解旧系统行为和设计意图 |
| OpenSpec 规格 | `specs/{module}/` | 每个模块的 proposal → specs → design → tasks |
| Phase 0 考古成果 | `docs/phase0/` | API 清单、模块依赖图、暗知识约束映射 |

---

## 反馈闭环协议（每次修复错误后必须执行）

> **黄金法则：不要只修代码，要修 Harness。**
> 每当 AI 犯错，修复代码只是第一步——必须同步加固约束文件，使同类错误永不再犯。

### 触发条件

当你在编码过程中遇到以下任一情况，修复代码后必须执行完整闭环：

- 编译失败或测试失败，且根因是 AGENTS.md 或 docs/ 中未覆盖的反模式
- design.md / tasks.md / spec.md 中的端点、字段名、类名与实际代码不一致
- 你在同一会话中重复修正同类问题（说明规则缺失）
- 新增依赖后导致已有测试编译失败（Mock 注入未同步更新）

### 闭环四步（缺一不可）

1. **修代码** — 解决当前问题，确保编译通过、测试绿灯
2. **补规则** — 在对应的 `docs/` 文件中添加规则条目，必须包含：
   - 禁止什么（一句话概括）
   - 为什么禁止（一两句话说明后果）
   - 反面示例（错误写法，用代码块）
   - 正面示例（正确写法，用代码块）
3. **记日志** — 在 [docs/CHANGELOG.md](docs/CHANGELOG.md) 表格追加一行：
   `| 日期 | 触发的错误 | 新增的规则 | 写入文件 |`
4. **更新红线** — 如果是全局性禁令（不只是单一模块的细节），同步添加到上方"硬性红线"表格

### 规则归属速查

| 错误类型 | 写入文件 |
|---------|---------|
| Controller 路径/命名风格不一致 | docs/BACKEND.md |
| Entity / DTO / VO 混淆或泄露 | docs/BACKEND.md |
| 依赖注入方式错误 | docs/BACKEND.md |
| 事务 / 查询 / 死锁 / Flyway | docs/DATA.md |
| JWT / 权限 / CORS / RBAC | docs/SECURITY.md |
| 上传 / 存储 / 配额 / 分片 | docs/FILE-STORAGE.md |
| 外部调用 / 超时 / 降级 / 熔断 | docs/RESILIENCE.md |
| javax / jakarta / Spring 版本迁移 | docs/MIGRATION.md |
| 前端组件 / 类型 / 状态管理 | docs/FRONTEND.md |
| 测试 Mock / 命名 / 结构 | docs/TESTING.md |
| 部署 / 启动顺序 / 环境变量 | docs/DEPLOYMENT.md |
| 文档与代码不一致（design/tasks/spec） | docs/BACKEND.md + 对应 OpenSpec 文件 |

### 示例

```
场景：AI 在 FileShareService 构造函数中新增了 RedisTemplate 参数，
      但 FileShareServiceTest 的手动构造未同步更新，编译失败。

步骤1 修代码：在测试中添加 @Mock StringRedisTemplate 并更新构造参数
步骤2 补规则：在 docs/TESTING.md 添加条目——
  "禁止手动 new Service：当 Service 有依赖注入时，必须使用 @InjectMocks
   或手动构造时确保参数列表与 Service 构造函数完全一致。
   反面：new FileShareService(repo, userFileRepo)  // 漏了新增的 redisTemplate
   正面：使用 @InjectMocks + @Mock，自动适配构造函数变化"
步骤3 记日志：在 CHANGELOG.md 追加一行
步骤4 判断：这是测试规范，非全局禁令，不需要更新红线表
```
