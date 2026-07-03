## ADDED Requirements

### Requirement: 流式文件下载
系统 SHALL 支持文件流式下载。小文件（< 50MB）直接通过 UFOP Downloader 流式返回，禁止全量加载到内存。下载操作 MUST 记录审计日志。

#### Scenario: 成功下载小文件
- **WHEN** 认证用户请求下载一个 < 50MB 的文件（通过 fileId）
- **THEN** 系统通过 UFOP Downloader 获取文件流，设置正确的 Content-Type 和 Content-Disposition 响应头
- **THEN** 以流式方式写入 HTTP 响应体，记录审计日志（用户 ID、文件 ID、下载时间、IP）

#### Scenario: 下载不存在的文件
- **WHEN** 用户请求下载不存在的 fileId
- **THEN** 系统返回 HTTP 404 和相应错误信息

#### Scenario: 下载无权限的文件
- **WHEN** 用户请求下载不属于自己的且未分享的文件
- **THEN** 系统返回 HTTP 403 和相应错误信息

### Requirement: 断点续传下载
系统 SHALL 对大文件（≥ 50MB）支持 HTTP Range 请求，实现断点续传下载。客户端可通过 Range 头指定下载起始位置。

#### Scenario: 首次请求大文件下载
- **WHEN** 客户端请求下载 ≥ 50MB 文件，未携带 Range 头
- **THEN** 系统返回完整文件流，响应头包含 `Accept-Ranges: bytes` 和 `Content-Length`

#### Scenario: 断点续传请求
- **WHEN** 客户端携带 `Range: bytes=START-END` 头请求大文件
- **THEN** 系统解析 Range 头，通过 UFOP Downloader 从指定位置开始读取
- **THEN** 返回 HTTP 206 Partial Content，设置 `Content-Range: bytes START-END/TOTAL`
- **THEN** 仅返回请求范围内的数据

#### Scenario: Range 请求超出文件范围
- **WHEN** 客户端携带的 Range 头起始位置超出文件大小
- **THEN** 系统返回 HTTP 416 Requested Range Not Satisfiable

### Requirement: 分享文件下载
系统 SHALL 支持通过分享链接下载文件。用户提供分享 code 和提取码访问文件。

#### Scenario: 通过分享链接下载
- **WHEN** 用户提供有效的分享 code 和正确的提取码
- **THEN** 系统验证分享未过期，允许下载分享的文件
- **THEN** 记录分享下载审计日志

#### Scenario: 分享链接已过期
- **WHEN** 用户访问已过期的分享链接
- **THEN** 系统返回 HTTP 410 Gone 和相应错误信息

#### Scenario: 提取码错误
- **WHEN** 用户提供错误的提取码
- **THEN** 系统返回 HTTP 401 和相应错误信息
