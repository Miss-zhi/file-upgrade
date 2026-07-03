## MODIFIED Requirements

### Requirement: Markdown预览渲染风格与工具栏
全屏Markdown预览器，使用浅色主题渲染风格，包含源码和渲染视图。

#### Scenario: 遮罩层
- **WHEN** 打开Markdown预览
- **THEN** 全屏遮罩，背景 `rgba(0,0,0,0.8)`，带淡入动画

#### Scenario: 顶部信息栏
- **WHEN** 渲染顶部栏
- **THEN** 高度48px，背景 `rgba(0,0,0,0.5)`，padding `0 48px`

#### Scenario: 容器尺寸
- **WHEN** 渲染Markdown容器
- **THEN** `margin: 56px auto 0`，`width: 90vw`，`height: calc(100vh - 80px)`

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

#### Scenario: 桌面端双栏布局
- **WHEN** screenWidth > 768px
- **THEN** 左侧源码 + 右侧渲染预览，各flex:1，中间分隔线

#### Scenario: 移动端单栏
- **WHEN** screenWidth ≤ 768px
- **THEN** 隐藏源码栏，只显示渲染预览

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
