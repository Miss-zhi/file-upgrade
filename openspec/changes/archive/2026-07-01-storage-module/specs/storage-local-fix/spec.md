## ADDED Requirements

### Requirement: Local 后端路径遍历防护
`LocalStorageBackend.resolvePath()` SHALL 在返回解析路径前执行安全校验，防止路径遍历攻击。

#### Scenario: 合法路径正常解析
- **WHEN** 传入合法 storagePath（如 `2026/07/01/abc.txt`）
- **THEN** 系统返回 basePath 下的正确路径，操作正常执行

#### Scenario: 路径遍历攻击被拦截
- **WHEN** 传入包含 `../` 的 storagePath（如 `../../etc/passwd`）
- **THEN** 系统 MUST 对路径执行 `normalize()` + `startsWith(basePath)` 校验
- **THEN** 校验失败时抛出 `SecurityException`，异常消息包含被拒绝的路径

#### Scenario: 绝对路径逃逸被拦截
- **WHEN** 传入绝对路径（如 `/etc/passwd`）作为 storagePath
- **THEN** normalize 后路径不在 basePath 下，系统 MUST 抛出 `SecurityException`

### Requirement: downloadRange 资源安全释放
`LocalStorageBackend.downloadRange()` SHALL 使用 try-with-resources 管理 `RandomAccessFile` 资源。

#### Scenario: 正常读取资源正确释放
- **WHEN** 执行 downloadRange 操作成功
- **THEN** `RandomAccessFile` 在 try-with-resources 块中自动关闭，无资源泄露

#### Scenario: 异常时资源正确释放
- **WHEN** downloadRange 过程中发生 IOException
- **THEN** `RandomAccessFile` 通过 try-with-resources 自动关闭，不泄露文件句柄

### Requirement: Local 后端测试补充
系统 SHALL 为 Local 后端补充单元测试覆盖。

#### Scenario: write 方法测试
- **WHEN** 调用 `write()` 写入文件
- **THEN** 文件内容正确写入，可被 `read()` 读回

#### Scenario: getPreviewUrl 返回 null
- **WHEN** 调用 `getPreviewUrl()`
- **THEN** 返回 null（本地存储不支持预览 URL）

#### Scenario: checkConnectivity 断言
- **WHEN** 调用 `checkConnectivity()`
- **THEN** 测试 MUST 包含明确的断言（`assertTrue` 或 `assertFalse`），验证 basePath 可写时返回 true

#### Scenario: 路径遍历安全测试
- **WHEN** 传入 `../../etc/passwd` 等恶意路径
- **THEN** 系统抛出 `SecurityException`
