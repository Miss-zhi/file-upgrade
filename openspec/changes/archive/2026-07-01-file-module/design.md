## Context

奇文网盘文件模块是系统核心，负责文件的上传、下载、管理和分享。当前系统从 Spring Boot 2 升级到 Spring Boot 3，需要在新栈上完全重写文件模块。

现有约束：
- 后端三层架构：Controller → Service → Repository，禁止跨层调用
- JPA (Hibernate) 6.x 为主 ORM，MyBatis-Plus 为辅（复杂查询/批量操作）
- UFOP 统一文件操作框架抽象 5 种存储后端
- 文件模块依赖 auth 模块（JWT 认证 + 用户 ID）
- Redis 用于缓存、分布式锁、上传进度追踪
- Flyway 管理 schema 迁移，生产环境 ddl-auto=validate
- 构造器注入，禁止 @Autowired 字段注入
- Entity 与 DTO/VO 严格分离
- 事务方法中禁止执行外部 IO

## Goals / Non-Goals

**Goals:**
- 实现 UFOP 统一存储工厂，支持 5 种存储后端（Local / MinIO / AliyunOSS / Qiniu / FastDFS）
- 实现完整的文件上传链路：普通上传、分片上传（>10MB）、秒传（hash 去重）
- 实现文件下载：流式下载 + 断点续传（Range 请求）
- 实现文件 CRUD：重命名、移动、创建文件夹、列表查询
- 实现回收站：软删除 + 恢复 + 异步永久删除
- 实现文件分享：链接 + 提取码 + 有效期
- 实现用户存储配额管理：预扣/确认/释放机制
- 所有 API 遵循 REST 规范，统一前缀 `/api/v1`，响应包装 `RestResult<T>`

**Non-Goals:**
- 不实现全文搜索（后续 search 模块）
- 不实现 OnlyOffice 在线预览（后续 document 模块）
- 不实现前端 UI 组件（前端单独迭代）
- 不实现文件版本管理
- 不实现文件加密存储

## Decisions

### 1. 数据模型设计：FileBean + UserFile 双层结构

**决策**：文件存储元数据（FileBean）和用户文件关系（UserFile）分离。

**理由**：
- FileBean 对应物理存储对象，通过文件 hash 去重，多个 UserFile 可引用同一个 FileBean
- UserFile 记录用户维度的文件信息（文件名、目录、权限），支持软删除/回收站
- 秒传利用此结构：hash 匹配时复用 FileBean，仅创建新 UserFile 引用

**替代方案**：单层结构（一个表同时存物理信息和逻辑信息）—— 无法实现文件去重，存储浪费。

### 2. UFOP 工厂模式：策略模式 + Spring Boot AutoConfiguration

**决策**：UFOP 框架定义 7 个操作接口（Uploader/Downloader/Copier/Deleter/Previewer/Reader/Writer），每种存储后端实现全部接口。通过 `StorageFactory` 根据 `storage-type` 配置获取对应实现。各后端通过独立 Starter 模块 + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。

**理由**：
- 策略模式便于新增存储后端，不影响业务代码
- AutoConfiguration.imports 是 Spring Boot 3 标准方式（替代 spring.factories）
- 启动时连通性验证确保配置正确

**替代方案**：Spring Profile 切换——不够灵活，无法在运行时动态选择。

### 3. 分片上传：UploadTask + UploadTaskDetail 追踪

**决策**：
- 大于 10MB 的文件强制分片上传，分片大小 5MB
- `UploadTask` 记录上传任务元数据（文件 hash、总分片数、状态）
- `UploadTaskDetail` 记录每个分片的上传状态
- Redis 缓存上传进度，定时任务清理超时任务
- 所有分片完成后异步合并

**理由**：
- 分片追踪支持断点续传和失败重试
- Redis 缓存避免频繁写库
- 超时清理防止存储碎片

### 4. 回收站：软删除 + 异步永久删除

**决策**：
- 删除文件时更新 UserFile 的 `deleteStatus` 字段（0=正常, 1=已删除），移入回收站视图
- 永久删除时通过异步线程池（`fileTaskExecutor`）执行 UFOP Deleter 清理存储对象
- 回收站自动清理超过 30 天的文件（定时任务）

**理由**：
- 软删除快速响应，用户体验好
- 永久删除是 IO 密集操作，异步执行避免阻塞请求线程
- 遵循"事务方法中禁止外部 IO"规则

### 5. 文件分享：独立分享表 + 提取码

**决策**：
- `ShareFile` 表记录分享信息（分享链接 code、提取码、过期时间、关联 UserFile）
- 分享链接使用 8 位随机 code，提取码 4-6 位
- 支持设置有效期（1天/7天/30天/永久）

