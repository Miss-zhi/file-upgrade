# 后端编码规范

## 依赖注入

构造器注入，禁止 `@Autowired` 字段注入。多依赖时用 `@RequiredArgsConstructor`（Lombok）或手写构造器。

```java
// 正确
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final UFOPFactory ufopFactory;
}

// 禁止
@Service
public class FileService {
    @Autowired
    private FileRepository fileRepository;
}
```

## Lombok 使用

允许：`@Getter`、`@Setter`、`@Builder`、`@RequiredArgsConstructor`、`@ToString`、`@Slf4j`、`@EqualsAndHashCode(onlyExplicitlyIncluded = true)`。

禁止：`@Data`。它隐式包含 `@EqualsAndHashCode`，在 JPA Entity 上引发性能问题（懒加载触发全表扫描）和正确性问题（`hashCode` 在持久化前后变化导致 Set/Map 行为不一致）。

## Entity 与 DTO 分离

### JPA Entity

使用 `@Entity` + `@Table` + `@Column`（`jakarta.persistence`）。主键策略根据业务选择：自增 `@GeneratedValue(strategy = IDENTITY)` 或业务 ID（如 Snowflake）。

### MyBatis-Plus Entity

使用 `@TableName` + `@TableField`。两套 ORM 的注解**不混用在同一个类上**。如果同一领域模型需要同时支持 JPA 和 MyBatis-Plus，创建独立的映射类。

### DTO 和 VO

严格与 Entity 分离，禁止将 Entity 直接暴露给 Controller 返回值。在 DTO 上使用 `@Valid` + jakarta.validation 注解做参数校验：

```java
public record RegisterRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String telephone,
    @NotBlank @Size(min = 8, max = 30) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$") String password
) {}
```

## 异常处理

全局异常处理使用 `@RestControllerAdvice`，按异常类型分方法。Controller 和 Service 中不做 try-catch（除非有明确的资源释放需求），异常统一上抛。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public RestResult<Void> handleBusiness(BusinessException e) {
        return RestResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return RestResult.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public RestResult<Void> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return RestResult.fail("INTERNAL_ERROR", "服务器内部错误");
    }
}
```

自定义业务异常体系：

```java
public abstract class BusinessException extends RuntimeException {
    public abstract String getCode();
    public abstract int getHttpStatus();
}
```

每个模块定义自己的 ErrorCode 枚举（如 `AuthErrorCode`、`FileErrorCode`），继承 `BusinessException`。

## 事务管理

所有写操作的 Service 方法必须声明 `@Transactional(rollbackFor = Exception.class)`。

禁止同类内部方法调用带事务的方法（会绕过 Spring AOP 代理）。解决方案：
- `@Lazy` 自注入：`@Lazy @Autowired private XxxService self;` 然后 `self.transactionalMethod()`
- `ApplicationContext.getBean(XxxService.class).transactionalMethod()`

禁止在事务方法中吞掉异常。如果 catch 了异常，必须重新抛出或记录并处理。

避免在事务方法中执行外部 IO 调用（HTTP、文件 IO），这些操作耗时长，会长时间占用数据库连接。

## Javadoc

所有 `public` 的 Controller、Service、Repository 方法必须有 Javadoc，说明用途、参数含义、返回值和可能的异常。AI 生成代码时必须自动包含。

## 日志

使用 `@Slf4j`，关键业务操作记录 INFO 日志，异常记录 ERROR 日志。日志中**不打印**密码、token、密钥等敏感信息。

审计日志通过 `@OperationLog` 自定义注解 + AOP 切面实现，记录操作人、操作类型、目标资源、耗时、IP 地址。

## 异步任务

使用独立的线程池，不依赖 Spring 默认线程池：

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("fileTaskExecutor")
    public TaskExecutor fileTaskExecutor(
            @Value("${async.file.core-pool-size:4}") int core,
            @Value("${async.file.max-pool-size:8}") int max,
            @Value("${async.file.queue-capacity:100}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("file-task-");
        return executor;
    }
}
```

