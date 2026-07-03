## Context

当前 document 模块（OnlyOffice 在线文档预览/编辑）功能基本可用，但存在严重架构和安全问题：

- **双 JWT 系统**：应用级 JJWT（用户认证）与 OnlyOffice primeframework JWT（文档 token）共存，密钥/算法/过期策略各自独立
- **回调框架未使用**：`CallbackHandler` + `DefaultCallbackManager` 完整实现但 Controller 用 if-else 硬编码处理
- **权限缺失**：任何登录用户知道 userFileId 即可编辑任意文件
- **文档 key 永不更新**：key = hash(userFileId + uploadTime)，保存后 OnlyOffice 缓存旧内容
- **SSL 全局关闭**：`verify-peer-off=true` 影响所有 HTTPS 调用
- **回调 URL 嵌入用户 token**：token 过期后回调静默失败，编辑内容丢失
- **文件大小限制 5MB**：与上传 2GB 能力严重不匹配
- **版本历史只写不读**：HistoryManager 被注释，前端无法访问

现有架构：
- 后端 Spring Boot 3.2.x + JPA (Hibernate) 6.x + MyBatis-Plus 3.5.x
- auth 模块已有 `TokenService`（jjwt 0.12.x），支持 access/refresh token、黑名单、全局撤销
- 存储层通过 UFOP `StorageFactory` 抽象，`StorageBackend` 接口包含 Uploader/Downloader/Copier/Deleter/Previewer/Reader/Writer
- `UserFile`（用户文件维度）和 `FileBean`（物理文件元数据）两个核心实体
- Flyway 迁移已到 V6，document 模块需要 V7

约束：
- 必须复用 auth 模块 JJWT 系统，废弃 primeframework JWT
- document 模块作为独立包 `com.qiwenshare.document`，不侵入 file/auth 模块
- 回调处理必须在 5 秒内响应（OnlyOffice 超时限制）
- 遵循 AGENTS.md 全部红线规则

## Goals / Non-Goals

**Goals:**
- 统一 JWT 系统：文档 token 和回调 token 全部使用 auth 模块的 JJWT，通过自定义 claims 区分用途
- 启用回调框架：策略模式分发 OnlyOffice 回调状态处理，替代 if-else 硬编码
- 实现文件级权限检查：预览/编辑前验证文件所有者或分享权限
- 修复文档 key 策略：key 包含 lastModifiedTime，保存后自动更新
- 文件大小限制提升至 50MB（可配置）
- 启用版本历史：版本存储、查询、下载、回滚
- 回调 URL 使用独立短期 token（30 分钟），不依赖用户 session
- SSL 验证默认开启
- 提供 OnlyOffice 健康检查端点

**Non-Goals:**
- OnlyOffice Document Server 的部署和运维
- 前端 OnlyOffice 编辑器页面（属于 document-module-frontend）
- 实时协作编辑冲突解决（OnlyOffice 内部处理）
- 文档模板管理
- PDF 在线标注/签名

## Decisions

### 1. JWT 统一方案：复用 auth 模块 TokenService + 自定义 claims

**选择：** 在 `TokenService` 中新增 `generateDocumentToken` 和 `generateCallbackToken` 方法，通过 `type` claim 区分（`doc` / `cb`）。

**替代方案：**
- document 模块独立维护 JWT 密钥：增加密钥管理复杂度，违反统一 JWT 原则
- 使用 OnlyOffice 内置 JWT（primeframework）：与主系统 JWT 系统割裂，无法复用验证逻辑

**理由：** 复用已有的 `TokenService` 签名密钥和验证逻辑，只需扩展生成方法。文档 token 和回调 token 使用独立 claims 前缀，与用户认证 token 区分：

```
用户认证 token：sub=userId, type=access, roles=[...], exp=15分钟
文档 token：    sub=userId, type=doc, doc.fileId=xxx, doc.action=edit|view, exp=4小时
回调 token：    sub=userId, type=cb, cb.fileId=xxx, cb.type=edit, exp=30分钟
```

**TokenService 扩展方法：**
```java
public String generateDocumentToken(String userId, Long userFileId, String action) { ... }
public String generateCallbackToken(String userId, Long userFileId, String type) { ... }
public Claims parseDocumentToken(String token) { ... }  // 验证 type=doc
public Claims parseCallbackToken(String token) { ... }  // 验证 type=cb
```

