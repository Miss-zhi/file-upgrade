## ADDED Requirements

### Requirement: 创建分享链接
系统 SHALL 支持用户创建文件分享链接。分享链接包含 8 位随机 code、提取码（4-6 位）、过期时间。

#### Scenario: 创建带提取码的分享链接
- **WHEN** 用户请求分享文件（提供 fileId、有效期类型），系统生成分享链接
- **THEN** 系统创建 ShareFile 记录，生成 8 位随机 shareCode、4-6 位随机提取码、根据有效期计算过期时间
- **THEN** 返回分享链接（shareCode）和提取码

#### Scenario: 创建无提取码的公开分享
- **WHEN** 用户请求分享文件并选择无提取码模式
- **THEN** 系统创建 ShareFile 记录，提取码为空
- **THEN** 任何人通过 shareCode 即可访问文件

#### Scenario: 设置有效期
- **WHEN** 用户选择有效期（1天/7天/30天/永久）
- **THEN** 系统根据选择计算 expireTime，永久分享时 expireTime 为 null

### Requirement: 查看分享内容
系统 SHALL 支持通过分享 code 查看分享的文件信息。

#### Scenario: 成功查看分享内容
- **WHEN** 用户提供有效的 shareCode（分享未过期）
- **THEN** 系统返回分享的文件信息（文件名、大小、类型），若设置了提取码则需要先验证提取码

#### Scenario: 分享链接已过期
- **WHEN** 用户访问已过期的分享链接（expireTime < 当前时间）
- **THEN** 系统返回 HTTP 410 Gone 和相应错误信息

#### Scenario: 分享链接不存在
- **WHEN** 用户提供不存在的 shareCode
- **THEN** 系统返回 HTTP 404 和相应错误信息

### Requirement: 验证提取码
系统 SHALL 支持通过提取码验证后访问分享文件。

#### Scenario: 提取码正确
- **WHEN** 用户提供 shareCode 和正确的提取码
- **THEN** 系统验证通过，返回文件访问权限（临时 token 或直接返回文件信息）

#### Scenario: 提取码错误
- **WHEN** 用户提供错误的提取码
- **THEN** 系统返回 HTTP 401 和相应错误信息

### Requirement: 分享文件下载
系统 SHALL 支持通过分享链接下载文件（通过提取码验证后）。

#### Scenario: 通过分享下载文件
- **WHEN** 用户通过 shareCode + 提取码验证后请求下载文件
- **THEN** 系统允许下载，记录分享下载审计日志（shareCode、下载者 IP、时间）

### Requirement: 管理分享列表
系统 SHALL 支持用户查看和管理自己创建的分享链接。

#### Scenario: 查看我的分享列表
- **WHEN** 用户请求查看自己的分享列表
- **THEN** 系统返回该用户创建的所有分享记录，包含 shareCode、文件名、创建时间、过期时间、浏览次数

#### Scenario: 取消分享
- **WHEN** 用户请求取消一个分享（提供 shareId）
- **THEN** 系统删除 ShareFile 记录，分享链接立即失效

### Requirement: 分享过期清理
系统 SHALL 通过定时任务清理过期的分享记录。

#### Scenario: 清理过期分享
- **WHEN** 定时任务执行（每天凌晨）
- **THEN** 系统查找所有 expireTime 不为 null 且已过期的分享记录
- **THEN** 删除这些 ShareFile 记录