IO 密集型和 CPU 密集型任务使用不同的 `TaskExecutor`。线程池参数从 `application.yml` 读取。

## AOP 代理与自调用

Spring 的 `@Async`、`@Transactional`、`@Cacheable` 等注解依赖 AOP 代理生效。同一个类内部的方法调用走的是 `this` 引用，不经过代理，注解静默失效——方法仍然执行，但异步/事务/缓存行为被跳过。

**禁止在同一个类中调用带 `@Async` 或 `@Transactional` 注解的方法。** 解决方案：将异步/事务方法提取到独立的 `@Component`。

```java
// 禁止 — asyncSave 通过 this 调用，@Async 不生效，实际是同步执行
@Aspect
@Component
public class AuditLogAspect {
    private final OperationLogRepository repository;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint jp, AuditLog auditLog) throws Throwable {
        // ...
        asyncSave(opLog); // this.asyncSave() → 绕过代理
        return result;
    }

    @Async("fileTaskExecutor")
    private void asyncSave(OperationLog opLog) { repository.save(opLog); }
}

// 正确 — 提取到独立 @Component，通过代理调用
@Component
@RequiredArgsConstructor
public class OperationLogAsyncWriter {
    private final OperationLogRepository repository;

    @Async("fileTaskExecutor")
    public void save(OperationLog opLog) { repository.save(opLog); }
}

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final OperationLogAsyncWriter asyncWriter;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint jp, AuditLog auditLog) throws Throwable {
        // ...
        asyncWriter.save(opLog); // 通过 Spring 代理调用，@Async 生效
        return result;
    }
}
```

注意：`@Transactional` 的同类内部调用也是同样的问题，解决方式相同（提取到独立 Bean），或使用 `@Lazy` 自注入。

```java
// @Lazy 自注入：当 @Async 方法内需要调用同类的 @Transactional 方法时
@Component
public class SaveCallbackAsyncWriter {
    private final SaveCallbackAsyncWriter self; // @Lazy 自注入

    public SaveCallbackAsyncWriter(..., @Lazy SaveCallbackAsyncWriter self) {
        this.self = self;
    }

    @Async("fileTaskExecutor")
    public void asyncSave(...) {
        // 阶段一：无事务的外部 IO
        byte[] data = download();
        // 阶段二：通过代理调用事务方法
        self.updateInTransaction(...); // ✅ 通过 AOP 代理
    }

    @Transactional
    public void updateInTransaction(...) { ... } // ✅ 事务生效
}

// 禁止 — this 调用绕过代理
@Async
public void asyncSave(...) {
    updateInTransaction(...); // ❌ @Transactional 静默失效
}
```

详见事务管理章节。

## 查询性能

### 禁止 N+1 查询

在循环中逐条调用 `repository.findById()` 或 `repository.findByX()` 会产生 N 次数据库查询。当列表较大时（如分页 20 条 × 每条 2~3 次关联查询），单次请求可能产生 60+ 次 DB 调用。

**必须使用批量查询 + 内存 Map 替代循环内逐条查询。**

```java
// 禁止 — 每个用户 2 次查询，20 个用户 = 40 次 DB 调用
for (User user : users) {
    List<UserRole> roles = userRoleRepository.findByUserId(user.getId());
    for (UserRole ur : roles) {
        Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
        // ...
    }
}

// 正确 — 固定 2~3 次查询，与列表大小无关
List<Long> userPkIds = users.stream().map(User::getId).toList();
List<UserRole> allUserRoles = userRoleRepository.findByUserIdIn(userPkIds);   // 1 次
Set<Integer> roleIds = allUserRoles.stream().map(UserRole::getRoleId).collect(toSet());
Map<Integer, Role> roleMap = roleRepository.findAllById(roleIds).stream()      // 1 次
        .collect(Collectors.toMap(Role::getRoleId, r -> r));

// 内存中组装
Map<Long, List<String>> userRoleMap = new HashMap<>();
for (UserRole ur : allUserRoles) {
    Role role = roleMap.get(ur.getRoleId());
    if (role != null) {
        userRoleMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(role.getRoleName());
    }
}
```

