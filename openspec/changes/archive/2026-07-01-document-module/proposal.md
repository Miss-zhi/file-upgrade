# document-module — 在线文档模块升级提案

## 背景

旧项目 document 模块（OnlyOffice 在线文档预览/编辑）功能基本完整，但存在严重的架构问题：两套 JWT 系统共存、回调框架写了但生产代码未使用、权限检查完全缺失、文档 key 不更新导致缓存问题、SSL 全局关闭、多处硬编码。需要将 OnlyOffice 集成从"能用"升级为安全、可维护的生产级实现。

### 旧系统已知问题清单

| # | 问题 | 严重性 |
|---|------|--------|
| 1 | 两套 JWT 系统：应用级 JJWT（用户认证）vs OnlyOffice primeframework JWT（文档 token），密钥/算法/过期策略各自独立 | 安全/维护 |
| 2 | 回调框架（CallbackHandler + DefaultCallbackManager）完整实现但生产代码未使用，Controller 用 if-else 硬编码处理 | 架构 |
| 3 | 权限检查完全缺失——任何登录用户知道 userFileId 就能编辑任何文件 | 安全漏洞 |
| 4 | 文档 key = hash(userFileId + uploadTime)，上传后永不变，保存后 OnlyOffice 缓存旧内容 | 数据一致性 |
| 5 | SSL 证书验证全局关闭（`verify-peer-off=true`），影响所有 HTTPS 调用 | 安全 |
| 6 | preview URL 中硬编码字符串 `"undefined"` 作为分享参数 | 代码质量 |
| 7 | 文件大小限制 5MB（`filesize-max=5242880`）但上传允许 2GB，能力不匹配 | 用户体验 |
| 8 | HistoryManager 被注释掉，版本数据在写入但前端无法访问 | 功能缺失 |
| 9 | 回调 URL 中嵌入用户 token，token 过期后回调静默失败，编辑内容丢失 | 数据丢失 |
| 10 | 格式转换失败时静默以新扩展名保存，用户文件格式被悄悄改变 | 数据完整性 |

## 升级目标

重构 OnlyOffice 集成：统一 JWT 系统、启用回调框架、实现文件级权限检查、修复文档 key 更新策略、消除硬编码、提升文件大小限制、启用版本历史。

## Capabilities

### 1. document-preview（文档预览）

提供 OnlyOffice 文档在线预览功能，支持 50+ 种文件格式。

**范围：**
- 预览配置构建：DocumentType 自动分类（word/cell/slide）、文件格式判断（viewed/edited/convert/fillForms）
- 预览 URL 生成：通过 UFOP 存储后端生成临时下载 URL（带签名和过期时间），不再拼接硬编码参数
- OnlyOffice Config 构建：document（key/url/fileType/title）、editorConfig（callbackUrl/mode/lang）
- JWT token 生成：使用统一 JWT 系统（复用 auth 模块的 jjwt），文档 token 设置合理过期时间（默认 4 小时）
- 只读预览：对不可编辑格式或无编辑权限的用户，强制 view 模式
- 平台自适应：根据 User-Agent 自动切换 desktop/mobile 编辑器类型

**约束：**
- 预览 URL 必须有签名和过期时间（默认 1 小时），禁止永久有效的 URL
- 预览 URL 中禁止硬编码 `"undefined"` 等前端概念，通过参数可选性处理
- 文档 key 必须包含文件修改时间戳，保存后重新生成（确保 OnlyOffice 加载最新内容）
- 文件大小检查在预览前执行，超过 OnlyOffice 限制时返回明确错误提示

### 2. document-edit（文档编辑）

提供 OnlyOffice 文档在线编辑功能，支持协作编辑和格式转换降级。

