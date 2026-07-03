# 部署运维规则

## 启动依赖验证

所有外部服务在应用上下文完全加载前完成连通性验证。

核心服务验证失败则阻止启动：MySQL、Redis。
非核心服务验证失败记录警告但允许启动（降级模式运行）：Elasticsearch、OnlyOffice。

```java
@Component
public class StartupDependencyChecker implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        checkMySQL();      // 失败 → 抛异常，阻止启动
        checkRedis();      // 失败 → 抛异常，阻止启动
        checkElasticsearch();  // 失败 → 记录 WARN
        checkOnlyOffice();     // 失败 → 记录 WARN
    }
}
```

## 启动顺序

Docker Compose 强制启动顺序：MySQL → Redis → Elasticsearch / OnlyOffice → App。

```yaml
services:
  app:
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      elasticsearch:
        condition: service_healthy
  mysql:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
  redis:
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## 密钥同步

JWT_SECRET 等共享密钥在多个服务间（后端与 OnlyOffice Docker）必须来自同一个环境变量或密钥管理器。禁止在 `application.yml` 和 `docker-compose.yml` 中分别硬编码不同的密钥值。

```yaml
# docker-compose.yml
services:
  onlyoffice:
    environment:
      JWT_SECRET: ${JWT_SECRET}  # 与后端共用同一变量

# application.yml
jwt:
  secret: ${JWT_SECRET}
```

## 环境配置

使用 Spring Profile 管理多环境：

| Profile | 用途 | 特殊配置 |
|---------|------|---------|
| `dev` | 本地开发 | H2 内存数据库可选、日志 DEBUG、Swagger 开启 |
| `test` | CI/测试 | Testcontainers、日志 INFO |
| `prod` | 生产 | 日志 WARN/ERROR、Swagger 关闭、HTTPS 强制 |

敏感配置（数据库密码、JWT 密钥、存储凭证）通过环境变量注入，不写入代码仓库：

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      password: ${REDIS_PASSWORD:}
```

## 日志配置

日志目录不存在时自动创建。容器化部署使用 stdout/stderr 输出（Logback ConsoleAppender），日志收集走 ELK/Loki。

日志写入失败不允许静默忽略（Logback 的 `immediateFlush=true` 或 `StatusListener` 捕获写入错误）。

生产环境日志级别：应用包 `INFO`，框架包 `WARN`，Hibernate SQL `OFF`（通过 P6Spy 或慢查询日志替代）。

## CI Pipeline

CI 必须包含以下检查，任何一步失败则整个 pipeline 失败：

```yaml
# .github/workflows/ci.yml
jobs:
  build:
    steps:
      - run: mvn compile                         # 编译通过
      - run: mvn test                            # 单元测试 + 集成测试
      - run: mvn checkstyle:check                # 代码风格
      - run: |                                    # Jakarta 迁移完整性
          ! grep -r "javax\.\(persistence\|servlet\|validation\|annotation\|transaction\)" src/main/java/
      - run: cd frontend && npm ci
      - run: cd frontend && npm run lint          # ESLint
      - run: cd frontend && npm run type-check    # TypeScript 类型检查
      - run: cd frontend && npm run test          # Vitest
      - run: cd frontend && npm run build         # 前端构建
```

## 构建产物

使用 Spring Boot fat JAR 作为标准构建产物。禁止自定义 CLASSPATH 脚本。

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/qiwenshare-backend-1.0.0.jar --spring.profiles.active=prod
```

## 存储迁移

存储后端切换时的数据迁移规则：
- 必须幂等（可重复执行，结果一致）
- 必须可验证（hash 校验每个文件）
- 必须可回滚（保留旧存储数据直到验证通过）
- 过渡期采用双写策略（新旧后端同时写入）
- 回滚流程必须经过测试

## TLS 证书

所有存储和 CDN 端点使用有效 TLS 证书。证书到期前 30 天告警。自签名证书仅在开发环境使用。

## 存储成本治理

- 用户配额强制执行
- 存储生命周期规则（归档/删除）已配置
- 用量 80% / 90% 告警通知
- 定期审计孤立存储对象（无元数据引用的文件）