Repository 中必须为常见的批量查询场景预定义方法：`findByXIn(Collection<T>)`、`findAllById(Collection<ID>)`。

### Snowflake ID 与 getUsername()

本项目 `UserDetailServiceImpl` 将 `user.getUserId()`（Snowflake 业务 ID）作为 Spring Security 的 `username` 传入 `User` 对象。因此 `UserDetails.getUsername()` 返回的是 Snowflake ID，不是真实用户名。

需要真实用户名时，必须通过 `userRepository.findByUserId(userId)` 查询，禁止假设 `getUsername()` 是真实用户名。

## 事务内缓存一致性

在 `@Transactional` 方法中直接操作 Redis（如 `evictCache`），如果事务后续步骤失败并回滚，Redis 已经被修改导致缓存与数据库不一致。

**事务方法中的 Redis 写操作/缓存失效必须延迟到事务提交后执行。**

```java
// 禁止 — 事务回滚后 Redis 已清除缓存，DB 与缓存不一致
@Transactional(rollbackFor = Exception.class)
public void updateConfig(String key, String value) {
    repository.save(new SystemParam(key, value));
    evictCache(key);  // 如果后续代码抛异常，事务回滚但缓存已清除
}

// 正确 — 事务提交后才清除缓存
@Transactional(rollbackFor = Exception.class)
public void updateConfig(String key, String value) {
    repository.save(new SystemParam(key, value));
    TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() { evictCache(key); }
            });
}
```

## Service 层 Entity 泄露防护

Service 方法返回 Entity 对象时，必须设为包私有（package-private）访问级别，禁止 public 返回 Entity。Entity→VO 转换必须在 Service 层内完成。

```java
// 禁止 — public 返回 Entity，可能被 Controller 直接使用
public DocumentVersion getVersion(Long userFileId, int versionNumber) {
    return repository.findByUserFileIdAndVersionNumber(userFileId, versionNumber)
            .orElseThrow(...);
}

// 正确 — 包私有，仅内部使用
DocumentVersion getVersion(Long userFileId, int versionNumber) {
    return repository.findByUserFileIdAndVersionNumber(userFileId, versionNumber)
            .orElseThrow(...);
}
```

## N+1 查询防护

Repository 必须提供精确的查询方法，禁止 `findAll()` + `stream().filter()` 模式。这种模式加载整张表再在 Java 中过滤，数据量大时性能灾难。

```java
// 禁止 — 加载整张表再 Java 过滤
List<ShareFile> shares = shareFileRepository.findAll().stream()
        .filter(s -> userFileId.equals(s.getUserFileId()))
        .filter(s -> s.getExpireTime() == null || s.getExpireTime().isAfter(LocalDateTime.now()))
        .toList();

// 正确 — 使用派生查询精确查询
long count = shareFileRepository.countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(
        userFileId, LocalDateTime.now());
```

## 代码整洁

重构后必须清理死 import 和未使用的构造注入依赖。IDE 通常提供“Optimize Imports”功能自动完成。

## REST 响应规范

所有 API 端点必须返回统一的 `RestResult<T>` 包装。如果因为外部协议要求（如 OnlyOffice 回调）需要返回非标准格式，必须在 Javadoc 中明确注释说明豁免原因。

```java
/**
 * 接收 OnlyOffice 状态回调。
 *
 * <p><b>注意：</b>此端点返回 {@code Map<String, Integer>} 而非项目统一的 {@code RestResult<T>}，
 * 因为 OnlyOffice Document Server 协议要求回调响应必须为 {@code {"error": 0|1}} 格式，
 * 属于 REST 规范的协议级豁免。</p>
 */
@PostMapping("/callback")
public ResponseEntity<Map<String, Integer>> callback(...) { ... }
```

