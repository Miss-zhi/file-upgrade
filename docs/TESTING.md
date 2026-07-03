# 测试规范

## 后端测试要求

### 覆盖率目标

- Service 层单元测试覆盖率 > 80%
- Controller 层集成测试覆盖所有公开端点
- Repository 层使用 `@DataJpaTest` 覆盖自定义查询

### 测试框架

- 单元测试：JUnit 5 + Mockito
- 集成测试：`@SpringBootTest` + `MockMvc` + Testcontainers
- Repository 测试：`@DataJpaTest` + H2 内存数据库或 Testcontainers MySQL
- 安全测试：`@SpringBootTest` + `@AutoConfigureMockMvc` + Spring Security Test

### Testcontainers 配置

```java
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class BaseIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("qiwenshare_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

### 安全模块专项测试

认证流程、权限校验、token 刷新必须有集成测试：

- 注册 → 登录 → 获取用户信息 → 修改密码 → 登出（完整链路）
- Token 过期 → 401
- Token 黑名单 → 401
- 全局撤销后旧 token → 401
- Refresh token rotation 成功
- Refresh token 重用检测 → 401 + 全部 token 失效
- 权限不足 → 403
- 管理员重置密码 → 用户 token 失效
- MD5 → BCrypt 透明迁移（首次登录旧账户）

权限继承链必须有专门的测试用例，覆盖多级继承场景。

### 测试命名

后端：`should_预期行为_when_条件()`

```java
@Test
void should_return_401_when_token_expired() { ... }

@Test
void should_return_user_info_when_valid_token() { ... }

@Test
void should_revoke_all_tokens_when_password_changed() { ... }
```

## 前端测试要求

### 测试框架

- Vitest（测试运行器，兼容 Jest API）
- Vue Test Utils（组件测试）
- MSW (Mock Service Worker) 或 axios-mock-adapter（API mock）

### 测试范围

- 组件单元测试：Props、Events、Slots、生命周期
- API 请求层 mock 测试：请求参数、响应处理、错误处理
- 路由守卫测试：认证跳转逻辑
- Store 测试：状态变更、异步 action

### 测试命名

```typescript
describe('AuthStore', () => {
  it('should set user after successful login', async () => { ... })
  it('should clear user after logout', async () => { ... })
  it('should redirect to login when fetchMe fails', async () => { ... })
})

describe('LoginView', () => {
  it('should show error message when login fails', async () => { ... })
  it('should disable submit button while loading', async () => { ... })
})
```

### 事务同步感知

当 Service 方法使用 `TransactionSynchronizationManager.registerSynchronization()` 在事务提交后发布事件时，单元测试中事务同步可能未激活，导致 `IllegalStateException`。

**禁止**：在单元测试中假设事务同步始终活跃。

**正确做法**：Service 代码中必须检查 `TransactionSynchronizationManager.isSynchronizationActive()`，未激活时直接执行（不注册 afterCommit 回调）。

```java
// 反面：直接注册，单元测试必崩
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override public void afterCommit() {
            eventPublisher.publishEvent(event);
        }
    });

// 正面：先检查是否活跃
if (TransactionSynchronizationManager.isSynchronizationActive()) {
    TransactionSynchronizationManager.registerSynchronization(...);
} else {
    eventPublisher.publishEvent(event);
}
```

### Mock 断言必须验证实际执行路径

禁止断言默认值或未被 mock 影响的字段来“证明”逻辑正确。必须断言方法的实际返回值或验证调用了正确的 mock。

**反面**：
```java
// status=2 不匹配任何 handler，dispatch 返回 1（未匹配）
// 但测试断言 context.getErrorCode() == 0（默认值），忽略了实际返回值
callbackManager.dispatch(context);
assertThat(context.getErrorCode()).isEqualTo(0); // 假绿！
```

**正面**：
```java
// 断言 dispatch 的实际返回值，确认是否匹配到 handler
int result = callbackManager.dispatch(context);
assertThat(result).isEqualTo(1); // 正确：status=2 无匹配处理器
```

### 核心路径必须有 IO 验证测试

当 Service 方法涉及外部 IO（文件写入、HTTP 调用）时，测试必须 `verify` IO 方法被实际调用，而不是仅断言返回值。

```java
// 禁止 — 仅断言 COW 标志，未验证物理文件复制是否执行
assertThat(result.isCowApplied()).isTrue();
verify(self).saveCowCopy(eq(userFile), any(FileBean.class));
// 缺少对 storageBackend.write() 的验证

// 正确 — 同时验证 IO 调用
assertThat(result.isCowApplied()).isTrue();
verify(self).saveCowCopy(eq(userFile), any(FileBean.class));
verify(storageBackend).write(anyString(), any(InputStream.class)); // ✅ 验证物理文件复制
```

### ES Java Client 测试注意事项

`Hit.source()` 是 final 方法，标准 Mockito 无法 mock。搜索模块的单元测试应聚焦异常路径和边界条件，正常解析路径通过集成测试覆盖。

`BooleanResponse` 位于 `co.elastic.clients.transport.endpoints` 包（非 `core` 包），使用 `mock(BooleanResponse.class)` 代替构造。

### Mockito 静态导入规则

禁止同时静态导入 `org.mockito.ArgumentMatchers.*` 和 `org.mockito.Mockito.*`。`Mockito` 类已继承 `ArgumentMatchers`，`import static org.mockito.Mockito.*` 已包含所有 matcher 方法（`any()`、`eq()`、`anyString()` 等）。同时导入两者会导致编译器报"对 X 的引用不明确"。

```
// 错误 — 同时导入两个包导致歧义
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 正确 — 只导入 Mockito.*
import static org.mockito.Mockito.*;
```

### 第三方依赖传递引入 mockito-all 冲突

`com.github.tobato:fastdfs-client` 传递依赖 `org.mockito:mockito-all:1.9.5`（compile scope），与 Spring Boot 管理的 `mockito-core:5.x` 冲突。必须在 pom.xml 中排除：

```xml
<dependency>
    <groupId>com.github.tobato</groupId>
    <artifactId>fastdfs-client</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-all</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

新增第三方依赖后，必须用 `mvn dependency:tree` 检查是否传递引入了冲突的测试库版本。

## 性能基线

核心 API 设性能基线，CI pipeline 对比基线，回归超过 20% 则构建失败：

| API | P50 基线 | P95 基线 |
|-----|---------|---------|
| POST /api/v1/auth/login | < 200ms | < 500ms |
| GET /api/v1/auth/me | < 50ms | < 100ms |
| GET /api/v1/file/list | < 100ms | < 300ms |
| POST /api/v1/filetransfer/uploadFile | < 100ms (开始) | < 200ms |
| GET /api/v1/search/searchFile | < 200ms | < 500ms |

使用 Spring Boot Actuator metrics 或 JMeter/Gatling 做性能测试。
