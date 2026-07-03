# preview-markdown — Markdown 预览

## Purpose

提供全屏覆盖层 Markdown 预览组件，使用 markdown-it 解析渲染，代码块以 highlight.js 高亮。桌面端双栏（源码+渲染），移动端仅渲染。

## Requirements

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

#### Scenario: 渲染视图浅色主题
- **WHEN** 渲染Markdown渲染区域
- **THEN** 背景 MUST 为浅色（白色或 `#fff`），文字颜色为深色（`#303133`），代码块、表格、引用等元素使用浅色主题样式
- **THEN** 代码块背景 `#f6f8fa`，文字 `#24292e`
- **THEN** 表格边框 `#dfe2e5`，表头背景 `#f6f8fa`
- **THEN** 链接颜色 `#409EFF`
- **THEN** 引用块左边框 `#dfe2e5`，文字 `#6a737d`

#### Scenario: 源码视图
- **WHEN** 渲染源码区域（桌面端）
- **THEN** 背景为深色 `rgba(0,0,0,0.3)`，文字 `rgba(255,255,255,0.8)`，等宽字体

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

### Requirement: Markdown预览字号调节

Markdown预览 SHALL 提供字号调节功能，用户可切换渲染区域的字体大小。

#### Scenario: 字号调节入口
- **WHEN** 渲染Markdown预览顶部栏或工具区域
- **THEN** MUST 提供字号切换控件（如字号下拉select或+/-按钮），提供至少3档字号选项（如12px/14px/16px/18px/20px）

#### Scenario: 渲染区域字号响应
- **WHEN** 用户选择不同字号
- **THEN** `.render-pane` 内的 `.markdown-body` 字体大小 MUST 动态变化，源码区 `.source-code` 字号同步变化

#### Scenario: 字号持久化
- **WHEN** 用户选择字号后关闭预览再重新打开
- **THEN** 字号选择 MUST 从localStorage恢复（key: `qiwen_file_markdown_fontsize`）
