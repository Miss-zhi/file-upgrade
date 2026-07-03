# frontend-preview — 技术设计文档

## Context

前端升级第四阶段（最终阶段）：基于已完成的 frontend-base（布局/路由/store 壳）、file-module-frontend（文件管理交互）、file-module 后端（30 端点 + 4 个 Document 端点），为文件管理系统添加 6 种文件预览能力。

**约束条件：**
- Vue 3.4+ Composition API + `<script setup lang="ts">` + TypeScript 5.x
- Element Plus 组件库，禁止 Options API
- 后端无专用文件内容预览端点，需通过现有下载端点获取文件内容
- 所有预览覆盖层需要 1:1 还原旧项目的视觉和交互

**已有基础设施：**
- `utils/file.ts` 文件类型判断函数已就绪
- `types/file.ts` 扩展名映射表已定义
- `api/file.ts` 下载和文件详情 API 已存在
- `views/FileView.vue` 的 `handleOpenFile` 已预留预览入口点

## Goals / Non-Goals

**Goals:**
- 实现 6 种文件预览类型：图片、视频、音频、代码、Markdown、Office/PDF
- 全屏覆盖层模式，与旧项目交互体验一致
- 基于文件扩展名的自动路由调度
- 支持文件列表批量预览（图片/视频/音频的分类视图）
- 键盘快捷键支持
- 用户偏好配置 localStorage 持久化

**Non-Goals:**
- 代码/Markdown 在线编辑保存（后端无对应端点）
- 文件搜索预览高亮
- PDF 独立预览（非 OnlyOffice）
- 压缩包内文件预览
- 文件预览分享页面

## Decisions

### 1. 预览组件架构：Composable + Teleport

**选择：** 每个预览类型一个 composable（如 `useImagePreview()`），返回 `{ visible, open(file, fileList?), close }`，内部通过 `<Teleport to="body">` 渲染全屏覆盖层。

**替代方案：**
- ❌ Vue.extend + $mount 动态挂载（旧项目方案）— Vue 3 已移除 Vue.extend
- ❌ 独立路由页面 — 成本高（每个预览类型一个路由），无法保持文件管理页面的上下文
- ❌ ElDialog/ElDrawer 弹窗 — 空间受限，视觉风格与旧项目全屏覆盖层差距大

**理由：** Composable 模式解耦预览逻辑与视图层，`open/close` 命令式调用方便，Teleport 确保覆盖层渲染到 body 层级避免 z-index 和 overflow 问题。与 Vue 3 Composition API 生态一致。

### 2. 视频播放器：HTML5 `<video>` 原生控件

**选择：** 使用 HTML5 `<video>` 元素 + 原生 controls 属性，替代 video.js。

**替代方案：**
- ❌ video.js ^7.18（旧项目依赖）— Vue 3 生态支持弱，包体积大（~500KB gzipped），功能过剩
- ❌ xgplayer / artplayer — 功能丰富但增加额外依赖

**理由：** 旧项目 video.js 仅使用基础播放功能（播放/暂停/进度/速率/音量），HTML5 `<video>` 原生控件完全覆盖。现代浏览器对 mp4/webm 支持良好。减少 ~500KB 依赖体积。

### 3. 代码预览：CodeMirror 6

**选择：** CodeMirror 6 系列（`@codemirror/view` + `@codemirror/state` + `@codemirror/language` + `@codemirror/commands` 等），只读模式。

**替代方案：**
- ❌ vue-codemirror ^4（旧项目依赖）— 基于 CodeMirror 5，Vue 3 不兼容
- ❌ Monaco Editor — 包体积大（~2MB），功能过剩（只读预览不需要 IntelliSense）
- ❌ highlight.js 纯渲染 — 不支持行号、折叠、主题切换等编辑器特性

**理由：** CodeMirror 6 原生支持 Vue 3 集成，模块化按需加载，包体积可控。旧项目使用了大量 CodeMirror 特性（69 种主题、18 种语言、行号、代码折叠），CodeMirror 6 是最接近的升级路径。

### 4. Markdown 预览：markdown-it + highlight.js

**选择：** 使用 markdown-it 解析 Markdown + highlight.js 代码高亮，桌面双栏（源码+渲染），移动端仅渲染。

**替代方案：**
- ❌ mavon-editor ^2.10（旧项目依赖）— Vue 3 不兼容，且旧项目仅使用预览模式（非编辑器）
- ❌ Vditor — 功能丰富但体积大，旧项目不需要编辑器功能

**理由：** 旧项目 Markdown 预览是只读渲染模式，不需要编辑器工具栏。markdown-it 是最轻量的 Markdown 解析方案，highlight.js 是代码高亮标准库。桌面双栏通过 CSS 媒体查询 `@media (min-width: 768px)` 实现，与旧项目一致。

