## ADDED Requirements

### Requirement: 后端端点一致性校验
前端API层 SHALL 与后端63个实际端点完全对齐。每个API函数的HTTP方法、路径、请求参数、响应类型 MUST 与后端Controller定义一致。

#### Scenario: 不存在端点降级处理
- **WHEN** 前端调用的后端端点不存在（如 `/notice/list`、`/notice/detail`、`/param/grouplist`）
- **THEN** 前端 MUST 移除或降级该API调用，使用硬编码默认值或从已有端点（如 `/admin/config`）获取数据
- **THEN** 页面 MUST 正常渲染，不因API 404而崩溃

#### Scenario: 用户配额接口接入
- **WHEN** 后端存在 `GET /api/v1/quota/info` 端点返回 `QuotaInfoVO`
- **THEN** 前端 MUST 调用该端点获取用户配额信息，替代或补充现有的 `getStorage()` 调用
- **THEN** sideMenu store 的存储容量条 MUST 使用正确的配额数据

#### Scenario: 分页类型统一
- **WHEN** 文件类端点返回 `PageResult<T>`（字段: content, totalElements, totalPages, number, size）
- **THEN** 前端 types/file.ts 的 `PageResult<T>` MUST 匹配后端字段
- **WHEN** 管理类端点返回 `PageResponse<T>`（字段: content, totalElements, totalPages, page, size, first, last）
- **THEN** 前端 types/admin.ts 的 `PageResponse<T>` MUST 匹配后端字段

#### Scenario: 请求参数名对齐
- **WHEN** 后端Controller使用 `@RequestParam page` 和 `@RequestParam pageSize`
- **THEN** 前端API函数 MUST 传递同名参数 `page` 和 `pageSize`，不得改为 `currentPage` 或 `pageCount`

### Requirement: API响应类型校正
前端类型定义 SHALL 与后端DTO/VO的字段名和类型完全一致。

#### Scenario: LoginResponse字段对齐
- **WHEN** 后端 `LoginResponse` 包含 `userId, username, roles, permissions`
- **THEN** 前端 `LoginResult` 类型 MUST 包含相同字段，字段名完全一致

#### Scenario: FileInfo字段对齐
- **WHEN** 后端 `FileListVO` 包含特定字段集合
- **THEN** 前端 `FileInfo` 类型 MUST 包含对应字段，不得遗漏或多余

#### Scenario: ShareInfo字段对齐
- **WHEN** 后端 `ShareInfoVO` 包含 `shareId, shareCode, extractCode, expireTime` 等字段
- **THEN** 前端 `ShareInfo` 类型 MUST 包含所有字段

### Requirement: 公开端点白名单对齐
前端路由守卫和API调用 SHALL 与后端Security配置中的公开端点一致。

#### Scenario: 分享页公开访问
- **WHEN** 用户未登录访问 `/share/:shareBatchNum`
- **THEN** 前端 MUST 允许访问，API调用 `getShareInfo` 和 `verifyShare` MUST 不携带认证要求

#### Scenario: 搜索重建需管理员权限
- **WHEN** 前端调用 `rebuildSearchIndex`
- **THEN** 该请求 MUST 发送到 `POST /api/v1/search/admin/rebuild`，且用户 MUST 具有 `admin:search-rebuild` 权限
