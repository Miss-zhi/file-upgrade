## Why

奇文网盘从 Spring Boot 2 升级到 Spring Boot 3，文件管理核心模块（file module）需要完全重写。当前旧系统基于 javax 命名空间、Spring Boot 2 + Vue 2 技术栈，无法利用 Spring Boot 3 的性能改进和 Jakarta EE 标准化。文件模块是网盘系统的核心功能，涵盖上传、下载、CRUD、回收站、分享和存储抽象，必须在新的技术栈上重新实现以确保类型安全、可维护性和可扩展性。

## What Changes

- 新增 UFOP 统一存储工厂框架，通过工厂模式抽象 5 种存储后端（Local / MinIO / AliyunOSS / Qiniu / FastDFS），使用 Spring Boot 3 `AutoConfiguration.imports` 注册
- 新增文件上传能力：普通上传 + 分片上传（>10MB 强制分片）+ 秒传（基于文件 hash 去重），三层大小限制校验（前端→网关→Service）
- 新增文件下载能力：流式下载 + 断点续传（Range 请求支持）
- 新增文件 CRUD 能力：重命名、移动、创建文件夹、文件列表浏览
- 新增回收站能力：软删除 → 恢复 / 永久删除，永久删除异步执行
- 新增文件分享能力：生成分享链接 + 提取码 + 有效期控制
- 新增用户存储配额管理：上传前预扣空间、完成后确认、失败时释放

## Capabilities

### New Capabilities

- `file-upload`: 文件上传全链路——普通上传、分片上传（>10MB）、秒传/文件去重（hash 校验）、三层大小限制校验、配额预扣、上传进度追踪、错误码规范
- `file-download`: 文件下载全链路——流式下载（<50MB）、断点续传下载（Range 请求，≥50MB）、审计日志
- `file-operations`: 文件 CRUD 操作——重命名、移动、创建文件夹、文件列表查询、文件详情查看
- `file-recovery`: 回收站管理——软删除（移入回收站）、恢复（从回收站还原）、永久删除（异步清理存储对象 + 元数据）
- `file-sharing`: 文件分享——生成分享链接、提取码、有效期控制、分享列表查询
- `ufop-storage`: UFOP 统一存储工厂——7 个操作接口（Uploader/Downloader/Copier/Deleter/Previewer/Reader/Writer）、5 种存储后端实现、AutoConfiguration.imports 注册、启动时连通性验证
- `storage-quota`: 用户存储配额管理——配额查询、上传预扣/确认/释放、配额超限拒绝

### Modified Capabilities

（无现有 spec 需要修改，所有文件模块能力均为新增）

## Impact

- **后端新增模块**：`file/` 模块（controller / service / repository / entity / dto / vo），`storage/` 模块（UFOP 框架 + 5 种存储后端实现）
- **数据库**：新增 Flyway 迁移脚本，创建文件元数据表（FileBean / UserFile / ShareFile / UploadTask / UploadTaskDetail / RecycleBin 等）
- **依赖**：JPA (Hibernate) 6.x + MyBatis-Plus 3.5.x、Redis 7.x（缓存/分布式锁/上传进度）、UFOP Starter
- **API**：新增 `/api/v1/file/` 和 `/api/v1/share/` REST 端点
- **配置**：`application.yml` 新增存储后端配置、文件大小限制、UFOP 工厂配置
- **前端**：新增文件管理页面组件（上传、下载、文件列表、回收站、分享）
- **与 auth 模块集成**：文件操作需要 JWT 认证，配额关联用户 ID
