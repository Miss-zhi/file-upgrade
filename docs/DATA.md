# 数据完整性规则

## 查询性能

列表/分页查询必须使用索引列作为过滤和排序条件。禁止在 `WHERE` 子句中对未建索引的列做过滤。

禁止 N+1 查询模式。解决方案按场景选择：
- JPA：`JOIN FETCH`、`@EntityGraph(attributePaths = {"roles", "permissions"})`
- MyBatis-Plus：`select` + `in` 批量查询，或子查询
- 对于树形结构（文件目录），使用递归 CTE 或一次性加载

分页大小上限 100，默认 20。客户端请求超过上限时自动截断并返回提示。

大表（> 100 万行）的 count 查询考虑缓存或估算（`EXPLAIN` 返回的近似行数）。

## 死锁预防

批量更新按一致的 key 排序（如按 id 升序），确保不同事务以相同顺序获取行锁。

事务持续时间超过 5 秒的必须标记并重构。在开发阶段可通过 AOP 切面记录慢事务：

```java
@Aspect
@Component
public class SlowTransactionDetector {
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object detect(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 5000) {
                log.warn("慢事务: {} 耗时 {}ms", pjp.getSignature().toShortString(), duration);
            }
        }
    }
}
```

避免在事务中执行外部调用（HTTP 请求、文件 IO、Redis 操作）。这些操作耗时长且不可控，会长时间占用数据库连接。

**COW（Copy-On-Write）模式的事务安全**：当需要在编辑前复制文件时，物理文件复制（外部 IO）必须在事务外执行，仅 DB 写操作在事务内完成。

反面示例（事务内执行外部 IO）：
```java
@Transactional
public void editWithCow(FileBean fileBean) {
    // 错误：物理文件下载+写入在事务内，长时间占用 DB 连接
    try (InputStream is = storageBackend.download(path)) {
        storageBackend.write(newPath, is);
    }
    fileBeanRepository.save(copy);
}
```

正面示例（分离 IO 和事务）：
```java
public void editWithCow(FileBean fileBean) {
    // 事务外：物理文件复制
    FileBean copy = copyPhysicalFile(fileBean);
    // 事务内：仅 DB 写操作（通过 @Lazy self 代理）
    self.saveCowCopy(copy);
}

@Transactional
public void saveCowCopy(FileBean copy) {
    fileBeanRepository.save(copy);
}
```

**JPQL 语法限制**：JPQL 不支持 `LIMIT` 语法（是 Hibernate HQL 扩展，非标准 JPA）。严格 JPA 模式下会解析失败。

反面示例：
```java
@Query("SELECT v FROM DocumentVersion v WHERE v.userFileId = :id ORDER BY v.versionNumber ASC LIMIT 1")
Optional<DocumentVersion> findOldest(@Param("id") Long id);
```

正面示例：
```java
// 使用 Spring Data 派生查询 findFirst
Optional<DocumentVersion> findFirstByUserFileIdOrderByVersionNumberAsc(Long id);
```

## Schema 迁移

生产环境 `spring.jpa.hibernate.ddl-auto=validate`（禁止 `update`/`create`/`create-drop`）。

所有 schema 变更通过 Flyway 管理：

```
src/main/resources/db/migration/
├── V1__create_auth_tables.sql
├── V2__init_auth_data.sql
├── V3__create_file_tables.sql
├── V4__create_storage_tables.sql
└── ...
```

命名规范：`V{version}__{description}.sql`。版本号递增，不允许跳号。

每个迁移脚本必须：
- 幂等（可重复执行不报错，用 `IF NOT EXISTS` 或 `CREATE OR REPLACE`）
- 可版本化（每次变更一个新文件，不修改已有文件）
- 有回滚脚本（`U{version}__{description}.sql`，放在 `db/undo/` 目录）

## 事务安全

所有写操作的 Service 方法必须声明 `@Transactional(rollbackFor = Exception.class)`。

禁止同类内部方法调用带事务的方法（绕过 Spring AOP 代理，事务不生效）。解决方案：

```java
@Service
public class FileService {
    @Lazy @Autowired
    private FileService self;  // 自注入

    public void outerMethod() {
        // 通过 self 调用，走代理，事务生效
        self.transactionalMethod();
    }

    @Transactional(rollbackFor = Exception.class)
    public void transactionalMethod() {
        // 业务逻辑
    }
}
```

禁止在事务方法中吞掉异常。如果 catch 了异常，必须重新抛出或记录并做补偿处理。

## 索引管理

- 外键列必须建索引（JPA `@JoinColumn` 默认不创建数据库索引，需要显式 `@Index`）
- 联合主键/联合唯一约束的列顺序影响查询性能，按查询频率排列
- 定期审查慢查询日志，补充缺失索引

## 数据一致性

- 跨表操作（如删除文件同时删除分享记录）在同一个事务中完成
- 异步操作（如 ES 索引更新）使用最终一致性：先更新 DB，再异步更新 ES，失败时通过定时任务补偿
- 计数器字段（如存储用量）使用乐观锁或 Redis 原子操作，避免并发更新覆盖