**理由**：
- 独立表便于管理分享列表和过期清理
- 提取码增加安全性

### 6. 存储配额：Redis 原子操作 + DB 最终一致

**决策**：
- 用户已用空间存储在 Redis（原子递增/递减），定期同步到 DB
- 上传前 `checkQuota` 校验，上传开始时预扣，完成时确认，失败时释放
- 使用 Redis `INCRBY` / `DECRBY` 保证并发安全

**理由**：
- Redis 原子操作避免并发更新覆盖
- 预扣机制防止超额上传
- DB 作为持久化备份，Redis 故障时从 DB 恢复

### 7. 目录结构：路径编码 + 树形查询

**决策**：
- 文件目录使用 `filePath` 字段存储完整路径（如 `/文档/工作/报告.docx`）
- `fileId` 作为主键，`filePath` 建索引用于快速定位
- 列表查询按 `filePath` 前缀过滤同目录文件

**理由**：
- 路径编码简化移动和重命名操作
- 前缀查询高效，避免递归查询

## Risks / Trade-offs

- **[文件去重 hash 碰撞]** → 使用 SHA-256 算法，碰撞概率极低（2^256）。额外校验文件大小作为二次验证。
- **[分片合并失败]** → 合并操作记录状态，失败时保留分片数据，定时任务重试或通知管理员。
- **[Redis 故障导致配额不准]** → 定时任务（每小时）从 DB 重新计算用户已用空间，修正 Redis 缓存。
- **[永久删除 IO 阻塞]** → 使用独立线程池（`fileTaskExecutor`），设置队列上限 100，溢出时 CallerRunsPolicy 降级。
- **[存储后端不可用]** → 启动时连通性验证失败阻止应用启动（核心后端），运行时操作失败返回 503 + UPLOAD_STORAGE_ERROR。
- **[大文件下载内存压力]** → 强制流式处理，禁止全量加载到内存。≥50MB 文件支持 Range 请求断点续传。

---
## Supplement: 类清单

### 包结构总览
```
com.qiwenshare.file
├── controller/         # REST 端点
├── service/            # 业务逻辑
├── repository/         # JpaRepository 接口
├── entity/             # JPA Entity
├── dto/                # 请求体 DTO（record 类型）
├── vo/                 # 响应体 VO（record 类型）
├── exception/          # 自定义异常
├── task/               # 定时任务
└── common/             # 工具类、常量

com.qiwenshare.storage
├── factory/            # StorageFactory 工厂
├── interfaces/         # 7 个操作接口
├── impl/
│   ├── local/          # 本地存储实现
│   ├── minio/          # MinIO 实现
│   ├── aliyun/         # AliyunOSS 实现
│   ├── qiniu/          # 七牛实现
│   └── fastdfs/        # FastDFS 实现
└── config/             # StorageProperties、AutoConfiguration
```

### 类清单表

