## ADDED Requirements

### Requirement: 回调端点与鉴权
系统 SHALL 提供 `POST /api/v1/document/callback` 端点接收 OnlyOffice 状态回调。回调端点 MUST 验证请求来源（OnlyOffice JWT header），防止伪造回调。

#### Scenario: 合法回调请求
- **WHEN** OnlyOffice Document Server 发送回调请求，header 中包含有效的 JWT 签名
- **THEN** 系统验证 JWT 签名（使用 `onlyoffice.jwt.secret`），处理回调，返回 `{"error": 0}`

#### Scenario: 伪造回调请求
- **WHEN** 回调请求 header 中 JWT 签名无效或缺失
- **THEN** 系统返回 HTTP 403，不处理回调内容

#### Scenario: 回调端点不需要用户认证
- **WHEN** OnlyOffice 发送回调请求（无用户 session）
- **THEN** 回调端点放行 Spring Security 过滤，通过 OnlyOffice JWT 验证

### Requirement: 回调状态分发（策略模式）
系统 MUST 使用策略模式分发回调状态处理，每个 OnlyOffice 状态码对应独立的 Handler 实现。

#### Scenario: Status 1 (EDITING) 处理
- **WHEN** 回调 status=1（正在编辑）
- **THEN** 系统记录编辑状态日志
- **THEN** 若检测到非协作用户（仅 1 人编辑），触发 forcesave

#### Scenario: Status 2 (SAVE) 处理
- **WHEN** 回调 status=2（保存）且 users 数组非空
- **THEN** 系统分两阶段处理：
  - 阶段一（外部 IO，无事务）：通过 OnlyOffice download URL 下载编辑后文件流 → 通过 UFOP StorageBackend Writer 写入存储 → 重新计算文件 hash
  - 阶段二（事务内，纯 DB 操作）：更新 FileBean.fileHash、FileBean.fileSize、FileBean.modifyTime → 创建 DocumentVersion 记录
- **THEN** 两阶段之间若失败，阶段一的临时文件清理，阶段二不执行
- **THEN** 返回 `{"error": 0}`

#### Scenario: Status 3 (CORRUPTED) 处理
- **WHEN** 回调 status=3（文档损坏）
- **THEN** 系统记录 ERROR 级别日志，包含 userFileId 和错误详情
- **THEN** 返回 `{"error": 1}`

#### Scenario: Status 4 (CLOSED_NO_EDIT) 处理
- **WHEN** 回调 status=4（关闭且无编辑）
- **THEN** 系统正常关闭，无需处理，返回 `{"error": 0}`

#### Scenario: Status 6 (MUST_FORCE_SAVE) 处理
- **WHEN** 回调 status=6（强制保存）
- **THEN** 系统执行与 status=2 相同的保存逻辑

#### Scenario: Status 7 (CORRUPTED_FORCE_SAVE) 处理
- **WHEN** 回调 status=7（损坏的强制保存）
- **THEN** 系统执行与 status=3 相同的错误处理逻辑

### Requirement: 回调文件保存
回调保存操作 MUST 更新 FileBean 的 hash 和 modifyTime，确保 OnlyOffice 下次加载新内容。

#### Scenario: 保存编辑后文件
- **WHEN** 回调触发文件保存（status=2 或 6）
- **THEN** 阶段一（无事务）：通过 OnlyOffice download URL 下载编辑后文件流 → 通过 UFOP StorageBackend Writer 写入存储
- **THEN** 阶段二（事务内）：重新计算文件 hash，更新 FileBean.fileHash 和 FileBean.fileSize → 更新 FileBean.modifyTime（触发 @PreUpdate）
- **THEN** 若阶段一失败，清理临时文件，返回 `{"error": 1}`，不进入阶段二

#### Scenario: 保存失败
- **WHEN** 文件下载或存储写入失败（网络异常/存储异常）
- **THEN** 系统返回 `{"error": 1}`，OnlyOffice 会重试
- **THEN** 记录 ERROR 日志，包含 userFileId 和异常详情

#### Scenario: 保存触发版本记录
- **WHEN** 文件保存成功
- **THEN** 系统创建 DocumentVersion 记录（旧版本元数据）
- **THEN** 版本记录包含 version_number、editor_id、file_size、create_time

### Requirement: 回调处理超时控制
回调处理 MUST 在 5 秒内返回响应（OnlyOffice 超时限制）。

#### Scenario: 正常保存在 5 秒内完成
- **WHEN** 文件大小在合理范围内（≤50MB）
- **THEN** Handler 将文件保存操作提交到异步线程（@Async 跨类调用），自身立即返回 `{"error": 0}`
- **THEN** 异步线程完成下载 + 写入 + DB 更新

#### Scenario: 保存操作超时
- **WHEN** 文件下载或存储写入超过 5 秒
- **THEN** 异步线程记录 WARN 日志，标记需要重试
- **THEN** 回调已返回 `{"error": 0}`，OnlyOffice 不会重试；下次打开文档时重新触发保存流程

### Requirement: 回调日志记录
所有回调事件 MUST 记录 INFO 级别日志。

#### Scenario: 记录回调事件
- **WHEN** 系统收到任何回调请求
- **THEN** 记录 INFO 日志，包含 status、userFileId（从 callback token 提取）、处理结果
