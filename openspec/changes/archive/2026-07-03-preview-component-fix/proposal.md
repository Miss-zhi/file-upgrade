## Why

5个文件预览组件（图片/视频/音频/代码/Markdown）的视觉效果与旧项目严重不符，共发现22处不达标问题。最严重的包括：音频预览主题色从橙色全错为蓝色、控制按钮用emoji替代iconfont图标、缺少播放动画和列表表头；代码预览工具栏从白色圆角变为暗色半透明、主题从69个缩减为2个；图片预览关闭按钮为空、缩略图缺少暗色遮罩。这些差异使预览体验远低于旧项目水准，需要逐组件修复。

## What Changes

- **ImagePreview**：修复空的关闭按钮、补回缩略图暗色遮罩(::after伪元素)、修正导航箭头位置(64px)、修正缩放滑块颜色(#303133)
- **VideoPreview**：顶部栏背景从rgba改为纯黑#000、补回"播放列表"标题头、修正列表项active样式
- **AudioPreview**：主题色从蓝色#409EFF全部改为橙色#E6A23C、emoji控制按钮改为iconfont图标(字号40px)、补回音频列表表头、补回wave.gif播放动画、补回歌词渐变遮罩、修正控制栏布局为左右分栏、修正进度条/音量条样式(2px高/橙色)
- **CodePreview**：工具栏从暗色半透明改为白色背景+圆角+边框、扩展主题数量(安装更多CM6主题包)、修正编辑器高度(calc(100vh-80px))、修正编辑器滚动条样式
- **MarkdownPreview**：从markdown-it暗色渲染改回mavon-editor或等效的浅色主题渲染方案、补回编辑器工具栏

## Capabilities

### New Capabilities
（无新增capability）

### Modified Capabilities
- `preview-image`: 修复关闭按钮为空、缩略图遮罩缺失、箭头位置偏差、缩放滑块颜色
- `preview-video`: 修复顶部栏背景色、播放列表标题缺失、列表项active样式
- `preview-audio`: 修复主题色错误(蓝→橙)、emoji替代iconfont、列表表头缺失、播放动画缺失、歌词遮罩缺失、控制栏布局错误、进度条样式
- `preview-code`: 修复工具栏外观(暗色→白色)、主题数量不足(2→69+)、编辑器高度偏差、滚动条样式
- `preview-markdown`: 修复渲染方案差异(暗色markdown-it→浅色mavon-editor风格)、工具栏缺失

## Impact

- **组件文件**：5个预览.vue文件全部需要修改template和style
- **composable文件**：useAudioPreview.ts可能需要调整（iconfont图标状态管理）
- **依赖**：CodePreview可能需要安装额外的CodeMirror 6主题包；MarkdownPreview可能需要评估是否恢复mavon-editor或调整markdown-it渲染风格
- **静态资源**：wave.gif播放动画需从旧项目复制
- **iconfont**：依赖preview-component-fix之外的iconfont迁移（frontend-rewrite change的missing-ui-features）