| 包 | 类名 | 类型 | 职责 |
|---|------|------|------|
| controller | `FileController` | @RestController | 文件 CRUD（创建、重命名、移动、复制、删除、列表、详情、文件树） |
| controller | `FileTransferController` | @RestController | 文件上传/下载 |
| controller | `RecoveryFileController` | @RestController | 回收站操作 |
| controller | `FileShareController` | @RestController | 文件分享操作 |
| controller | `QuotaController` | @RestController | 存储配额查询/管理 |
| service | `FileUploadService` | @Service | 普通上传、秒传、分片上传 |
| service | `FileDownloadService` | @Service | 流式下载、断点续传 |
| service | `FileOperationService` | @Service | 文件 CRUD、列表查询、文件树 |
| service | `FileRecoveryService` | @Service | 软删除、恢复、永久删除 |
| service | `FileShareService` | @Service | 创建分享、验证提取码、下载分享文件 |
| service | `StorageQuotaService` | @Service | 配额校验、预扣/确认/释放、Redis 原子操作 |
| service | `AuditLogService` | @Service | 异步写入下载审计日志 |
| entity | `FileBean` | @Entity | 物理文件元数据（hash、大小、存储路径） |
| entity | `UserFile` | @Entity | 用户-文件关联（文件名、路径、软删除标记） |
| entity | `UploadTask` | @Entity | 分片上传任务 |
| entity | `UploadTaskDetail` | @Entity | 分片上传详情 |
| entity | `ShareFile` | @Entity | 分享记录 |
| entity | `StorageBean` | @Entity | 用户存储配额 |
| entity | `AuditLog` | @Entity | 下载审计日志 |
| repository | `FileBeanRepository` | JpaRepository | findByFileHashAndFileSize、existsByFileHashAndFileSize |
| repository | `UserFileRepository` | JpaRepository | findByUserId+filePath、findByDeleteStatus |
| repository | `UploadTaskRepository` | JpaRepository | findByTaskId、findByStatus+过期时间 |
| repository | `ShareFileRepository` | JpaRepository | findByShareCode、findByUserId |
| repository | `StorageBeanRepository` | JpaRepository | findByUserId |
| repository | `AuditLogRepository` | JpaRepository | save |
| dto | `UploadFileDTO` | record | 普通上传请求体 |
| dto | `ChunkUploadDTO` | record | 分片上传请求体 |
| dto | `SpeedUploadDTO` | record | 秒传请求体 |
| dto | `CreateFileDTO` | record | 创建文件请求体 |
| dto | `CreateFoldDTO` | record | 创建文件夹请求体 |
| dto | `RenameFileDTO` | record | 重命名请求体 |
| dto | `MoveFileDTO` | record | 移动请求体 |
| dto | `BatchMoveFileDTO` | record | 批量移动请求体 |
| dto | `CopyFileDTO` | record | 复制请求体 |
| dto | `BatchCopyFileDTO` | record | 批量复制请求体 |
| dto | `DeleteFileDTO` | record | 删除请求体 |
| dto | `BatchDeleteFileDTO` | record | 批量删除请求体 |
| dto | `FileListDTO` | record | 文件列表查询参数 |
| dto | `ShareCreateDTO` | record | 创建分享请求体 |
| dto | `ShareVerifyDTO` | record | 提取码验证请求体 |
| dto | `RestoreFileDTO` | record | 恢复文件请求体 |
| vo | `FileListVO` | record | 文件列表响应 |
| vo | `FileDetailVO` | record | 文件详情响应 |
| vo | `TreeNodeVO` | record | 文件树节点 |
| vo | `UploadFileVO` | record | 上传响应 |
| vo | `QuotaInfoVO` | record | 配额信息响应 |
| vo | `ShareInfoVO` | record | 分享信息响应 |
| vo | `BatchOperationResultVO` | record | 批量操作结果（成功数+失败列表） |
| exception | `FileModuleException` | RuntimeException | 文件模块业务异常 |
| exception | `FileErrorCode` | enum | 错误码枚举（UPLOAD_SIZE_EXCEEDED 等） |
| task | `UploadCleanupTask` | @Scheduled | 清理超时未完成的上传任务 |
| task | `RecycleBinCleanupTask` | @Scheduled | 清理 >30 天的回收站文件 |
| task | `ShareCleanupTask` | @Scheduled | 清理过期分享记录 |
| task | `QuotaSyncTask` | @Scheduled | 每小时同步 Redis 配额到 DB |
| common | `FileAsyncConfig` | @Configuration | fileTaskExecutor 异步线程池 |
| common | `FileCategory` | enum | 文件类型分类（IMAGE/DOCUMENT/VIDEO/AUDIO/ARCHIVE/OTHER） |
| storage/factory | `StorageFactory` | @Component | 根据 storage.type 返回存储后端实例 |
| storage/interfaces | `Uploader` | interface | 上传操作 |
| storage/interfaces | `Downloader` | interface | 下载操作 |
| storage/interfaces | `Copier` | interface | 复制操作 |
| storage/interfaces | `Deleter` | interface | 删除操作 |
| storage/interfaces | `Previewer` | interface | 预览操作 |
| storage/interfaces | `Reader` | interface | 读取操作 |
| storage/interfaces | `Writer` | interface | 写入操作 |
| storage/interfaces | `StorageBackend` | interface | 组合接口，继承全部 7 个操作接口 |
| storage/impl/local | `LocalStorageBackend` | @Component | 本地文件存储实现 |
| storage/impl/local | `LocalStorageAutoConfiguration` | @AutoConfiguration | 自动配置 + 条件注解 |
| storage/impl/minio | `MinioStorageBackend` | @Component | MinIO S3 兼容实现 |
| storage/impl/aliyun | `AliyunOssStorageBackend` | @Component | 阿里云 OSS 实现 |
| storage/impl/qiniu | `QiniuStorageBackend` | @Component | 七牛云实现 |
| storage/impl/fastdfs | `FastDfsStorageBackend` | @Component | FastDFS 实现 |
| storage/config | `StorageProperties` | @ConfigurationProperties | storage.type、连接参数 |
| storage/config | `StorageHealthChecker` | @Component + SmartLifecycle | 启动时写入→读取→删除连通性验证 |

---

## Supplement: 数据库 Schema

### Flyway V4__create_file_tables.sql