## 资源复用

`HttpClient`、`ObjectMapper` 等重量级对象必须复用，禁止每次调用时新建。

```java
// 禁止 — 每次请求新建 HttpClient，连接池无法复用
public byte[] download(String url) {
    HttpClient client = HttpClient.newBuilder().build(); // ❌ 每次新建
    return client.send(...).body();
}

// 正确 — 作为 final 字段复用
private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

public byte[] download(String url) {
    return httpClient.send(...).body(); // ✅ 复用连接池
}
```

## URL 拼接安全

拼接 URL 时必须处理末尾斜杠，防止双斜杠 `//` 导致路径异常。

```java
// 禁止 — serverUrl 末尾有斜杠时变成 //healthcheck
URI.create(serverUrl + "/healthcheck");

// 正确 — 先规范化
String normalizedUrl = serverUrl.endsWith("/")
        ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
URI.create(normalizedUrl + "/healthcheck");
```

## Handler 链排序

当多个 Handler 实现同一接口并由 Manager 按列表顺序分发时，必须使用 `@Order` 注解明确排序，并在 Manager 构造时调用 `AnnotationAwareOrderComparator.sort()`。

```java
// 正确 — 每个 Handler 标注 @Order
@Component @Order(1) public class EditingHandler implements CallbackStatusHandler { ... }
@Component @Order(2) public class SaveHandler implements CallbackStatusHandler { ... }

// Manager 构造时排序
public CallbackManager(List<CallbackStatusHandler> handlers) {
    List<CallbackStatusHandler> sorted = new ArrayList<>(handlers);
    AnnotationAwareOrderComparator.sort(sorted);
    this.handlers = sorted;
}
```

## 注释与代码一致性

代码注释必须与实际代码行为一致。如果注释说"无 Bearer 前缀"但代码防御性剥离 Bearer，必须更新注释说明防御性处理的原因。

```java
// 禁止 — 注释与代码矛盾
// OnlyOffice JWT 在 header 中直接传递（无 Bearer 前缀）
String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

// 正确 — 注释解释防御性处理的原因
// OnlyOffice 协议规定 JWT 直接传递（无 Bearer 前缀），
// 但为兼容网关/代理可能添加的 Bearer，做防御性剥离
String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
```

## 高频日志级别控制

心跳、健康检查等高频回调必须使用 DEBUG 级别，避免 INFO 日志量爆炸。

```java
// 禁止 — status=1 编辑心跳每秒多次，INFO 级别日志量爆炸
log.info("分发回调: status={}, userFileId={}", status, context.getUserFileId());

// 正确 — 高频心跳降为 DEBUG
if (status == 1) {
    log.debug("分发回调: status={}, userFileId={}", status, context.getUserFileId());
} else {
    log.info("分发回调: status={}, userFileId={}", status, context.getUserFileId());
}
```

## OnlyOffice 编辑模式判定

OnlyOffice 6.x 仅能原生保存 OOXML 格式（docx/xlsx/pptx/csv/txt），无法保存旧二进制格式（doc/xls/ppt）。
对不支持原生保存的格式强制 edit 模式会导致 status=2 保存回调永不发送，用户看到"文件无法保存"。

### 规则

1. **预览端点（`/preview`）始终以 view 模式打开**——不管文件格式和权限。编辑由独立的 `/edit` 端点处理。
2. **编辑端点（`/edit`）仅对 `editedExtensions` 中的格式使用 edit 模式**。`convertExtensions` 中的格式（doc/xls/ppt 等）降级为 view。

