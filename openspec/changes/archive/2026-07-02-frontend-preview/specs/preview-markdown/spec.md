# preview-markdown — Markdown 预览规格

## ADDED Requirements

### Requirement: 全屏覆盖层 Markdown 预览

系统 SHALL 提供全屏覆盖层 Markdown 预览组件，使用 markdown-it 解析渲染，桌面端双栏（源码+渲染），移动端仅渲染。

#### Scenario: 打开 Markdown 预览
- **WHEN** 用户触发 Markdown 预览
- **THEN** 系统显示全屏覆盖层，z-index: 2，背景动画到 rgba(0,0,0,0.8)

#### Scenario: 关闭 Markdown 预览
- **WHEN** 用户点击关闭按钮或按 Escape 键
- **THEN** Markdown 预览关闭

### Requirement: 顶部信息栏

系统 SHALL 在覆盖层顶部显示 48px 高的半透明信息栏，包含文件名、"在线预览"标签、下载链接和关闭按钮。

#### Scenario: 顶部栏显示
- **WHEN** Markdown 预览打开
- **THEN** 顶部栏显示：文件名、"在线预览"标签、下载链接、关闭按钮

### Requirement: Markdown 渲染

系统 SHALL 使用 markdown-it 解析 Markdown 文本并渲染为 HTML，代码块使用 highlight.js 高亮。

#### Scenario: Markdown 内容渲染
- **WHEN** Markdown 文件内容加载完成（通过 getFileText）
- **THEN** 渲染容器（90vw × calc(100vh-80px)）显示解析后的 HTML

#### Scenario: 代码块语法高亮
- **WHEN** Markdown 包含代码块
- **THEN** 代码块使用 highlight.js 进行语法高亮

### Requirement: 桌面双栏布局

系统 SHALL 在屏幕宽度 >768px 时显示源码 + 渲染双栏布局。

#### Scenario: 桌面端双栏显示
- **WHEN** 视口宽度 >768px 且 Markdown 预览打开
- **THEN** 左侧显示原始 Markdown 源码，右侧显示渲染后的 HTML

#### Scenario: 移动端仅渲染
- **WHEN** 视口宽度 ≤768px
- **THEN** 仅显示渲染后的 HTML，不显示源码