**范围：**
- 编辑权限检查：验证当前用户对目标文件有写权限（文件所有者或有分享编辑权限）
- 编辑模式构建：Action.edit，权限设置（edit/comment/review 根据用户权限动态配置）
- COW（Copy-on-Write）：文件被多个 UserFile 引用时，编辑前先创建独立副本
- 格式转换降级：不可直接编辑但可转换的格式（如 .doc → .docx），自动转换后编辑
- 协作编辑配置：fast 模式（乐观并发），允许 change
- 文档 token：包含 userId、userFileId、过期时间，使用统一 JWT

**约束：**
- 编辑前必须检查文件权限（所有者 or 分享编辑权限），禁止仅凭 userFileId 编辑任意文件
- COW 操作必须在事务中完成，失败时回滚
- 格式转换失败时返回明确错误，禁止静默改变文件扩展名
- 同一文件同时编辑人数限制（可配置，默认 20）

### 3. document-callback（OnlyOffice 回调处理）

处理 OnlyOffice Document Server 的状态回调，实现文档保存、强制保存和错误处理。

**范围：**
- 回调端点：POST /api/v1/document/callback（新路径，符合 REST 规范）
- 回调鉴权：验证请求来源（OnlyOffice JWT header）+ 回调 token 验证
- 状态处理：使用策略模式分发（CallbackHandler 框架）
  - Status 1 (EDITING)：记录编辑状态，检测非协作用户时触发 forcesave
  - Status 2 (SAVE)：下载编辑后文件 → 写回 UFOP 存储 → 更新 MD5 → 更新版本 → 生成新 document key
  - Status 3 (CORRUPTED)：记录错误日志，返回 error=1
  - Status 4 (CLOSED_NO_EDIT)：正常关闭，无需处理
  - Status 6 (MUST_FORCE_SAVE)：同 SAVE 处理
  - Status 7 (CORRUPTED_FORCE_SAVE)：同 CORRUPTED 处理
- 文件保存：通过 UFOP StorageBackend 写入，重新计算 MD5
- 版本管理：保存时移动旧版本到历史目录，记录变更 diff
- 回调 URL 生成：使用短期 token（30 分钟），不依赖用户长期 token

**约束：**
- 回调端点必须有 OnlyOffice JWT 验证（防止伪造回调）
- 回调 token 使用独立的短期 token（30 分钟），不依赖用户 session token
- 文件保存必须更新 document key（追加时间戳或版本号），确保 OnlyOffice 下次加载新内容
- 保存失败（网络/存储异常）时返回 error=1，OnlyOffice 会重试
- 回调处理必须在 5 秒内返回响应（OnlyOffice 超时限制）
- 所有回调事件记录 INFO 日志（包括 status、userFileId、结果）

### 4. document-history（文档版本历史）

管理文档编辑的历史版本，支持版本查看和回滚。

**范围：**
- 版本存储：每次编辑保存时，将旧版本文件保存到历史目录
- 版本元数据：版本号、编辑者、编辑时间、文件大小
- 版本查询 API：GET /api/v1/document/{userFileId}/history（返回版本列表）
- 版本下载 API：GET /api/v1/document/{userFileId}/history/{version}（下载指定版本）
- 版本回滚：POST /api/v1/document/{userFileId}/history/{version}/restore（将指定版本恢复为当前版本）
- OnlyOffice history 集成：构建 history 对象（changes/url）传给前端编辑器，支持编辑器内查看 diff

**约束：**
- 版本数据写入和读取必须配对（旧系统只写不读，这次必须同时实现）
- 版本历史最多保留 10 个版本（可配置），超出时删除最旧版本
- 版本回滚等同于一次编辑保存，触发新的版本记录和 document key 更新

### 5. document-config（文档配置）

OnlyOffice 集成相关的配置管理。

