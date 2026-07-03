# preview-office — OnlyOffice 集成

## Purpose

通过 OnlyOffice Document Server 提供 Office 和 PDF 文件的在线预览与编辑能力。用户在文件列表中打开 Office/PDF 文件时，系统在新标签页加载 OnlyOffice DocEditor。

## Requirements

### Requirement: OnlyOffice 预览页面

系统 SHALL 提供独立的 Office 预览页面 `/preview/office`，通过 OnlyOffice Document Server 在线预览 Office 和 PDF 文件。

#### Scenario: 打开 Office 文件预览
- **WHEN** 用户打开 doc/docx/xls/xlsx/ppt/pptx 文件
- **THEN** 系统在新标签页打开 `/preview/office?userFileId=xxx`，加载 OnlyOffice 文档编辑器

#### Scenario: 打开 PDF 文件预览
- **WHEN** 用户打开 PDF 文件
- **THEN** 系统在新标签页打开 `/preview/office?userFileId=xxx`

### Requirement: 文档预览配置获取

系统 SHALL 调用 `POST /api/v1/document/preview` 获取 OnlyOffice DocEditor 配置。

#### Scenario: 成功获取预览配置
- **WHEN** OfficePreview 页面加载
- **THEN** 系统调用 document preview API，获取包含 document、editorConfig、token 的预览配置

#### Scenario: 预览配置获取失败
- **WHEN** document preview API 返回错误
- **THEN** 页面显示错误提示信息

### Requirement: OnlyOffice 编辑器初始化

系统 SHALL 使用 OnlyOffice API JS 初始化 DocEditor，编辑器占满全屏（100vw × 100vh）。

#### Scenario: 编辑器初始化
- **WHEN** 预览配置成功获取
- **THEN** 系统加载 OnlyOffice API JS，调用 `new DocsAPI.DocEditor('editor', config)` 初始化编辑器

#### Scenario: 编辑器占满全屏
- **WHEN** 编辑器初始化完成
- **THEN** 编辑器容器占满整个浏览器视口（100vw × 100vh）

### Requirement: OnlyOffice API JS 加载

系统 SHALL 从 OnlyOffice Document Server 加载 API JS 脚本。

#### Scenario: API JS 加载
- **WHEN** OfficePreview 页面加载
- **THEN** 系统从环境变量 `VITE_ONLYOFFICE_API_URL` 获取 OnlyOffice Document Server 地址，动态加载脚本

### Requirement: 文档编辑支持

系统 SHALL 支持通过 `POST /api/v1/document/edit` 获取编辑配置，用于在线编辑模式。

#### Scenario: 获取编辑配置
- **WHEN** 用户通过"在线编辑"入口打开 Office 文件
- **THEN** 系统调用 document edit API 获取编辑模式配置（支持 COW 写时复制）

### Requirement: 页面路由守卫

系统 SHALL 对 `/preview/office` 路由添加登录校验。

#### Scenario: 未登录访问
- **WHEN** 未登录用户访问 `/preview/office`
- **THEN** 系统重定向到登录页面

#### Scenario: 已登录访问
- **WHEN** 已登录用户访问 `/preview/office?userFileId=xxx`
- **THEN** 页面正常加载并初始化 OnlyOffice 编辑器