### 2. 回调处理：策略模式 + CallbackHandler 框架

**选择：** 启用已有的 `CallbackHandler` 框架，每个 OnlyOffice 状态码对应一个 Handler 实现。

**替代方案：**
- Controller 内 if-else 分发（旧系统方案）：代码臃肿，新增状态需修改 Controller
- 消息队列异步处理：回调必须 5 秒内响应，MQ 增加复杂度和延迟

**理由：** 旧系统已设计好 `CallbackHandler` + `DefaultCallbackManager` 框架，只需启用并实现各状态处理器。策略模式使每个状态处理逻辑独立，便于测试和维护。

**Handler 实现：**
```
CallbackStatusHandler (interface)
├── EditingCallbackHandler      (status=1)
├── SaveCallbackHandler          (status=2, 6)
├── CorruptedCallbackHandler     (status=3, 7)
└── ClosedCallbackHandler        (status=4)
```

**回调流程：**
```
OnlyOffice DS --POST /api/v1/document/callback--> DocumentCallbackController
  → 验证 OnlyOffice JWT header
  → CallbackManager.dispatch(status, body)
  → 对应 Handler 处理
  → 返回 {"error": 0} 或 {"error": 1}
```

### 3. 文档 Key 策略：hash(userFileId + lastModifiedTime)

**选择：** document key = `hash(userFileId + ":" + fileBean.modifyTime.toEpochMilli())`

**替代方案：**
- 使用 UUID 每次随机生成：无法判断内容是否变化
- 使用 FileBean.fileHash：fileHash 是上传时计算的，编辑保存后不会自动更新

**理由：** `lastModifiedTime` 在每次编辑保存时更新（通过 FileBean.onUpdate），OnlyOffice 通过 key 变化判断是否需要重新加载文档。保存后 `modifyTime` 变化 → key 变化 → OnlyOffice 加载新内容。

### 4. 权限模型：文件所有者 + 分享权限

**选择：** 通过 file 模块的 Service 接口检查权限，document 模块不直接查询分享表。

**权限规则：**
```
预览权限：文件所有者 OR 有分享查看权限 OR 文件所在目录有查看权限
编辑权限：文件所有者 OR 有分享编辑权限 OR 文件所在目录有编辑权限
```

**实现方式：** 在 file 模块新增 `FilePermissionService` 接口，提供 `canView(userId, userFileId)` 和 `canEdit(userId, userFileId)` 方法。document 模块通过此接口检查权限，不直接访问 share 模块。

### 5. 版本历史：document_version 表 + 文件存储

**选择：** 新增 `document_version` 表记录版本元数据，版本文件存储在独立目录（通过 UFOP Writer 写入历史路径）。

**替代方案：**
- 使用 OnlyOffice 内置 history：数据存储在 OnlyOffice 端，后端无法独立管理
- 每次保存创建完整 FileBean 副本：浪费存储空间

