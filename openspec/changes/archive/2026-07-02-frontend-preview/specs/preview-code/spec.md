# preview-code — 代码预览规格

## ADDED Requirements

### Requirement: 全屏覆盖层代码预览

系统 SHALL 提供全屏覆盖层代码预览组件，使用 CodeMirror 6 以只读模式显示代码。

#### Scenario: 打开代码预览
- **WHEN** 用户触发代码预览
- **THEN** 系统显示全屏覆盖层，z-index: 2，背景动画到 rgba(0,0,0,0.8)

#### Scenario: 关闭代码预览
- **WHEN** 用户点击关闭按钮或按 Escape 键
- **THEN** 代码预览关闭

### Requirement: 顶部信息栏

系统 SHALL 在覆盖层顶部显示 48px 高的半透明信息栏，包含文件名、"在线预览"标签、下载链接和关闭按钮。

#### Scenario: 顶部栏显示
- **WHEN** 代码预览打开
- **THEN** 顶部栏显示：文件名、"在线预览"标签、下载链接、关闭按钮

### Requirement: CodeMirror 编辑器

系统 SHALL 使用 CodeMirror 6 渲染代码，配置为只读模式，支持行号、代码折叠、自动括号闭合。

#### Scenario: 代码内容渲染
- **WHEN** 代码预览打开且文件内容加载完成
- **THEN** CodeMirror 编辑器（90vw × calc(100vh-80px)）显示代码，tabSize=4, lineNumbers=true, readOnly=true

#### Scenario: 自动检测语言模式
- **WHEN** 文件扩展名在 fileSuffixCodeModeMap 中
- **THEN** CodeMirror 自动应用对应语言模式的语法高亮

#### Scenario: yml 扩展名映射到 yaml
- **WHEN** 文件扩展名为 yml
- **THEN** 语言模式映射为 yaml

### Requirement: 工具栏

系统 SHALL 在编辑器上方显示工具栏，包含自动换行切换、字号选择、语言模式选择、主题选择。

#### Scenario: 自动换行切换
- **WHEN** 用户点击自动换行切换
- **THEN** 编辑器在换行/不换行之间切换

#### Scenario: 主题选择
- **WHEN** 用户在下拉列表中选择主题
- **THEN** 编辑器主题切换，选择持久化到 localStorage key `qiwen_file_codemirror_theme`

#### Scenario: 语言模式选择
- **WHEN** 用户在下拉列表中选择语言模式
- **THEN** 编辑器语法高亮切换到对应语言

#### Scenario: 字号选择
- **WHEN** 用户更改字号设置
- **THEN** 编辑器字号相应调整
