## MODIFIED Requirements

### Requirement: 图片预览遮罩层与布局
全屏图片预览器，包含缩略图侧栏、主图区域、顶部信息栏和底部缩放控制。

#### Scenario: 遮罩层
- **WHEN** 打开图片预览
- **THEN** 全屏遮罩，背景 `rgba(0,0,0,0.8)`，带淡入动画（从transparent到rgba）

#### Scenario: 顶部信息栏
- **WHEN** 渲染顶部栏
- **THEN** 高度48px，背景 `rgba(0,0,0,0.5)`，padding-right 48px，包含：折叠按钮(左侧)、文件名(中间flex:1)、序号输入框+旋转+下载+关闭按钮(右侧)

#### Scenario: 关闭按钮必须可见
- **WHEN** 渲染关闭按钮
- **THEN** MUST 包含关闭图标（`<Close />` 或 el-icon-close），字号18px，不得为空

#### Scenario: 缩略图侧栏
- **WHEN** 渲染缩略图侧栏（total > 1 且未折叠）
- **THEN** 宽120px，`top: 48px`，背景 `rgba(0,0,0,0.5)`，padding `8px 16px`，每张80×80

#### Scenario: 缩略图暗色遮罩
- **WHEN** 渲染非当前选中的缩略图
- **THEN** MUST 使用 `::after` 伪元素覆盖黑色半透明层（`opacity: 0.4`），hover时变为 `opacity: 0.2`
- **WHEN** 缩略图为当前选中
- **THEN** 无暗色遮罩，显示原始图片

#### Scenario: 主图区域
- **WHEN** 渲染主图区域
- **THEN** `top: 48px, right: 0, left: 120px`（有侧栏时）或 `left: 0`（无侧栏时），`height: calc(100vh - 48px)`

#### Scenario: 导航箭头位置
- **WHEN** 渲染前后导航箭头
- **THEN** 字号60px，白色，`left: 64px / right: 64px`（考虑侧栏宽度偏移）

#### Scenario: 缩放滑块
- **WHEN** 渲染底部缩放滑块
- **THEN** 宽600px居中，滑块和进度条颜色 MUST 为 `#303133`（非Element Plus默认蓝色）