```java
// 禁止 — 对 convertible 格式强制 edit，OnlyOffice 无法保存 .doc
if (!isEditable && !isConvertible) { /* 降级预览 */ }
buildConfig(userFile, fileBean, "edit", userId);  // .doc 也被强制 edit！

// 正确 — 仅 editedExtensions 使用 edit 模式
if (!isEditable) { /* 降级预览 */ }
buildConfig(userFile, fileBean, "edit", userId);  // 只有 docx/xlsx/pptx/csv/txt 到达此处
```

## 新建 Office 文档必须使用模板

"新建 Word / Excel / PPT" 功能**必须**从 classpath 下的模板文件读取真实内容，上传到存储后端并创建 FileBean，再关联到 UserFile。禁止创建 fileId=null 的"空文件记录"。

**原因**：fileId=null 或 fileSize=0 的文件在 OnlyOffice 编辑器打开时会报错（500 或 "Document cannot be opened"），因为 OnlyOffice 需要真实的 OOXML 字节流才能解析。

### 模板映射

```java
// 禁止 — 创建空文件记录（fileId=null），OnlyOffice 无法打开
userFile.setFileId(null);
userFileRepository.save(userFile);  // ❌ 0B 空文件，预览 500，编辑失败

// 正确 — 使用模板 + UFOP 上传
private static final Map<String, String> TEMPLATE_MAP = Map.of(
    "docx", "static/template/Word.docx",
    "xlsx", "static/template/Excel.xlsx",
    "pptx", "static/template/PowerPoint.pptx"
);
// 1. 读取模板字节
byte[] templateBytes = classLoader.getResourceAsStream(templatePath).readAllBytes();
// 2. 计算 SHA-256 hash，检查去重
// 3. 上传到 StorageBackend
// 4. 创建 FileBean（fileSize/hash/storagePath/storageType）
// 5. 关联 UserFile.fileId = fileBean.getFileId()
```

### 模板文件位置

模板必须放在 `src/main/resources/static/template/` 下，且必须是**有效的 OOXML ZIP 包**（不能是 0 字节占位文件）。
Word.docx 如果上游模板缺失或为 0 字节，必须用 python-zipfile 手写最小 OOXML 结构生成（`[Content_Types].xml` + `_rels/.rels` + `word/document.xml` + `word/styles.xml`）。

## 秒传端点必须优雅降级

`/api/v1/filetransfer/upload/speed` 端点的唯一职责是"探测是否可复用现有 FileBean"。
**所有失败场景**（hash 未命中、同名冲突、配额不足、参数校验通过但业务失败）都必须返回 `RestResult.success("...", null)`，引导前端走普通上传，**禁止**向上传抛出 FileModuleException 导致前端收到 400/500 而中断上传流程。

```java
// 禁止 — 仅处理 FILE_NOT_FOUND，其他异常向上抛，前端收到 400/500
try {
    UploadFileVO result = fileUploadService.speedUpload(dto, userId);
    return RestResult.success(result);
} catch (FileModuleException e) {
    if (e.getErrorCode() == FileErrorCode.FILE_NOT_FOUND) {
        return RestResult.success("需要普通上传", null);
    }
    throw e;  // ❌ UPLOAD_DUPLICATE / QUOTA_EXCEEDED 会让前端报错
}

// 正确 — 任何失败都降级为普通上传回退信号
try {
    UploadFileVO result = fileUploadService.speedUpload(dto, userId);
    if (result == null) {
        return RestResult.success("秒传未命中，请走普通上传", null);
    }
    return RestResult.success(result);
} catch (FileModuleException e) {
    log.info("秒传降级为普通上传: code={}, msg={}", e.getErrorCode(), e.getMessage());
    return RestResult.success("秒传失败，请走普通上传", null);
}
```

对应的 `SpeedUploadDTO.fileSize` 和 `ChunkUploadInitDTO.fileSize` 必须用 `@PositiveOrZero`（而非 `@Positive`），允许 0 字节文件通过秒传探测（0 字节 hash 几乎不会命中去重，会自然降级到普通上传）。
