## 1. 基础设施与配置

- [x] 1.1 创建 document 模块包结构（com.qiwenshare.document 下 config/controller/service/callback/dto/vo/entity/repository/exception）
- [x] 1.2 创建 OnlyOfficeProperties（@ConfigurationProperties 绑定 onlyoffice.*），包含 server-url、api-url、converter-url、command-url、jwt.secret、jwt.header、max-file-size（默认 50MB）、ssl-verify（默认 true）、callback-base-url、格式扩展名列表、max-version-count（默认 10）
- [x] 1.3 创建 DocumentErrorCode 枚举（DOC_ACCESS_DENIED、DOC_FILE_TOO_LARGE、DOC_CONVERT_FAILED、DOC_VERSION_NOT_FOUND、DOC_SERVER_UNAVAILABLE、DOC_CALLBACK_AUTH_FAILED）
- [x] 1.4 创建 DocumentModuleException（继承 RuntimeException，包含 ErrorCode）
- [x] 1.5 在 application.yml 添加 onlyoffice 配置段（开发环境默认值）
- [x] 1.6 创建 Flyway V7 迁移脚本：document_version 表

## 2. JWT Token 扩展

- [x] 2.1 在 TokenService 中新增 generateDocumentToken(userId, userFileId, action) 方法，claims: type=doc, doc.fileId, doc.action, exp=4小时
- [x] 2.2 在 TokenService 中新增 generateCallbackToken(userId, userFileId, type) 方法，claims: type=cb, cb.fileId, cb.type, exp=30分钟
- [x] 2.3 在 TokenService 中新增 parseDocumentToken(token) 和 parseCallbackToken(token) 方法，验证 type claim
- [x] 2.4 编写 TokenService 文档/回调 token 相关单元测试

## 3. 权限检查（⚠️ 以下任务修改 file 模块，非 document 模块）

- [x] 3.1 在 file 模块创建 FilePermissionService 接口，定义 canView(userId, userFileId) 和 canEdit(userId, userFileId) 方法（⚠️ file 模块新增文件）
- [x] 3.2 在 file 模块实现 FilePermissionServiceImpl：文件所有者直接通过，否则查询分享权限（⚠️ file 模块新增文件）
- [x] 3.3 编写 FilePermissionService 单元测试

## 4. 回调框架

- [x] 4.1 创建 CallbackStatusHandler 接口（handle(CallbackContext) 方法）
- [x] 4.2 创建 CallbackManager（注入所有 CallbackStatusHandler 实现，按 status 分发）
- [x] 4.3 创建 CallbackBodyDTO（status、users、url、key 等字段）
- [x] 4.4 实现 EditingCallbackHandler（status=1）：记录日志，检测非协作用户触发 forcesave
- [x] 4.5 实现 SaveCallbackHandler（status=2,6）：分两阶段处理——阶段一（无事务）：HTTP 下载编辑后文件 → UFOP 写入 → 计算 hash；阶段二（事务内）：更新 FileBean hash/modifyTime → 创建 DocumentVersion 记录。Handler 将阶段一提交到异步线程（@Async 跨类调用，注意红线 #16），自身立即返回 `{"error": 0}`
- [x] 4.6 实现 CorruptedCallbackHandler（status=3,7）：记录 ERROR 日志，返回 error=1
- [x] 4.7 实现 ClosedCallbackHandler（status=4）：正常关闭，无操作
- [x] 4.8 编写 CallbackManager 和各 Handler 单元测试

## 5. 版本历史

- [x] 5.1 创建 DocumentVersion 实体（version_id、user_file_id、file_id、version_number、file_size、editor_id、create_time）
- [x] 5.2 创建 DocumentVersionRepository（JPA Repository，按 userFileId 查询、按 version_number 排序、统计数量）
- [x] 5.3 创建 DocumentHistoryService：创建版本、查询版本列表、查询指定版本、删除最旧版本
- [x] 5.4 编写 DocumentHistoryService 单元测试

## 6. 预览与编辑服务

- [x] 6.1 创建 DocumentPreviewService：构建预览 Config（document key/url/type/title + editorConfig callbackUrl/mode/lang），权限检查，大小检查，格式分类
- [x] 6.2 创建 DocumentEditService：构建编辑 Config（mode=edit + permissions），COW 逻辑（检查 FileBean 引用数，>1 时创建副本），格式转换降级
- [x] 6.3 创建 DocumentTokenService（封装 TokenService 的文档/回调 token 生成与验证）
- [x] 6.4 编写 DocumentPreviewService 单元测试
- [x] 6.5 编写 DocumentEditService 单元测试（含 COW 和格式转换场景）

## 7. Controller 层

- [x] 7.1 创建 DocumentController：POST /api/v1/document/preview、POST /api/v1/document/edit、GET /api/v1/document/{userFileId}/history、GET /api/v1/document/{userFileId}/history/{version}、POST /api/v1/document/{userFileId}/history/{version}/restore
- [x] 7.2 创建 DocumentCallbackController：POST /api/v1/document/callback，放行 Spring Security，验证 OnlyOffice JWT
- [x] 7.3 创建 DocumentAdminController：GET /api/v1/admin/document/health（管理员权限）
- [x] 7.4 配置 DocumentSecurityConfig：回调端点放行用户认证，仅验证 OnlyOffice JWT
- [x] 7.5 编写 DocumentController 单元测试（MockMvc）
- [x] 7.6 编写 DocumentCallbackController 单元测试

## 8. DTO/VO 与异常处理

- [x] 8.1 创建 PreviewRequestDTO（userFileId）、EditRequestDTO（userFileId）
- [x] 8.2 创建 PreviewConfigVO、EditConfigVO（OnlyOffice Config 对象结构）
- [x] 8.3 创建 DocumentVersionVO（version_number、editor_id、file_size、create_time）
- [x] 8.4 创建 DocumentHealthVO（status、serverUrl、error）

## 9. 集成与配置

- [x] 9.1 在 application.yml 添加完整 onlyoffice 配置段
- [x] 9.2 更新 docker-compose 配置（OnlyOffice 环境变量、JWT_SECRET 同步）
- [x] 9.3 全局异常处理器添加 DocumentModuleException 处理