```sql
CREATE TABLE `file_bean` (
  `file_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `file_size`     BIGINT       NOT NULL COMMENT '文件大小（字节）',
  `file_hash`     VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hash',
  `storage_type`  VARCHAR(20)  NOT NULL COMMENT '存储后端类型 local/minio/aliyun/qiniu/fastdfs',
  `storage_path`  VARCHAR(500) NOT NULL COMMENT '存储后端中的物理路径',
  `file_status`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1-正常 0-已清理',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  UNIQUE KEY `uk_file_hash_size` (`file_hash`, `file_size`),
  KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物理文件元数据';

CREATE TABLE `user_file` (
  `user_file_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`       BIGINT       NOT NULL COMMENT '用户 ID（关联 user.id）',
  `file_id`       BIGINT       DEFAULT NULL COMMENT '关联 file_bean.file_id（文件夹为 NULL）',
  `file_name`     VARCHAR(255) NOT NULL COMMENT '文件名（不含扩展名）',
  `extend_name`   VARCHAR(20)  DEFAULT '' COMMENT '文件扩展名（不含点号）',
  `file_path`     VARCHAR(1000) NOT NULL DEFAULT '/' COMMENT '虚拟目录路径（/分隔）',
  `file_type`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1-普通文件 2-文件夹',
  `delete_status` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0-正常 1-已删除',
  `delete_time`   DATETIME     DEFAULT NULL COMMENT '删除时间',
  `delete_batch_num` VARCHAR(32) DEFAULT NULL COMMENT '删除批次号',
  `upload_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_file_id`),
  UNIQUE KEY `uk_user_path_name` (`user_id`, `file_path`, `file_name`, `extend_name`, `delete_status`, `file_type`),
  KEY `idx_user_id_path` (`user_id`, `file_path`),
  KEY `idx_delete_status` (`user_id`, `delete_status`, `delete_time`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户文件关联';

CREATE TABLE `upload_task` (
  `task_id`       VARCHAR(64)  NOT NULL COMMENT '上传任务 ID',
  `user_id`       BIGINT       NOT NULL,
  `file_name`     VARCHAR(255) NOT NULL,
  `file_hash`     VARCHAR(64)  NOT NULL,
  `file_size`     BIGINT       NOT NULL COMMENT '文件总大小',
  `total_chunks`  INT          NOT NULL COMMENT '总分片数',
  `uploaded_chunks` INT        NOT NULL DEFAULT 0 COMMENT '已上传分片数',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0-进行中 1-合并中 2-完成 3-失败',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分片上传任务';

CREATE TABLE `upload_task_detail` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`       VARCHAR(64)  NOT NULL,
  `chunk_index`   INT          NOT NULL COMMENT '分片序号（从 0 开始）',
  `chunk_size`    BIGINT       NOT NULL COMMENT '分片大小',
  `chunk_hash`    VARCHAR(64)  DEFAULT NULL COMMENT '分片 hash',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待上传 1-已上传 2-失败',
  `storage_path`  VARCHAR(500) DEFAULT NULL COMMENT '临时存储路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_chunk` (`task_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分片上传详情';

CREATE TABLE `share_file` (
  `share_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL COMMENT '分享者 ID',
  `user_file_id`  BIGINT       NOT NULL COMMENT '关联 user_file',
  `share_code`    VARCHAR(8)   NOT NULL COMMENT '8 位随机分享码',
  `extract_code`  VARCHAR(6)   DEFAULT NULL COMMENT '提取码（NULL 表示公开）',
  `expire_time`   DATETIME     DEFAULT NULL COMMENT '过期时间（NULL 表示永久）',
  `view_count`    INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享';

CREATE TABLE `storage_bean` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL COMMENT '用户 ID',
  `total_quota`   BIGINT       NOT NULL DEFAULT 10737418240 COMMENT '总配额（字节），默认 10GB',
  `used_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '已用空间（字节）',
  `pre_used_size` BIGINT       NOT NULL DEFAULT 0 COMMENT '预扣空间（字节）',
  `modify_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户存储配额';

CREATE TABLE `audit_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `user_file_id`  BIGINT       NOT NULL,
  `action`        VARCHAR(20)  NOT NULL COMMENT '操作类型 download/share_download',
  `ip_address`    VARCHAR(45)  NOT NULL,
  `user_agent`    VARCHAR(500) DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_file_time` (`user_file_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';
```

### Flyway V5__create_share_tables.sql

```sql
-- V5 已合并到 V4，ShareFile 表在 V4 中一并创建
-- 此文件保留用于后续分享相关扩展表
```

---

## Supplement: API 端点总表

| Controller | 方法 | 路径 | 权限 | 说明 |
|-----------|------|------|------|------|
| **FileController** | GET | `/api/v1/file/getfilelist` | authenticated | 文件列表（分页+类型筛选） |
| | GET | `/api/v1/file/getfilelist/bycategory` | authenticated | 按分类查询文件列表 |
| | GET | `/api/v1/file/getfiletree` | authenticated | 文件夹树 |
| | GET | `/api/v1/file/getfiledetail/{userFileId}` | authenticated | 文件详情 |
| | POST | `/api/v1/file/createfile` | authenticated | 创建文件 |
| | POST | `/api/v1/file/createfold` | authenticated | 创建文件夹 |
| | POST | `/api/v1/file/renamefile` | authenticated | 重命名 |
| | POST | `/api/v1/file/movefile` | authenticated | 移动文件 |
| | POST | `/api/v1/file/batchmovefile` | authenticated | 批量移动 |
| | POST | `/api/v1/file/copyfile` | authenticated | 复制文件 |
| | POST | `/api/v1/file/batchcopyfile` | authenticated | 批量复制 |
| **FileTransferController** | POST | `/api/v1/filetransfer/upload` | authenticated | 普通上传（≤10MB） |
| | POST | `/api/v1/filetransfer/upload/speed` | authenticated | 秒传 |
| | POST | `/api/v1/filetransfer/upload/chunk/init` | authenticated | 分片上传初始化 |
| | POST | `/api/v1/filetransfer/upload/chunk` | authenticated | 上传分片 |
| | POST | `/api/v1/filetransfer/upload/chunk/merge` | authenticated | 分片合并 |
| | GET | `/api/v1/filetransfer/download/{userFileId}` | authenticated | 流式下载 |
| | POST | `/api/v1/filetransfer/batch-download` | authenticated | 批量下载（ZIP） |
| **RecoveryFileController** | GET | `/api/v1/recycle/list` | authenticated | 回收站列表 |
| | POST | `/api/v1/recycle/deletefile` | authenticated | 软删除文件 |
| | POST | `/api/v1/recycle/batchdeletefile` | authenticated | 批量软删除 |
| | POST | `/api/v1/recycle/restorefile` | authenticated | 恢复文件 |
| | POST | `/api/v1/recycle/deletepermanent` | authenticated | 永久删除 |
| | POST | `/api/v1/recycle/deleteall` | authenticated | 清空回收站 |
| **FileShareController** | POST | `/api/v1/share/createshare` | authenticated | 创建分享链接 |
| | GET | `/api/v1/share/info/{shareCode}` | permitAll | 查看分享内容 |
| | POST | `/api/v1/share/verifyshare` | permitAll | 验证提取码 |
| | GET | `/api/v1/share/download/{shareCode}` | permitAll | 分享文件下载 |
| | GET | `/api/v1/share/myshares` | authenticated | 我的分享列表 |
| | DELETE | `/api/v1/share/cancelshare/{shareId}` | authenticated | 取消分享 |
| **QuotaController** | GET | `/api/v1/quota/info` | authenticated | 查询当前用户配额 |
| | PUT | `/api/v1/admin/quota/{userId}` | hasRole("ADMIN") | 管理员设置配额 |

---

## Supplement: Redis Key 总表

| Key 模式 | 数据类型 | TTL | 用途 | 写入方 | 读取方 |
|----------|---------|-----|------|--------|--------|
| `file:quota:used:{userId}` | STRING (值=字节数) | 无过期 | 用户已用空间（原子 INCRBY/DECRBY） | StorageQuotaService | StorageQuotaService |
| `file:quota:pre:{userId}` | STRING (值=预扣字节数) | 1h | 上传预扣空间 | StorageQuotaService | StorageQuotaService |
| `file:upload:progress:{taskId}` | STRING (值=JSON 进度信息) | 2h | 分片上传进度缓存 | FileUploadService | FileUploadService |
| `file:share:verified:{shareCode}` | STRING (值="1") | 30min | 分享验证缓存 | FileShareService | FileShareController |
| `file:list:{userId}:{pathHash}` | STRING (值=文件列表 JSON) | 2min | 文件列表缓存 | FileOperationService | FileOperationService |

**Key 命名规范**：生产环境追加应用名前缀 `qw:file:` 避免多应用冲突。

**降级策略**：
- `file:quota:used` Redis 不可用时回退到 DB 查询 StorageBean.usedSize
- `file:upload:progress` Redis 不可用时直接查 UploadTaskDetail 表
- `file:share:verified` Redis 不可用时每次下载都要求验证提取码
- `file:list` 缓存 miss 时直接查 DB，无降级需要

