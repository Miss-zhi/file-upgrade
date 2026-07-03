## ADDED Requirements

### Requirement: 文档预览配置构建
系统 SHALL 提供 `POST /api/v1/document/preview` 端点，接收 `userFileId`，返回 OnlyOffice 编辑器配置对象（Config）。系统 MUST 自动判断 DocumentType（word/cell/slide）、文件格式分类（viewed/edited/convert/fillForms），构建完整的 document + editorConfig 对象。

#### Scenario: 成功预览可编辑格式文件
- **WHEN** 认证用户请求预览一个 .docx 文件，且用户有查看权限
- **THEN** 系统返回 OnlyOffice Config 对象，包含 document（key/url/fileType/title）、editorConfig（callbackUrl/mode=view 或 edit/lang）
- **THEN** document.key = hash(userFileId + ":" + fileBean.modifyTime.toEpochMilli())
- **THEN** document.url 为带签名和过期时间（默认 1 小时）的临时下载 URL

#### Scenario: 预览只读格式文件
- **WHEN** 认证用户请求预览一个 .pdf 文件（不可编辑格式）
- **THEN** 系统返回 Config 对象，editorConfig.mode = "view"，editorConfig.permissions 不包含 edit/comment/review

#### Scenario: 无编辑权限用户预览
- **WHEN** 认证用户请求预览文件，用户有查看权限但无编辑权限
- **THEN** 系统返回 Config 对象，editorConfig.mode = "view"

#### Scenario: 预览文件超过大小限制
- **WHEN** 请求预览的文件大小超过 `onlyoffice.max-file-size`（默认 50MB）
- **THEN** 系统返回 HTTP 413 和错误码 `DOC_FILE_TOO_LARGE`

#### Scenario: 无权限预览
- **WHEN** 认证用户请求预览非本人文件，且无分享权限
- **THEN** 系统返回 HTTP 403 和错误码 `DOC_ACCESS_DENIED`

### Requirement: 预览 URL 签名与过期
系统 MUST 为 OnlyOffice 文档下载 URL 生成带签名和过期时间的临时链接，禁止永久有效的 URL。

#### Scenario: 生成带签名的预览 URL
- **WHEN** 系统构建预览配置
- **THEN** 通过 UFOP StorageBackend 生成临时下载 URL，过期时间默认 1 小时（可配置）

#### Scenario: 预览 URL 过期后访问
- **WHEN** OnlyOffice Document Server 使用过期的 URL 下载文件
- **THEN** 存储后端返回 403 或 404，OnlyOffice 提示文件不可访问

### Requirement: 文档 Token 生成
系统 MUST 使用统一 JWT 系统（auth 模块 TokenService）生成文档 token，禁止使用独立的 JWT 密钥。

#### Scenario: 生成文档 token
- **WHEN** 系统构建预览/编辑配置
- **THEN** 生成 JWT token，claims 包含 sub=userId, type=doc, doc.fileId=userFileId, doc.action=edit|view, exp=4小时
- **THEN** token 使用 auth 模块的签名密钥

### Requirement: 平台自适应编辑器类型
系统 SHALL 根据请求 User-Agent 自动切换 desktop/mobile 编辑器类型。

#### Scenario: 桌面浏览器请求预览
- **WHEN** 桌面浏览器请求预览文档
- **THEN** editorConfig 中回调 URL 和参数适配桌面模式

#### Scenario: 移动设备请求预览
- **WHEN** 移动设备请求预览文档
- **THEN** 系统返回适合移动端的编辑器配置
