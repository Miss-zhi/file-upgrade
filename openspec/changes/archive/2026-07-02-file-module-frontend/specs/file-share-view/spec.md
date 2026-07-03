## ADDED Requirements

### Requirement: 分享信息获取
系统 SHALL 在 ShareView 加载时获取分享文件信息。

#### Scenario: 获取分享信息
- **WHEN** 用户访问 `/share/:shareBatchNum`
- **THEN** 调用 `getShareInfo(shareBatchNum)` 获取分享文件信息

#### Scenario: 分享不存在
- **WHEN** 分享码无效或已过期
- **THEN** 显示"分享链接已失效"错误提示

### Requirement: 提取码验证
系统 SHALL 支持提取码验证后访问分享文件。

#### Scenario: 需要提取码
- **WHEN** 分享信息返回需要提取码（extractCode 不为 null）
- **THEN** 显示提取码输入框

#### Scenario: 提取码正确
- **WHEN** 用户输入正确提取码并点击"提取文件"
- **THEN** 调用 `verifyShare` API，验证成功后显示文件列表

#### Scenario: 提取码错误
- **WHEN** 用户输入错误提取码
- **THEN** 显示"提取码错误"提示

#### Scenario: 无需提取码
- **WHEN** 分享信息返回 extractCode 为 null
- **THEN** 直接显示文件列表，无需验证

### Requirement: 分享文件列表展示
系统 SHALL 在验证通过后展示分享的文件列表。

#### Scenario: 显示分享文件信息
- **WHEN** 提取码验证成功
- **THEN** 显示文件名、大小、过期时间

#### Scenario: 分享已过期
- **WHEN** 分享 expireTime 早于当前时间
- **THEN** 显示"分享已过期"提示，不显示文件列表

### Requirement: 分享文件下载
系统 SHALL 支持从分享页面下载文件。

#### Scenario: 点击下载
- **WHEN** 用户点击下载按钮
- **THEN** 调用 `downloadShareFile(shareCode)` 触发下载

### Requirement: 保存到我的网盘
系统 SHALL 支持登录用户将分享文件保存到自己的网盘。

#### Scenario: 显示保存按钮
- **WHEN** 用户已登录
- **THEN** 显示"保存到我的网盘"按钮

#### Scenario: 保存文件
- **WHEN** 用户点击"保存到我的网盘"
- **THEN** 打开 SaveShareDialog，选择目标路径后保存

### Requirement: 公开端点无需登录
系统 SHALL 确保 ShareView 为公开页面，无需登录即可访问。

#### Scenario: 未登录访问
- **WHEN** 未登录用户访问 `/share/:shareBatchNum`
- **THEN** 允许访问，不跳转登录页
