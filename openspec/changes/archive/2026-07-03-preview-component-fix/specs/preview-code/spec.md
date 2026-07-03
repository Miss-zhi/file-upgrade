## MODIFIED Requirements

### Requirement: 代码预览工具栏与编辑器样式
全屏代码查看器，包含顶部信息栏、设置工具栏和CodeMirror编辑器。

#### Scenario: 遮罩层
- **WHEN** 打开代码预览
- **THEN** 全屏遮罩，背景 `rgba(0,0,0,0.8)`，带淡入动画

#### Scenario: 顶部信息栏
- **WHEN** 渲染顶部栏
- **THEN** 高度48px，背景 `rgba(0,0,0,0.5)`，padding `0 48px`，包含文件名+在线预览标签+下载+关闭

#### Scenario: 工具栏外观
- **WHEN** 渲染编辑器设置工具栏
- **THEN** MUST 为白色背景 `background: #fff`，圆角 `8px 8px 0 0`，底部边框 `1px solid #DCDFE6`，padding `8px 16px`
- **THEN** 包含：自动换行checkbox + 字号select(96px) + 语言select(120px) + 主题select(190px)

#### Scenario: 编辑器容器
- **WHEN** 渲染编辑器容器
- **THEN** `margin: 56px auto 0`，`width: 90vw`，`height: calc(100vh - 80px)`

#### Scenario: 编辑器高度
- **WHEN** 渲染CodeMirror编辑器
- **THEN** 高度 MUST 为 `calc(100vh - 129px)`（减去工具栏和顶部栏），圆角 `0 0 8px 8px`

#### Scenario: 编辑器滚动条
- **WHEN** 编辑器内容溢出
- **THEN** 自定义滚动条：宽12px，透明轨道，`#C0C4CC` 滑块，border-radius 2em

#### Scenario: 编辑器字体
- **WHEN** 渲染编辑器代码
- **THEN** 字体 MUST 为等宽字体 `SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace`

#### Scenario: 主题选择
- **WHEN** 渲染主题下拉框
- **THEN** MUST 提供至少10个以上主题选项（通过安装CodeMirror 6主题包实现），主题切换后持久化到localStorage
