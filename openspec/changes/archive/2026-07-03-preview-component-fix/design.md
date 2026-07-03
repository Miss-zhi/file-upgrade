## Context

5个文件预览组件（ImagePreview、VideoPreview、AudioPreview、CodePreview、MarkdownPreview）在前次实现时偏离了旧项目的UI设计。经逐项对比发现22处不达标问题，涉及：颜色主题错误、图标方案降级、布局结构差异、样式细节缺失、依赖库替换导致的视觉差异。

本次change仅修复预览组件的视觉效果，不改变composable业务逻辑和API调用方式。

## Goals / Non-Goals

**Goals:**
- 5个预览组件的视觉效果1:1还原旧项目
- 音频预览主题色从蓝色修正为橙色(#E6A23C)
- 代码预览工具栏恢复白色背景+扩展主题数量
- Markdown预览渲染区域恢复浅色主题风格
- 图片预览关闭按钮可见、缩略图遮罩效果恢复
- 视频预览顶部栏恢复纯黑背景+播放列表标题

**Non-Goals:**
- 不改变预览组件的业务逻辑（composable层不动）
- 不改变文件加载方式（仍用getFileContent/getFileText + ObjectURL）
- 不改变Teleport+inject架构模式
- 不恢复video.js（HTML5 video已满足需求，仅修样式）
- 不恢复mavon-editor依赖（用markdown-it+自定义浅色CSS实现等效视觉）

## Decisions

### D1: 音频预览主题色统一为橙色

**决定**: 音频预览全局强调色从 `#409EFF`(蓝) 改为 `#E6A23C`(橙/Warning)。

**理由**: 旧项目音频播放器使用 `$Warning` 橙色作为主题色（active项、hover、进度条、歌词高亮、音量条、循环图标），这是旧项目的视觉设计决策，必须1:1还原。

**影响范围**: AudioPreview.vue 的 `<style>` 块中所有涉及强调色的地方，约30+处CSS值修改。

### D2: 音频控制按钮改用iconfont

**决定**: 播放/暂停/上下首/循环/音量按钮从emoji字符改为iconfont图标类名。

**理由**: 旧项目使用iconfont图标（icon-icon-7播放、icon-icon-3暂停、icon-shangyishou上一首等），emoji在不同操作系统渲染不一致且视觉粗糙。iconfont已在frontend-rewrite change中迁移。

**前提**: 依赖frontend-rewrite change的iconfont迁移任务完成。

### D3: 代码预览工具栏恢复白色背景

**决定**: 工具栏从暗色半透明改为白色背景+圆角+底部边框。

**理由**: 旧项目代码预览工具栏是白色背景(`#fff`)，圆角`8px 8px 0 0`，底部边框`1px solid #DCDFE6`。这与编辑器的暗色背景形成对比，视觉上更清晰。

### D4: CodeMirror 6主题扩展

**决定**: 安装 `@highlightjs/codemirror-theme` 或 `cm6-themes` 包，提供10+主题选项。

**理由**: 旧项目通过vue-codemirror支持69个主题。新项目只有2个(default + OneDark)远远不够。不需要完全达到69个，但至少提供主流主题（Dracula、Solarized、GitHub、Monokai、Nord、Material等10+个）。

**替代方案**: 直接用CodeMirror 6的 `EditorView.theme()` API 手写几个主题——工作量大且效果不如成熟主题包。

### D5: Markdown预览浅色渲染方案

**决定**: 保持markdown-it渲染引擎不变，为 `.markdown-body` 编写一套GitHub风格的浅色主题CSS覆盖。

**理由**: 旧项目用mavon-editor自带GitHub浅色主题。新项目用markdown-it但渲染在暗色背景上导致视觉完全不同。不需要恢复mavon-editor依赖（太重），只需为渲染区域设置白色背景+深色文字+浅色代码块+浅色表格边框即可达到等效视觉。

**实现**: 在MarkdownPreview.vue的 `<style>` 中为 `.render-pane` 设置白色背景，为 `.markdown-body` 编写GitHub-flavored浅色样式。

### D6: 图片预览缩略图遮罩恢复

**决定**: 为非当前缩略图添加 `::after` 伪元素遮罩。

**理由**: 旧项目通过 `::after` 伪元素在非active缩略图上覆盖黑色半透明层(opacity 0.4)，hover变0.2，突出当前选中图片。新项目缺少这个效果导致所有缩略图亮度一致，选中态不明显。

## Risks / Trade-offs

### [R1] iconfont依赖前置
→ 音频预览的iconfont图标依赖frontend-rewrite change的iconfont迁移。如果iconfont未迁移，音频按钮将无图标可用。
→ 缓解：两个change可并行开发，但音频预览的验证需在iconfont迁移后。

### [R2] CodeMirror 6主题包兼容性
→ 第三方CM6主题包可能与当前CM6版本不兼容。
→ 缓解：先测试 `@highlightjs/codemirror-theme` 的兼容性，不行则用 `cm6-theme-dracula` 等独立主题包。

### [R3] Markdown浅色主题与暗色遮罩对比
→ 渲染区域白色背景与外层暗色遮罩(rgba(0,0,0,0.8))的对比可能过于强烈。
→ 缓解：渲染区域加圆角 `8px` 和适当padding，使过渡更自然。

### [R4] wave.gif动画资源
→ 音频播放动画依赖wave.gif，需从旧项目复制。
→ 缓解：已在frontend-rewrite tasks中列入静态资源迁移。
