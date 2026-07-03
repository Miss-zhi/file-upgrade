# 规则演进日志

> 每次 AI 犯错后在此追加记录。这是 Harness Engineering 闭环的核心证据。
> 记录格式：日期 | 触发的错误 | 新增的规则 | 写入的 docs/ 文件
>
> 面试展示时，这张表的 git 提交历史就是"驾驭 AI 的成长史"。

---

| 日期 | 触发的错误 | 新增的规则 | 写入文件 |
|------|-----------|-----------|---------|
| 2026-06-30 | 初版创建 | 从旧项目 repo wiki 故障文档（8 个文件）提炼 32 条约束，覆盖启动配置、安全认证、数据完整性、上传存储、性能、容错降级、运维迁移 7 大类 | 全部 docs/ 文件 |
| 2026-07-01 | Bug: AuditLogAspect 中 @Async save 方法通过 this 调用，绕过 Spring AOP 代理，异步不生效 | 禁止 @Async/@Transactional 方法的同类内部调用，必须提取到独立 @Component | docs/BACKEND.md |
| 2026-07-01 | Bug: AdminUserService.listUsers 在循环中逐个查询角色，N+1 查询 O(N\*M\*K) | 循环内禁止逐条 findById，必须批量 findByXIn + findAllById 构建内存 Map | docs/BACKEND.md |
| 2026-07-01 | Issue: AuditLogAspect 将 userDetails.getUsername() 误当真实用户名写入审计日志 | 明确 getUsername() 存储 Snowflake 业务 ID，需额外查 DB 获取真实用户名 | docs/BACKEND.md |
| 2026-07-01 | Issue: SystemConfigService 在 @Transactional 内直接操作 Redis，回滚后缓存已失效 | 事务方法中的缓存失效必须用 TransactionSynchronizationManager.afterCommit | docs/BACKEND.md |
| 2026-07-01 | Issue: AdminControllerTest 缺少 @Mock AdminUserService，@InjectMocks 构造时 NPE | 新增 Service 依赖后必须同步更新测试 @Mock 列表 | docs/TESTING.md |
| 2026-07-01 | Bug: file-index-mapping.json 双层 mappings 包装导致 ES 索引创建失败 | TypeMapping.withJson() 期望 {"properties":{...}} 层级，禁止外层包 mappings | docs/BACKEND.md |
| 2026-07-01 | Bug: FileChangedListener + SearchIndexService 双重 @Async 浪费线程 | 事件 Listener 禁止标注 @Async，由 Service 方法的 @Async 独自负责异步 | docs/BACKEND.md |
| 2026-07-01 | Bug: rebuildAll() 内 N+1 查询，每个文件单独 findById FileBean | 全量重建必须批量 findAllById + 内存 Map 查找 | docs/BACKEND.md |
| 2026-07-01 | Bug: SearchModuleException 无专属异常处理器，全部被吞为 500 | 每个模块必须有专属 @RestControllerAdvice 按 ErrorCode 返回正确 HTTP 状态码 | docs/BACKEND.md |
| 2026-07-01 | Issue: FileOperationService/FileRecoveryService/FileUploadService 中 TransactionSynchronizationManager 未检查同步是否活跃，单元测试 IllegalStateException | Service 中使用 TransactionSynchronizationManager 必须先检查 isSynchronizationActive() | docs/TESTING.md |
| 2026-07-01 | Issue: ES Hit.source() 是 final 方法无法 Mockito mock | ES Java Client 测试聚焦异常路径，final 方法用 mock(Class) + lenient | docs/TESTING.md |
| 2026-07-01 | Bug: CallbackManagerTest status=2 断言 context.getErrorCode()==0（默认值），但 dispatch 实际返回 1（无匹配 handler），测试假绿 | Mock 断言必须验证实际执行路径，禁止断言默认值证明逻辑正确 | docs/TESTING.md |
| 2026-07-01 | Bug: SaveCallbackAsyncWriter @Async 方法内 this.updateFileBeanAndCreateVersion() 绕过 @Transactional 代理 | 同类内 @Transactional 自调用必须用 @Lazy 自注入通过代理 | docs/BACKEND.md |
| 2026-07-01 | Bug: DocumentCallbackController 读取 OnlyOffice JWT secret 但调用 parseCallbackToken 用应用级密钥验证，两个 secret 不同时回调全部 403 | 回调鉴权必须用 OnlyOffice 独立 secret 验证，新增 verifyOnlyOfficeJwt 方法 | docs/SECURITY.md |
| 2026-07-01 | Bug: DocumentPreviewService 文档 key 用 LocalDateTime.toString() + hashCode，纳秒精度不稳定且 32-bit hash 碰撞率高 | 文档 key 必须用 toEpochMilli() + SHA-256 保证稳定唯一 | docs/BACKEND.md |
| 2026-07-01 | Bug: DocumentController history 端点无权限检查，任何登录用户可获取任意文件版本历史（IDOR 漏洞） | 所有资源访问端点必须显式调用 FilePermissionService 校验 | docs/SECURITY.md |
| 2026-07-01 | Bug: DocumentEditService COW 在 @Transactional 内执行物理文件下载+写入（外部 IO），违反红线 #15 | 事务方法内禁止外部 IO，COW 物理文件复制必须在事务外执行，DB 写操作通过 @Lazy 自注入事务方法完成 | docs/DATA.md |
| 2026-07-01 | Bug: FilePermissionServiceImpl.hasValidShare 加载整张 share_file 表再 Java 过滤，N+1 查询（红线 #14） | Repository 必须提供精确查询方法，禁止 findAll + stream.filter 模式 | docs/BACKEND.md |
| 2026-07-01 | Issue: DocumentHistoryService.getVersion 返回 Entity 而非 VO，可能泄露到 Controller 层（红线 #4） | 返回 Entity 的 Service 方法必须设为包私有，禁止 public 返回 Entity | docs/BACKEND.md |
| 2026-07-01 | Bug: DocumentVersionRepository JPQL 中使用 LIMIT 1，不是合法 JPQL 语法 | JPQL 禁止使用 LIMIT，必须用 Spring Data 派生查询 findFirst 或 PageRequest | docs/DATA.md |
| 2026-07-01 | Issue: SaveCallbackHandler 有 12 个死 import 和 4 个未使用的构造注入依赖 | 重构后必须清理死 import 和未使用依赖，保持代码整洁 | docs/BACKEND.md |
| 2026-07-01 | Issue: DocumentCallbackController 返回 Map 而非 RestResult<T>，缺少协议豁免注释 | 非标准 REST 响应必须加注释说明协议级豁免原因 | docs/BACKEND.md |
| 2026-07-01 | Bug: DocumentAdminController 内 try-catch 吞异常（红线 #5），HTTP 健康检查逻辑在 Controller 层 | Controller 禁止 try-catch 吞异常，业务逻辑必须提取到 Service 层 | docs/BACKEND.md |
| 2026-07-01 | Bug: CorruptedCallbackHandler markError 返回 error=1 触发 OnlyOffice 重试，但损坏是永久性状态，导致 retry storm | 永久性错误（如文档损坏）必须返回 error=0 避免无意义重试 | docs/RESILIENCE.md |
| 2026-07-01 | Issue: DocumentAdminService/SaveCallbackAsyncWriter 每次调用新建 HttpClient，连接池无法复用 | HttpClient 等重量级对象必须作为 final 字段复用，禁止每次新建 | docs/BACKEND.md |
| 2026-07-01 | Issue: DocumentAdminService URL 拼接 serverUrl + "/healthcheck" 可能产生双斜杠 | URL 拼接必须处理末尾斜杠，防止双斜杠路径异常 | docs/BACKEND.md |
| 2026-07-01 | Issue: DocumentGlobalExceptionHandler 只 catch DocumentModuleException，缺参数校验/类型转换/兜底异常处理 | 模块级 ExceptionHandler 必须包含 MethodArgumentNotValidException、NumberFormatException、Exception 兜底 | docs/BACKEND.md |
| 2026-07-01 | Issue: CallbackManager 中 Handler 无 @Order，dispatch 顺序不确定 | Handler 链必须用 @Order 明确排序，Manager 构造时 AnnotationAwareOrderComparator.sort | docs/BACKEND.md |
| 2026-07-01 | Issue: CallbackManager status=1 编辑心跳用 INFO 日志，高频场景日志量爆炸 | 心跳/健康检查等高频回调必须用 DEBUG 级别 | docs/BACKEND.md |
| 2026-07-01 | Issue: DocumentCallbackController 注释说“无 Bearer 前缀”但代码防御性剥离 Bearer，两者矛盾 | 代码注释必须与实际行为一致，防御性处理需注释说明原因 | docs/BACKEND.md |
| 2026-07-01 | Issue: CallbackManagerTest 缺 SaveCallbackHandler dispatch 测试（status=2/6 核心保存路径零覆盖） | 核心业务路径必须有 dispatch 测试覆盖 | docs/TESTING.md |
| 2026-07-01 | Issue: DocumentEditServiceTest COW 测试未 verify storageBackend.write() 被调用 | 涉及外部 IO 的测试必须 verify IO 方法被实际调用 | docs/TESTING.md |
| 2026-07-01 | Issue: fastdfs-client:1.26.2 传递引入 mockito-all:1.9.5（compile scope），与 mockito-core:5.7.0 冲突，导致测试编译报"引用不明确"和"找不到符号" | 排除 fastdfs-client 中的 mockito-all 传递依赖，禁止同时静态导入 ArgumentMatchers.* 和 Mockito.* | docs/TESTING.md |

---

## 新增规则模板

发现 AI 犯错后，按以下步骤操作：

1. **在此文件追加一行**：填写日期、错误描述、新增规则摘要、对应的 docs/ 文件
2. **在对应的 docs/ 文件中添加详细规则**：说明规则内容、为什么需要、正反示例
3. **在 AGENTS.md 的硬性红线表中添加**（如果是禁止类规则）：一句话描述 + 指向 docs/ 文件
4. **git commit**：提交信息说明触发的错误和新增的规则

示例：

```
日期：2026-07-15
触发的错误：AI 在 FileService 中使用了 @Autowired 字段注入 UserMapper
新增的规则：在 BACKEND.md 依赖注入章节添加禁止字段注入的示例
写入文件：docs/BACKEND.md
AGENTS.md：已在硬性红线表 #2 中覆盖
```
