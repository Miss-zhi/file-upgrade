# API 韧性与降级规则

## 外部服务降级

每个外部服务必须有降级策略，不允许第三方故障级联为全系统不可用：

| 服务 | 降级策略 | 实现方式 |
|------|---------|---------|
| Elasticsearch | 搜索返回空结果 + 用户提示"搜索服务暂时不可用" | `try-catch` + fallback 空列表 |
| OnlyOffice | 降级为只读预览（提供下载链接） | 检查服务健康状态，不可用时切换预览模式 |
| Redis | 短暂降级为本地缓存（Caffeine），持续不可用则告警 | 双层缓存：Redis + Caffeine，Redis 不可用时走 Caffeine |
| 对象存储（OSS/MinIO） | 回退到本地文件系统存储 + 告警 | UFOP 工厂切换到 LOCAL_STORAGE 后端 |

降级触发条件：
- 连续 3 次调用失败（熔断器半开状态探测）
- 健康检查连续 2 次失败
- 调用超时超过阈值

## 外部调用韧性

所有外部服务调用必须配置三项保护，禁止无限阻塞：

### 超时

```java
// 连接超时 5s，读取超时 10s
RestClient.builder()
    .requestFactory(new JdkClientHttpRequestFactory(
        HttpClient.newHttpClient()))
    .defaultHeader("Connection", "close")
    .build();
```

每个外部服务的超时参数从 `application.yml` 读取，可按环境调整。

### 重试

指数退避重试，最多 3 次：

```java
@Retryable(
    retryFor = {IOException.class, TimeoutException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
)
public String callExternalService() { ... }
```

不重试的场景：4xx 错误（客户端错误）、认证失败、幂等性不确定的写操作、**永久性错误**（如文档损坏、文件格式无效）。

**永久性错误禁止触发重试**：当错误表示不可恢复的状态（如 OnlyOffice status=3/7 文档损坏）时，必须返回成功响应（error=0）并记录 ERROR 日志。返回错误码（error=1）会触发外部系统重试，导致 retry storm。

### 熔断

使用 Resilience4j 熔断器：

```java
@CircuitBreaker(name = "elasticsearch", fallbackMethod = "searchFallback")
public List<FileSearchResult> search(String query) {
    return elasticsearchClient.search(query);
}

private List<FileSearchResult> searchFallback(String query, Exception e) {
    log.warn("ES 搜索降级: {}", e.getMessage());
    return Collections.emptyList();
}
```

熔断器配置：
- 失败率阈值：50%
- 半开状态探测间隔：30 秒
- 半开状态允许探测请求数：5
- 等待从打开到半开状态的持续时间：60 秒

## 健康检查

Actuator `/actuator/health` 报告所有外部依赖状态。每个外部服务实现 Spring Boot 的 `HealthIndicator` 接口：

```java
@Component
public class ElasticsearchHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            esClient.ping();  // 或 cluster health API
            return Health.up().withDetail("cluster", "green").build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

必须实现 HealthIndicator 的服务：MySQL、Redis、Elasticsearch、OnlyOffice、当前激活的存储后端。

启动行为：
- 核心服务（MySQL、Redis）健康检查失败 → 阻止应用启动
- 非核心服务（ES、OnlyOffice）健康检查失败 → 记录警告，允许启动（降级模式）

## 回调连通性

需要回调的服务（OnlyOffice）在部署时验证回调 URL 的双向连通性：

1. 后端 → OnlyOffice：HTTP GET 检查文档服务状态
2. OnlyOffice → 后端：验证回调 URL 可达（发送测试请求）

网络要求写入部署文档。Docker 环境中确保容器间网络互通。

## 请求可观测性

每个 API 端点记录请求 URI、耗时、用户上下文（通过 AOP 切面 `@OperationLog`）。

```java
@Aspect
@Component
public class OperationLogAspect {
    @Around("@annotation(operationLog)")
    public Object log(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("[{}] {} {} - {}ms",
                SecurityContextHolder.getContext().getAuthentication()?.getName(),
                operationLog.action(),
                pjp.getSignature().toShortString(),
                duration);
        }
    }
}
```

P95/P99 延迟可通过 Actuator metrics 获取（`/actuator/metrics/http.server.requests`）。
