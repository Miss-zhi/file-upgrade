## Context

storage 模块是奇文网盘的 UFOP（统一文件操作平台）框架，为 file、search、document 等模块提供文件读写抽象。当前状态：

- **Local 后端**：完整实现（12/12 方法），但存在路径遍历安全漏洞（`resolvePath()` 无校验）和 `downloadRange()` 资源泄露
- **MinIO / Aliyun OSS / Qiniu / FastDFS**：全部 stub 实现（方法体抛 `UnsupportedOperationException`），无 `@Component`、无 AutoConfiguration、无 SDK 依赖
- **StorageFactory**：支持按类型获取后端，但缺少按文件级 `storageType` 路由能力
- **测试**：仅 Local 后端有基础测试，无 StorageFactory / StorageHealthChecker 测试

现有接口设计（1 个 `StorageBackend` 组合接口 + 7 个子接口 + 12 个方法）保持扁平，优于旧项目 35 个 product 类方案，保持不变。

## Goals / Non-Goals

**Goals:**

- 修复 Local 后端安全漏洞（路径遍历防护）和资源泄露（downloadRange try-with-resources）
- 实现 4 个云存储后端的完整 StorageBackend（MinIO、Aliyun OSS、Qiniu、FastDFS）
- 每个后端配套 AutoConfiguration + SDK 依赖 + AutoConfiguration.imports 注册
- SDK 客户端作为单例 Bean 复用，禁止每次操作 new 客户端
- StorageFactory 增加 `getBackendForStorageType()` 按文件级存储类型路由
- 补充 StorageFactory、StorageHealthChecker 单元测试
- 所有后端配置从 `StorageProperties` 读取，禁止硬编码凭证

**Non-Goals:**

- 分片上传的 Redis 协调机制（属于 file 模块）
- 存储后端热切换（运行时动态切换 storage.type）
- 缩略图生成 / 图片处理
- 对象存储跨区域复制
- 存储用量统计/计费（属于 admin 模块）

## Decisions

### D1: 保持扁平 StorageBackend 组合接口，不拆分操作接口

**决定**：维持现有 1 个 `StorageBackend` 继承 7 个子接口的设计，不拆分为独立操作接口。

**理由**：旧项目按操作拆分为 7 抽象类 × 5 后端 = 35 product 类，导致类爆炸。新项目的组合接口更简洁，所有后端必须实现全部操作（即使某些操作是降级实现如 FastDFS 的 `getPreviewUrl()` 返回 null）。

**替代方案**：拆分为独立 Uploader/Downloader 等 Bean → 增加复杂度但无实质收益，放弃。

### D2: SDK 客户端单例 Bean + AutoConfiguration 条件注册

**决定**：每个后端一个 `@AutoConfiguration` 类，使用 `@ConditionalOnProperty(name = "storage.type", havingValue = "xxx")` 条件激活。SDK 客户端作为单例 `@Bean`。

**理由**：
- 未激活的后端不创建任何 Bean，零运行时开销
- 单例客户端复用连接池（旧项目每次 `new` 客户端是反模式）
- Spring Boot 3 标准 AutoConfiguration 模式，与 `AutoConfiguration.imports` 配合

**替代方案**：所有后端 Bean 都创建、运行时按 type 选择 → 浪费资源且可能因缺少凭证导致启动失败，放弃。

### D3: Aliyun OSS 客户端生命周期管理

**决定**：通过 `@Bean(destroyMethod = "shutdown")` 声明 OSS 客户端 Bean，Spring 容器关闭时自动调用 `shutdown()`。

**理由**：阿里云 OSS SDK 内部维护连接池，不 shutdown 会导致连接泄露。`@Bean(destroyMethod)` 是最简洁的声明方式，无需 AutoConfiguration 实现 `DisposableBean` 接口。

### D4: FastDFS 降级策略

**决定**：
- `downloadRange()`：FastDFS 无原生 range API，下载全量后截取。>50MB 文件记录 WARN 日志
- `getPreviewUrl()`：返回 null（FastDFS 无内置 HTTP 预览，需配合 Nginx）
- `copy()`：下载源文件 → 重新上传（FastDFS 无服务端 copy）

**理由**：FastDFS 功能受限是客观事实，降级实现保证接口契约不打破，调用方需处理 null preview URL。

### D5: StorageFactory 按文件路由策略

**决定**：新增 `getBackendForStorageType(String storageType)` 方法，接受存储类型字符串而非 `FileBean` 对象。当文件的 `storageType` 对应后端未注册时，fallback 到全局活跃后端并 `log.warn`。

**理由**：支持存储迁移场景——全局切换到新后端后，旧文件仍可通过文件级 storageType 正确访问。方法参数使用 `String storageType` 而非 `FileBean`，避免 storage 模块反向依赖 file 模块（架构边界：storage 是底层模块，只被上层调用）。调用方从 `fileBean.getStorageType()` 取值后传入。fallback 策略保证系统不因旧文件配置缺失而不可用。

### D6: 异常处理统一为 UncheckedIOException

**决定**：所有后端的 IO 操作失败统一抛 `UncheckedIOException`，与 Local 后端现有行为一致。

**理由**：StorageBackend 接口方法不声明 checked exception，使用 `UncheckedIOException` 保留原始 IOException 信息，便于上层统一处理。

### D7: 测试策略——Mock 为主，集成测试可选

**决定**：
- 4 个云存储后端使用 Mockito Mock SDK 客户端，验证方法调用和参数传递
- Local 后端使用临时目录（`@TempDir`），补充路径遍历安全测试
- StorageFactory / StorageHealthChecker 使用 Mock Backend
- 集成测试（Testcontainers MinIO）标记 `@Tag("integration")`，默认跳过

**理由**：真实云存储服务不可用于 CI 环境，Mock 测试覆盖核心逻辑。MinIO 作为 S3 兼容存储可通过 Testcontainers 做集成验证。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| SDK 版本与云服务商 API 不兼容 | 锁定 SDK 版本，定期更新；集成测试验证基本连通性 |
| FastDFS downloadRange 大文件性能 | WARN 日志提醒调用方；后续可引入 CDN 层缓存 |
| 按文件路由时 fallback 到全局后端可能导致文件找不到 | log.warn 记录 fallback 事件，便于运维排查 |
| 4 个 SDK 依赖增加 pom.xml 体积和潜在冲突 | 使用 Maven dependencyManagement 统一版本；排除不必要的传递依赖 |
| Local 后端路径遍历修复可能影响现有合法相对路径 | `normalize() + startsWith()` 只拦截真正逃逸的路径，合法路径不受影响 |
