# app-theme — 主题与样式系统

## Description

将旧项目的 Stylus 样式体系完整迁移为 SCSS，建立全局主题变量、CSS reset、Element Plus 覆盖、通用 mixin 和响应式断点。

## Requirements

### REQ-1: SCSS 变量文件

`assets/styles/variables.scss` 定义全局 SCSS 变量，从旧项目 `varibles.styl` 1:1 迁移。

**Scenarios:**

- **主题色**：`$primary: #409EFF`、`$success: #67C23A`、`$warning: #E6A23C`、`$danger: #F56C6C`、`$info: #909399`
- **Hover 色**：`$primary-hover: #ecf5ff`、`$success-hover: #f0f9eb`、`$warning-hover: #fdf6ec`、`$danger-hover: #fdf6ec`、`$info-hover: #e9e9eb`
- **文字色**：`$primary-text: #303133`、`$regular-text: #606266`、`$secondary-text: #909399`、`$placeholder: #C0C4CC`
- **边框色**：`$border-base: #DCDFE6`、`$border-light: #E4E7ED`、`$border-lighter: #EBEEF5`、`$border-extralight: #F2F6FC`
- **布局尺寸**：`$header-height: 61px`、`$sidebar-width: 210px`、`$sidebar-storage-bar: 66px`
- **阴影**：`$tab-box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1)`、`$tab-box-shadow-min: 0 2px 4px 0 rgba(0,0,0,0.1)`
- **背景**：`$tab-back-color: #F5F7FA`

### REQ-2: CSS Reset

`assets/styles/reset.scss` 从旧项目 `base.styl` 迁移。

**Scenarios:**

- 全局 `margin: 0`、`padding: 0`、`box-sizing: border-box`
- 字体族包含 PingFang SC、Microsoft YaHei、sans-serif
- 链接无下划线
- `#app` 容器 `height: 100%`

### REQ-3: Element Plus 样式覆盖

`assets/styles/element-override.scss` 从旧项目 `elementCover.styl` 迁移，适配 Element Plus。

**Scenarios:**

- `el-dialog` 的 `margin-top: 9vh`
- `el-avatar img` 的 `width: 100%`
- `el-input textarea` 的 `font-family: inherit`
- Element Plus CSS 变量覆盖：`--el-color-primary: #409eff` 等

### REQ-4: 通用 Mixin

`assets/styles/mixins.scss` 从旧项目 `mixins.styl` 迁移。

**Scenarios:**

- `@mixin setScrollbar($width, $trackColor, $thumbColor)` — 自定义 WebKit 滚动条样式
- `@mixin setEllipsis($lines)` — 多行文本溢出省略（`-webkit-line-clamp` + `-webkit-box-orient`）

### REQ-5: 响应式样式

`assets/styles/responsive.scss` 从旧项目 `mediaScreenXs.styl` 迁移关键规则。

**Scenarios:**

- 断点定义：`$breakpoint-xs: 768px`
- 移动端 Header 高度调整
- 移动端 Sidebar 切换为 absolute 定位
- 移动端 Dialog 宽度 80%
- 移动端文件列表/网格模式适配
- 移动端上传组件适配

### REQ-6: main.ts 样式导入顺序

`main.ts` 按正确顺序导入全局样式。

**Scenarios:**

- 导入顺序：`reset.scss` → `variables.scss` → `element-override.scss` → `responsive.scss`
- Element Plus CSS：`element-plus/dist/index.css`（已有）
- 移除旧的 `main.css` 和 `base.css`

## Dependencies

- `sass` npm 包（devDependency）
- Vite SCSS 预处理器配置