**范围：**
- OnlyOffice 连接配置：`onlyoffice.server-url`、`onlyoffice.api-url`、`onlyoffice.converter-url`、`onlyoffice.command-url`
- JWT 配置：`onlyoffice.jwt.secret`、`onlyoffice.jwt.header`（与 Docker JWT_SECRET 一致）
- 文件限制配置：`onlyoffice.max-file-size`（默认 50MB，提升旧系统 5MB 限制）
- 格式配置：`onlyoffice.viewed-extensions`、`onlyoffice.edited-extensions`、`onlyoffice.convert-extensions`、`onlyoffice.fillforms-extensions`
- SSL 配置：`onlyoffice.ssl-verify`（默认 true，旧系统强制 false 是安全漏洞）
- 回调 URL 配置：`onlyoffice.callback-base-url`（不再从 deployment.host 硬拼）
- 健康检查：GET /api/v1/admin/document/health（检查 OnlyOffice Document Server 连通性）

**约束：**
- 所有配置从 application.yml 读取，禁止硬编码
- SSL 验证默认开启，仅开发环境可关闭
- JWT secret 不能硬编码，必须从环境变量或密钥管理服务读取（参照 SECURITY.md）
- 配置变更不需要重新编译

## 与现有模块的关系

| 模块 | 关系 | 说明 |
|------|------|------|
| file/ | 依赖 | 通过 UserFile/FileBean 获取文件元数据，通过 UFOP 读写文件 |
| auth/ | 依赖 | 用户认证（JWT token）、权限检查（文件所有者/分享权限） |
| storage/ | 依赖 | UFOP 框架提供文件下载 URL 生成、文件流读写 |
| share/ | 依赖 | 分享文件的预览/编辑权限判断 |
| admin/ | 被依赖 | 管理端提供 OnlyOffice 健康检查端点 |

## 技术方案要点

### JWT 统一方案

废弃旧系统的 primeframework JWT（`DefaultJwtManager`），统一使用 auth 模块的 JJWT（`jjwt 0.12.x`）。文档 token 和回调 token 使用独立的 claims 前缀（`doc.` 和 `cb.`），与用户认证 token 区分。

```
用户认证 token：sub=userId, exp=7天
文档 token：    sub=userId, doc.fileId=xxx, doc.action=edit, exp=4小时
回调 token：    sub=userId, cb.fileId=xxx, cb.type=edit, exp=30分钟
```

### 文档 Key 策略

旧系统 key = hash(userFileId + uploadTime)，永远不变。新系统 key = hash(userFileId + lastModifiedTime)，每次编辑保存后更新 lastModifiedTime，OnlyOffice 下次打开加载新内容。

### 权限模型

```
预览权限：文件所有者 OR 有分享查看权限 OR 文件所在目录有查看权限
编辑权限：文件所有者 OR 有分享编辑权限 OR 文件所在目录有编辑权限
```

### OnlyOffice 通信链路

```
Browser --load api.js--> OnlyOffice Document Server
Browser --POST /api/v1/document/preview--> Backend --> 返回 FileModel + docserviceApiUrl
Browser --DocsAPI.DocEditor(config)--> OnlyOffice DS
OnlyOffice DS --GET previewUrl--> Backend (UFOP signed URL) --> 下载原始文档
OnlyOffice DS --POST callbackUrl--> Backend --> 处理编辑/保存/关闭
```

## 不在范围内

- OnlyOffice Document Server 的部署和运维（Docker Compose 已配置）
- 前端 OnlyOffice 编辑器页面（属于 document-module-frontend）
- 实时协作编辑冲突解决（OnlyOffice 内部处理，后端无需干预）
- 文档模板管理（可作为后续迭代）
- PDF 在线标注/签名（后续迭代）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~28 个 Java 文件（controller, service, callback, config, dto, vo, entity, repository, exception） |
| 修改文件 | file 模块新增 FilePermissionService 接口和实现（权限检查），不修改现有 FileController |
| 新增依赖 | `io.jsonwebtoken:jjwt`（已在 auth 模块引入，复用） |
| Flyway | V7 新增 document_version 表 |
| 外部依赖 | OnlyOffice Document Server 8.x（Docker 容器） |
| docker-compose | 更新 OnlyOffice 镜像版本、环境变量 |