### 5. 文件内容获取策略

**选择：** 在 `api/file.ts` 新增两个函数：
- `getFileContent(userFileId)` — `client.get(url, { responseType: 'blob' })` 获取二进制内容，用于图片/视频/音频（转 Object URL）
- `getFileText(userFileId)` — `client.get(url, { responseType: 'text' })` 获取文本内容，用于代码/Markdown

通过现有 `GET /filetransfer/download/{userFileId}` 端点获取，忽略 `Content-Disposition: attachment` 头（Axios blob/文本模式不受影响）。

**理由：** 后端无专用预览端点，复用下载端点是前端最小改动方案。`responseType: 'blob'` 使 Axios 返回 Blob 对象而非触发下载，通过 `URL.createObjectURL()` 转为可用的 src。

### 6. 文件组织结构

```
src/
├── components/preview/           # 预览组件
│   ├── ImagePreview.vue
│   ├── VideoPreview.vue
│   ├── AudioPreview.vue
│   ├── CodePreview.vue
│   ├── MarkdownPreview.vue
│   └── PreviewTopBar.vue         # 共享顶部栏组件
├── composables/
│   ├── useImagePreview.ts
│   ├── useVideoPreview.ts
│   ├── useAudioPreview.ts
│   ├── useCodePreview.ts
│   ├── useMarkdownPreview.ts
│   └── usePreviewRouter.ts       # 预览类型路由调度
├── views/
│   ├── FileView.vue              # 修改：handleOpenFile 调用预览路由
│   └── OfficePreview.vue         # 新增：OnlyOffice 独立页面
├── api/
│   └── file.ts                   # 修改：新增 getFileContent/getFileText + 文档 API
├── types/
│   └── file.ts                   # 修改：新增预览相关类型定义
└── router/
    └── index.ts                  # 修改：新增 /preview/office 路由
```

### 7. 预览路由调度逻辑

**`usePreviewRouter` composable 设计：**

```typescript
function openFilePreview(file: FileItem, fileList?: FileItem[]) {
  const ext = getFileExtension(file.fileName).toLowerCase()
  
  if (isImage(ext))       return imagePreview.open(file, fileList)
  if (isVideo(ext))       return videoPreview.open(file, fileList)
  if (isAudio(ext))       return audioPreview.open(file, fileList)
  if (isOffice(ext) || ext === 'pdf') return openOfficePreview(file)
  if (isMarkdown(ext))    return markdownPreview.open(file, fileList)
  if (isCode(ext))        return codePreview.open(file, fileList)
  
  // fallback: 下载
  downloadFile(file.userFileId)
}
```

### 8. 依赖版本选择

| 依赖 | 版本 | 用途 | 选择理由 |
|------|------|------|---------|
| `@codemirror/view` | ^6.x | 代码编辑器视图 | CodeMirror 6 核心，Vue 3 友好 |
| `@codemirror/state` | ^6.x | 编辑器状态管理 | CM6 依赖 |
| `@codemirror/language` | ^6.x | 语言模式支持 | 语法高亮 |
| `@codemirror/commands` | ^6.x | 编辑器命令 | 快捷键支持 |
| `@codemirror/autocomplete` | ^6.x | 自动补全 | 只读模式下可选项 |
| `@codemirror/fold` | ^6.x | 代码折叠 | 旧项目有此功能 |
| `@codemirror/theme-one-dark` | ^6.x | 默认暗色主题 | 默认主题 |
| `markdown-it` | ^14.x | Markdown 解析 | 最主流的 Markdown 解析器 |
| `highlight.js` | ^11.x | 代码高亮 | Markdown 中代码块着色 |
| `js-base64` | ^3.x | Base64 编解码 | 歌词 LRC 解码 |

## Risks / Trade-offs

- **[文件内容获取]** 通过下载端点获取预览内容 → 大文件（>100MB 视频）会导致内存占用高和加载慢。短期可行，长期需后端支持 Range 请求的流式预览端点。
- **[HTML5 视频兼容性]** 部分浏览器对 MKV/FLV 容器支持有限 → Mitigation: 检测 `canPlayType`，不支持时降级为下载。
- **[CodeMirror 6 主题迁移]** 旧项目 69 种 CodeMirror 5 主题不能直接用于 CM6 → 使用 CM6 内置主题 + 社区主题包覆盖主要场景。部分冷门主题可能缺失。
- **[OnlyOffice 依赖]** 需要外部 OnlyOffice Document Server 运行 → 已有后端 Document 端点，开发环境需配置 `ONLYOFFICE_API_URL`。
- **[移动端适配]** 旧项目音频预览在移动端存在布局问题 → 仅做基本的响应式适配（768px 断点），不做全面的移动端优化。
