## ADDED Requirements

### Requirement: 版本查询
系统 SHALL 提供 `GET /api/v1/document/{userFileId}/history` 端点，返回文件的版本历史列表。版本历史查询权限与预览权限一致（查看权限即可，无需编辑权限）。

#### Scenario: 查询有版本历史的文件
- **WHEN** 认证用户查询有编辑历史的文件版本，且用户有查看权限
- **THEN** 系统返回版本列表，每个版本包含 version_number、editor_id、file_size、create_time
- **THEN** 版本按 version_number 降序排列（最新在前）

#### Scenario: 查询无版本历史的文件
- **WHEN** 认证用户查询从未编辑过的文件版本
- **THEN** 系统返回空列表

#### Scenario: 无权限查询版本历史
- **WHEN** 用户查询非本人文件且无分享权限的版本历史
- **THEN** 系统返回 HTTP 403 和错误码 `DOC_ACCESS_DENIED`

### Requirement: 版本下载
系统 SHALL 提供 `GET /api/v1/document/{userFileId}/history/{version}` 端点，下载指定版本的文件。

#### Scenario: 成功下载指定版本
- **WHEN** 有权限的用户请求下载文件的第 N 个版本
- **THEN** 系统通过 UFOP StorageBackend 读取历史版本文件流，返回文件下载响应

#### Scenario: 版本不存在
- **WHEN** 用户请求下载不存在的版本号
- **THEN** 系统返回 HTTP 404 和错误码 `DOC_VERSION_NOT_FOUND`

### Requirement: 版本回滚
系统 SHALL 提供 `POST /api/v1/document/{userFileId}/history/{version}/restore` 端点，将指定版本恢复为当前版本。

#### Scenario: 成功回滚到历史版本
- **WHEN** 有编辑权限的用户请求回滚到第 N 个版本
- **THEN** 系统将历史版本文件恢复为当前文件（等同于一次编辑保存）
- **THEN** 创建新的 DocumentVersion 记录（回滚操作本身），记录 editor_id 和操作类型为 RESTORE
- **THEN** 更新 FileBean.modifyTime（触发 document key 变化）
- **THEN** OnlyOffice history 对象中的新条目显示为"回滚到版本 N"，与普通编辑保存区分

#### Scenario: 无编辑权限回滚
- **WHEN** 用户无编辑权限尝试回滚
- **THEN** 系统返回 HTTP 403 和错误码 `DOC_ACCESS_DENIED`

### Requirement: 版本数量限制
系统 MUST 限制每个文件的最大版本数量，超出时自动删除最旧版本。

#### Scenario: 版本数量达到上限
- **WHEN** 文件保存触发新版本创建，当前版本数已达上限（默认 10 个，可配置）
- **THEN** 系统删除最旧的版本记录和历史文件
- **THEN** 创建新版本记录

#### Scenario: 版本数量未达上限
- **WHEN** 文件保存触发新版本创建，当前版本数未达上限
- **THEN** 系统直接创建新版本记录，不删除旧版本

### Requirement: OnlyOffice History 集成
系统 SHALL 构建 OnlyOffice history 对象传给前端编辑器，支持编辑器内查看版本 diff。

#### Scenario: 编辑配置包含 history 对象
- **WHEN** 用户请求编辑有历史版本的文件
- **THEN** 编辑配置中包含 history 对象（currentVersion、history 数组）
- **THEN** 每个 history 条目包含 changes（操作者、时间）和 url（版本文件下载 URL）