**表结构：**
```sql
CREATE TABLE document_version (
  version_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_file_id   BIGINT NOT NULL,
  file_id        BIGINT NOT NULL,         -- 指向旧 FileBean
  version_number INT NOT NULL,
  file_size      BIGINT NOT NULL,
  editor_id      BIGINT NOT NULL,         -- 编辑者 userId
  create_time    DATETIME NOT NULL,
  INDEX idx_user_file_version (user_file_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**版本管理策略：** 每次保存时，将当前 FileBean 信息记录为历史版本，然后创建新的 FileBean 存储编辑后文件。最多保留 10 个版本（可配置），超出时删除最旧版本。

### 6. COW（Copy-on-Write）：多引用时编辑前创建副本

**选择：** 编辑前检查 FileBean 被多少 UserFile 引用（`SELECT COUNT(*) FROM user_file WHERE file_id = ?`），若 > 1 则创建新的 FileBean 副本。

**理由：** 多个 UserFile 引用同一 FileBean 时（文件去重/秒传），直接编辑会影响其他引用。COW 确保编辑操作不影响其他用户的文件副本。

### 7. 模块包结构

```
com.qiwenshare.document/
├── config/
│   ├── OnlyOfficeProperties.java       // @ConfigurationProperties 绑定 onlyoffice.*
│   └── DocumentSecurityConfig.java     // 回调端点安全配置（放行 + JWT 验证）
├── controller/
│   ├── DocumentController.java         // 预览/编辑/历史 API
│   └── DocumentCallbackController.java // OnlyOffice 回调端点
├── service/
│   ├── DocumentPreviewService.java     // 预览配置构建
│   ├── DocumentEditService.java        // 编辑模式构建 + 权限检查 + COW
│   ├── DocumentCallbackService.java    // 回调分发 + 文件保存
│   ├── DocumentHistoryService.java     // 版本管理
│   ├── DocumentTokenService.java       // 文档/回调 token 生成与验证
│   └── DocumentConfigService.java      // OnlyOffice 配置查询
├── callback/
│   ├── CallbackStatusHandler.java      // 回调处理接口
│   ├── CallbackManager.java            // 回调分发器
│   ├── EditingCallbackHandler.java     // status=1
│   ├── SaveCallbackHandler.java        // status=2,6
│   ├── CorruptedCallbackHandler.java   // status=3,7
│   └── ClosedCallbackHandler.java      // status=4
├── dto/
│   ├── PreviewRequestDTO.java          // 预览请求
│   ├── EditRequestDTO.java             // 编辑请求
│   └── CallbackBodyDTO.java            // 回调请求体
├── vo/
│   ├── PreviewConfigVO.java            // 预览配置（OnlyOffice Config 对象）
│   ├── EditConfigVO.java               // 编辑配置
│   ├── DocumentVersionVO.java          // 版本信息
│   └── DocumentHealthVO.java           // 健康检查结果
├── entity/
│   └── DocumentVersion.java            // 版本记录实体
├── repository/
│   └── DocumentVersionRepository.java  // 版本数据访问
└── exception/
    ├── DocumentErrorCode.java          // 错误码枚举
    └── DocumentModuleException.java    // 模块异常
```

**注意：** `FilePermissionService` 接口定义在 file 模块，document 模块依赖此接口进行权限检查。

### 8. 格式转换降级

**选择：** 不可直接编辑但可转换的格式（如 .doc → .docx），通过 OnlyOffice Converter API 自动转换后编辑。

**转换流程：**
1. 检查文件格式是否在 `edited-extensions` 中 → 是直接编辑
2. 检查是否在 `convert-extensions` 中 → 调用 OnlyOffice Converter 转换
3. 转换成功 → 创建新的 FileBean（转换后文件），以新格式打开编辑
4. 转换失败 → 返回明确错误，禁止静默改变扩展名

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 回调处理超时（5 秒限制） | 文件保存操作异步执行，回调先返回成功。保存失败时记录日志，下次回调重试。使用 `@Async` 时注意跨类调用 AOP 代理（AGENTS.md 红线 #16）。 |
| OnlyOffice Document Server 不可用 | 健康检查端点监控连通性，不可用时预览/编辑 API 返回明确错误。不影响文件上传/下载等核心功能。 |
| COW 操作失败导致数据不一致 | COW 在事务中执行，失败时回滚。事务内不执行外部 IO（AGENTS.md 红线 #15），FileBean 创建和 UserFile 更新在同一事务中完成。 |
| 版本文件占用过多存储空间 | 最多保留 10 个版本（可配置），超出时自动删除最旧版本。版本文件通过 UFOP 存储在独立目录，支持定期清理。 |
| TokenService 扩展影响 auth 模块 | 新增方法为纯扩展（`generateDocumentToken` / `generateCallbackToken`），不修改现有方法签名。通过接口隔离，document 模块只依赖 token 生成/验证方法。 |
| 回调端点安全（伪造回调） | 回调端点验证 OnlyOffice JWT header（`onlyoffice.jwt.secret` 签名验证），不通过验证直接返回 403。回调 URL 中的短期 token 30 分钟过期。 |
| 格式转换失败静默改变文件 | 转换失败时返回 `CONVERT_FAILED` 错误，原始文件不变。禁止静默以新扩展名保存（AGENTS.md 红线 #13 精神）。 |
| `FilePermissionService` 新增接口影响 file 模块 | 接口为纯新增，不修改现有 file 模块逻辑。实现中复用已有的 UserFileRepository 和分享查询。 |
| COW 物理文件复制对大文件耗时 | 接近 50MB 上限的文件复制可能耗时较长。COW 操作设置超时（默认 30 秒），超时返回 `DOC_CONVERT_FAILED` 错误。编辑请求本身不在事务中等待复制完成，而是先返回配置，复制在后台异步执行。 |
