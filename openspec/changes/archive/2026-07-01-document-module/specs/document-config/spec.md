## ADDED Requirements

### Requirement: OnlyOffice 连接配置
系统 MUST 从 application.yml 读取 OnlyOffice 连接配置，禁止硬编码。

#### Scenario: 配置项绑定
- **WHEN** 应用启动
- **THEN** 系统从 `onlyoffice.*` 配置项绑定 OnlyOfficeProperties
- **THEN** 配置项包含：server-url、api-url、converter-url、command-url、jwt.secret、jwt.header、max-file-size、ssl-verify、callback-base-url

#### Scenario: 必要配置缺失
- **WHEN** `onlyoffice.server-url` 未配置
- **THEN** 系统启动时抛出配置异常，阻止启动

### Requirement: 文件大小限制配置
系统 MUST 提供可配置的 OnlyOffice 文件大小限制，默认 50MB。

#### Scenario: 文件大小限制生效
- **WHEN** 预览/编辑请求的文件大小超过 `onlyoffice.max-file-size`
- **THEN** 系统返回 HTTP 413 和错误码 `DOC_FILE_TOO_LARGE`

#### Scenario: 默认限制 50MB
- **WHEN** `onlyoffice.max-file-size` 未显式配置
- **THEN** 系统使用默认值 50MB（52428800 字节）

### Requirement: 格式扩展名配置
系统 MUST 提供可配置的格式扩展名列表，控制预览/编辑/转换行为。

#### Scenario: 格式分类配置
- **WHEN** 系统判断文件格式分类
- **THEN** 从 `onlyoffice.viewed-extensions` 读取可预览格式列表
- **THEN** 从 `onlyoffice.edited-extensions` 读取可直接编辑格式列表
- **THEN** 从 `onlyoffice.convert-extensions` 读取可转换格式列表
- **THEN** 从 `onlyoffice.fillforms-extensions` 读取可填写表单格式列表

#### Scenario: 默认扩展名参考值
- **WHEN** 各扩展名列表未显式配置
- **THEN** viewed 默认：pdf, txt, csv, html, htm, xml, epub, djvu, xps, oxps
- **THEN** edited 默认：docx, xlsx, pptx, ppsx, odt, ods, odp, docm, xlsm, pptm
- **THEN** convert 默认：doc, xls, ppt, pps, odp, ods, odt, rtf, txt, csv, epub
- **THEN** fillForms 默认：docx, pdf, odt

### Requirement: SSL 验证配置
系统 MUST 默认开启 SSL 证书验证，仅开发环境可关闭。

#### Scenario: 生产环境 SSL 验证开启
- **WHEN** 应用以 prod profile 启动
- **THEN** `onlyoffice.ssl-verify` 默认为 true
- **THEN** OnlyOffice 通信使用 HTTPS 并验证证书

#### Scenario: 开发环境 SSL 验证关闭
- **WHEN** 应用以 dev profile 启动且配置 `onlyoffice.ssl-verify=false`
- **THEN** OnlyOffice 通信跳过 SSL 证书验证

### Requirement: 回调 URL 配置
系统 MUST 从配置读取回调基础 URL，禁止从 deployment.host 硬拼。

#### Scenario: 回调 URL 构建
- **WHEN** 系统构建编辑配置的 callbackUrl
- **THEN** 使用 `onlyoffice.callback-base-url` + `/api/v1/document/callback` 拼接
- **THEN** callback-base-url 为 OnlyOffice Document Server 可访问的后端地址

### Requirement: OnlyOffice 健康检查
系统 SHALL 提供 `GET /api/v1/admin/document/health` 端点，检查 OnlyOffice Document Server 连通性。

#### Scenario: OnlyOffice 服务正常
- **WHEN** 管理员请求健康检查，且 OnlyOffice Document Server 可达
- **THEN** 系统返回健康状态（status=UP, serverUrl, version 等）

#### Scenario: OnlyOffice 服务不可达
- **WHEN** 管理员请求健康检查，且 OnlyOffice Document Server 不可达
- **THEN** 系统返回健康状态（status=DOWN, error 信息）
- **THEN** 不影响文件管理核心功能

#### Scenario: 健康检查需要管理员权限
- **WHEN** 非管理员用户请求健康检查
- **THEN** 系统返回 HTTP 403
