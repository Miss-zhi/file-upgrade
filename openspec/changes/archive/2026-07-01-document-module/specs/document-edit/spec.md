## ADDED Requirements

### Requirement: 编辑权限检查
系统 MUST 在编辑前验证当前用户对目标文件有写权限。禁止仅凭 userFileId 编辑任意文件。

#### Scenario: 文件所有者编辑
- **WHEN** 文件所有者请求编辑自己的文件
- **THEN** 系统允许编辑，返回编辑模式配置

#### Scenario: 有分享编辑权限的用户编辑
- **WHEN** 被分享用户有编辑权限，请求编辑分享文件
- **THEN** 系统允许编辑，返回编辑模式配置

#### Scenario: 无编辑权限用户尝试编辑
- **WHEN** 用户无编辑权限（仅有查看权限或无权限），请求编辑文件
- **THEN** 系统返回 HTTP 403 和错误码 `DOC_ACCESS_DENIED`

#### Scenario: 编辑非本人文件且无分享
- **WHEN** 用户尝试编辑非本人文件，且无任何分享权限
- **THEN** 系统返回 HTTP 403 和错误码 `DOC_ACCESS_DENIED`

### Requirement: 编辑模式构建
系统 SHALL 提供 `POST /api/v1/document/edit` 端点，接收 `userFileId`，返回 OnlyOffice 编辑模式配置。

#### Scenario: 成功构建编辑配置
- **WHEN** 有编辑权限的用户请求编辑 .docx 文件
- **THEN** 系统返回 Config 对象，editorConfig.mode = "edit"，permissions 包含 edit=true
- **THEN** editorConfig.callbackUrl 包含短期 token（30 分钟过期）
- **THEN** 协作编辑配置：fast 模式（乐观并发），允许 change

#### Scenario: 编辑不可直接编辑但可转换的格式
- **WHEN** 用户请求编辑 .doc 文件（不可直接编辑但在 convert-extensions 中）
- **THEN** 系统调用 OnlyOffice Converter API 将 .doc 转换为 .docx
- **THEN** 创建新的 FileBean 存储转换后文件，以 .docx 格式打开编辑

#### Scenario: 格式转换失败
- **WHEN** 格式转换调用失败
- **THEN** 系统返回 HTTP 422 和错误码 `DOC_CONVERT_FAILED`
- **THEN** 原始文件不变，禁止静默改变文件扩展名

### Requirement: COW（Copy-on-Write）
当 FileBean 被多个 UserFile 引用时，编辑前 MUST 创建独立副本，避免编辑影响其他引用。

#### Scenario: 多引用文件编辑触发 COW
- **WHEN** 用户编辑一个 FileBean 被 2 个以上 UserFile 引用的文件
- **THEN** 系统创建新的 FileBean 副本（复制物理文件），更新当前 UserFile 的 fileId 指向新 FileBean
- **THEN** COW 操作在事务中完成，失败时回滚

#### Scenario: 单引用文件编辑不触发 COW
- **WHEN** 用户编辑一个 FileBean 仅被 1 个 UserFile 引用的文件
- **THEN** 系统直接编辑，不创建副本

### Requirement: 编辑回调 URL 使用短期 Token
回调 URL MUST 使用独立的短期 token（30 分钟），不依赖用户长期 session token。

#### Scenario: 生成带短期 token 的回调 URL
- **WHEN** 系统构建编辑配置
- **THEN** 回调 URL 包含 callback token（type=cb, exp=30分钟）
- **THEN** token 过期后回调返回 401，OnlyOffice 重新请求配置

#### Scenario: 回调 token 验证失败
- **WHEN** 回调请求携带无效或过期的 token
- **THEN** 系统返回 HTTP 401
