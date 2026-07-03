# frontend-preview — 文件预览前端交互提案

## 背景

前端升级第四阶段（最终阶段）：基于已完成的 frontend-base（布局/路由/store 壳）、file-module-frontend（文件管理交互）、file-module 后端（30 端点 + 4 个 Document 端点），为文件管理系统添加 6 种文件预览能力。此阶段产出后，用户可以在线预览图片、视频、音频、代码、Markdown、Office/PDF 文件，完成全部核心操作闭环。

### 当前状态

file-module-frontend 已交付：

| 已有文件 | 状态 |
|---------|------|
| `views/FileView.vue` | 文件管理主页面，`handleOpenFile` 对非文件夹文件调用 `downloadFile()`（占位，注释"预览功能留给 frontend-preview"） |
| `components/file/ContextMenu.vue` | 右键菜单已有"查看"动作（→ handleOpenFile）和"在线编辑"动作（→ 未实现，无 case 'edit'） |
| `utils/file.ts` | `isOffice()` / `isMarkdown()` / `isCode()` / `isArchive()` / `isFolder()` 已就绪 |
| `types/file.ts` | `officeFileType`（6 种）/ `markdownFileType`（2 种）/ `fileSuffixCodeModeMap`（30+ 种代码语言映射）已定义 |
| `api/file.ts` | `downloadFile(userFileId)` 已有，`getFileDetail(userFileId)` 已有 |

### 旧项目文件预览参考

旧项目（Vue 2 + Element UI）文件预览架构：

- **统一入口**：`handleFileNameClick`（`libs/globalFunction/file.js`）根据文件扩展名路由到不同预览类型
- **全部全屏覆盖层**：图片/视频/音频/代码/Markdown 均为 `position: fixed; 100vw/100vh` 覆盖层，非弹窗或独立页面
- **Vue.extend + Promise 服务化调用**：每种预览有 `index.js` 创建 Vue 实例挂载到 `document.body`，通过 `$openBox.xxxPreview()` 调用
- **Office/PDF**：通过 OnlyOffice Document Server 在新浏览器标签页中打开

**6 种预览类型详情：**

| 类型 | z-index | 顶部栏 | 内容区域 | 外部依赖 | 特殊功能 |
|------|---------|--------|---------|---------|---------|
| 图片 | 2 | 48px 半透明黑 | 主图 + 80x80 缩略图侧栏 + 缩放滑块 + 旋转 | 无 | 键盘导航（←→Esc），1-200% 缩放，侧栏折叠（localStorage 持久化） |
| 视频 | 3 | 48px 纯黑 | video.js 播放器 + 280px 播放列表侧栏 | video.js ^7.18 | 播放速率 0.5/1/1.5/2x，移动端自动折叠播放列表 |
| 音频 | 3 | 无（右上角操作框） | 播放列表 + 160x160 专辑封面 + LRC 歌词滚动 + 控制栏 120px | js-base64 | 模糊专辑背景（blur 65px），歌词自动高亮，三种循环模式，文件详情 API 获取音乐元数据 |
| 代码 | 2 | 48px 半透明黑 | CodeMirror 编辑器（90vw 宽）+ 工具栏（主题/字体/语言/自动换行） | vue-codemirror ^4 | 69 种主题（localStorage 持久化），18 种语言模式，Ctrl+S 保存 |
| Markdown | 2 | 48px 半透明黑 | mavon-editor 双栏编辑器（90vw 宽） | mavon-editor ^2.10 | 桌面双栏/移动单栏（768px 断点），工具栏仅编辑模式显示 |
| Office/PDF | — | — | 新标签页 OnlyOffice Document Editor | — | 通过 OnlyOffice API JS 加载编辑器 |

**旧项目预览 URL 模式：**
- 文件内容预览：`GET /filetransfer/preview?userFileId=...&isMin=false`
- 缩略图：`GET /filetransfer/preview?userFileId=...&isMin=true`
- 下载：`GET /filetransfer/downloadfile?userFileId=...`

### 后端预览 API 现状

| 端点 | Controller | 状态 | 用途 |
|------|-----------|------|------|
| `GET /filetransfer/download/{userFileId}` | FileTransferController | ✅ 已有 | 单文件下载，支持 Range/206，但 `Content-Disposition: attachment`（强制下载） |
| `POST /document/preview` | DocumentController | ✅ 已有 | 获取 OnlyOffice 预览配置（DocEditor config），返回 PreviewConfigVO |
| `POST /document/edit` | DocumentController | ✅ 已有 | 获取 OnlyOffice 编辑配置，支持 COW（写时复制） |
| `GET /document/{userFileId}/history` | DocumentController | ✅ 已有 | 文档版本历史 |
| `POST /document/{userFileId}/history/{version}/restore` | DocumentController | ✅ 已有 | 回滚到指定版本 |
| `POST /document/callback` | DocumentCallbackController | ✅ 已有 | OnlyOffice 回调（编辑/保存/关闭事件） |

**⚠️ 关键缺失：** 后端无专用文件内容预览端点（`GET /filetransfer/preview`）。当前下载端点始终返回 `Content-Disposition: attachment`，不支持浏览器内联展示。图片/视频/音频/代码/Markdown 预览需要获取文件原始内容。

**前端解决方案：** 使用 Axios 的 `responseType: 'blob'` 或 `responseType: 'text'` 通过现有下载端点获取文件内容，在浏览器端创建 Object URL 或直接展示文本。此方案无需后端改动。

## 升级目标

1:1 还原旧项目的文件预览交互和视觉样式，技术栈从 Vue 2 + video.js 7 + vue-codemirror 4 + mavon-editor 2 迁移到 Vue 3 + 兼容 Vue 3 的替代库。

## Capabilities

### 1. preview-router — 预览路由调度

修改 `FileView.vue` 的 `handleOpenFile` 函数，根据文件扩展名路由到对应预览组件。

**路由逻辑（与旧项目 `handleFileNameClick` 一致）：**
- 图片（png/jpg/jpeg/gif/svg/webp/bmp）→ ImagePreview
- 视频（mp4/avi/mkv/mov/webm/flv）→ VideoPreview
- 音频（mp3/flac/wav/aac/ogg）→ AudioPreview
- Office（doc/docx/xls/xlsx/ppt/pptx）→ OnlyOffice 新标签页
- PDF → OnlyOffice 新标签页（或浏览器内置 PDF 阅读器）
- Markdown（md/markdown）→ MarkdownPreview
- 代码（fileSuffixCodeModeMap 中的 30+ 扩展名）→ CodePreview
- 其他 → 保持 downloadFile() 行为

**预览组件统一模式：**
- 使用 composable + Teleport 全屏覆盖层模式（替代旧项目的 Vue.extend + document.body 挂载）
- 每个预览类型一个 composable（如 `useImagePreview()`），返回 `{ visible, open(file, fileList?), close }`
- 所有覆盖层共享顶部栏样式：48px 高，`background: rgba(0,0,0,0.5)`

**入口整合：**
- 双击文件行/网格项 → `handleOpenFile` → 路由到预览
- 右键菜单"查看" → 同上
- 右键菜单"在线编辑" → Office 文件走 OnlyOffice 编辑模式，代码/Markdown 走可编辑预览

**文件内容获取：**
- 新增 `api/file.ts` 函数 `getFileContent(userFileId)` — 使用 `client.get` + `responseType: 'blob'` 获取文件二进制内容
- 新增 `api/file.ts` 函数 `getFileText(userFileId)` — 使用 `client.get` + `responseType: 'text'` 获取文本内容
- 图片/视频/音频：blob → `URL.createObjectURL()` → 设为 src
- 代码/Markdown：text → 直接设为编辑器内容

### 2. preview-image — 图片预览

1:1 还原旧项目 `imgPreview/BoxMask.vue`。

**ImagePreview 组件（`components/preview/ImagePreview.vue`）：**
- 全屏覆盖层：`position: fixed; top/right/bottom/left: 0; z-index: 2`，背景从透明动画到 `rgba(0,0,0,0.8)`
- 顶部栏 `.tip-wrapper`：48px，`rgba(0,0,0,0.5)`，显示折叠按钮/文件名/序号输入框+总数/旋转按钮/下载链接/关闭按钮
- 左侧缩略图栏 `.min-img-list`：`top: 48px; left: 0; height: calc(100vh-48px)`，每项 80x80px，可折叠（状态持久化 localStorage `qiwen_file_img_preview_show_min`）
- 主图区域 `.img-wrapper`：`top: 48px; right: 0; left: 120px; height: calc(100vh-48px)`（侧栏折叠时 left: 0）
- 底部缩放条 `.zoom-bar`：`bottom: 20px; width: 600px`，1%-200% 范围
- 左右箭头导航：`font-size: 60px; top: 50%; z-index: 3`
- 缩放：鼠标滚轮/滑块/自适应计算，使用 CSS `zoom` 属性
- 旋转：CSS `transform: rotate(Ndeg)`
- 键盘：Escape（关闭）、←→（切换）
- 播放列表支持：分类视图（fileType=1）时传入同目录所有图片列表

### 3. preview-video — 视频预览

1:1 还原旧项目 `videoPreview/BoxMask.vue` + `VideoPlayer.vue`。

**VideoPreview 组件（`components/preview/VideoPreview.vue`）：**
- 全屏覆盖层：`position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 3; background: rgba(0,0,0,0.75)`
- 顶部栏 `.top`：48px，纯黑背景，显示文件名+大小/下载链接/折叠播放列表按钮/关闭按钮
- 下部 `.bottom`：`height: calc(100vh-60px); display: flex`
- 视频播放器区域：`flex: 1`，使用 HTML5 `<video>` 原生控件（替代 video.js 以减少依赖）
- 右侧播放列表 `.video-list-wrapper`：`width: 280px; background: #000`，可折叠
- 移动端（≤768px）自动折叠播放列表
- 播放列表支持：分类视图（fileType=3）时传入同目录所有视频列表

**依赖选择：** 使用 HTML5 `<video>` 原生控件替代 video.js。现代浏览器对 mp4/webm 支持良好，原生控件足以覆盖旧项目 video.js 的功能（播放速率、进度条、音量等）。如后续需要更丰富的播放器功能，可引入 xgplayer 或 artplayer。

### 4. preview-audio — 音频预览

1:1 还原旧项目 `audioPreview/BoxMask.vue`。

**AudioPreview 组件（`components/preview/AudioPreview.vue`）：**
- 全屏覆盖层：`position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 3; background: $PrimaryText`
- 模糊背景：`position: fixed; filter: blur(65px); opacity: 0.6; z-index: -1`，使用专辑封面图
- 右上角操作框：tooltip + 关闭按钮
- 主体 `.audio-list-wrapper`：`margin: 0 auto; width: 85%; height: calc(100vh-120px); padding-top: 32px; display: flex`
  - 左：`.audio-list` 可滚动曲目列表（名称/播放暂停/下载/分享/大小/路径）
  - 右：`.img-and-lyrics`（340px）— 160x160 专辑封面 + 曲名/歌手/专辑 + 歌词自动滚动高亮
- 底部控制栏 `.control-wrapper`：`margin: 0 auto; width: 85%; height: 120px; padding: 24px 0 32px 0`
  - 上一首/播放暂停/下一首按钮、进度滑块、时间显示、循环模式（列表循环/单曲循环/随机）、下载、分享、音量
- 音乐元数据：调用 `getFileDetail(userFileId)` 获取 `music.trackLength` / `music.albumImage`（base64）/ `music.lyrics`（base64 编码 LRC）/ `music.artist` / `music.album`
- 歌词解析：Base64 解码 → LRC 格式解析 `[mm:ss.xx]text` → 自动滚动高亮当前行
- 键盘：Escape（关闭）、←→（上一首/下一首）、↑↓（音量）、Space（播放/暂停）
- 三种循环模式：列表循环(1) / 单曲循环(2) / 随机(3)
- 播放列表支持：分类视图（fileType=4）时传入同目录所有音频列表

**新增依赖：** `js-base64`（用于歌词 Base64 解码，与旧项目一致）

### 5. preview-code — 代码预览

1:1 还原旧项目 `codePreview/BoxMask.vue`（只读模式）。

**CodePreview 组件（`components/preview/CodePreview.vue`）：**
- 全屏覆盖层：`position: fixed; top/right/bottom/left: 0; z-index: 2`，背景动画到 `rgba(0,0,0,0.8)`
- 顶部栏 `.tip-wrapper`：48px，`rgba(0,0,0,0.5)`，显示文件名/在线预览标签/下载链接/关闭按钮
- 编辑器容器 `.code-editor-wrapper`：`margin: 56px auto 0; width: 90vw; height: calc(100vh-80px)`
- 工具栏 `.operate-wrapper`：自动换行 checkbox / 字号选择 / 语言模式选择 / 主题选择
- CodeMirror 编辑器：`height: calc(100vh-129px)`
- 配置：`tabSize: 4, lineNumbers: true, autoCloseBrackets: true, foldGutter: true, lineWrapping: true, readOnly: true`
- 语言模式：从 `fileSuffixCodeModeMap` 自动检测（yaml→yml 映射）
- 主题：69 种可选（localStorage 持久化 key `qiwen_file_codemirror_theme`）

**新增依赖：** `@codemirror/view` + `@codemirror/state` + `@codemirror/language` + `@codemirror/commands` + `@codemirror/autocomplete` + `@codemirror/fold` + `@codemirror/theme-one-dark`（CodeMirror 6 系列，原生支持 Vue 3 集成）

### 6. preview-markdown — Markdown 预览

1:1 还原旧项目 `markdownPreview/BoxMask.vue`（只读模式）。

**MarkdownPreview 组件（`components/preview/MarkdownPreview.vue`）：**
- 全屏覆盖层：`position: fixed; top/right/bottom/left: 0; z-index: 2`，背景动画到 `rgba(0,0,0,0.8)`
- 顶部栏 `.tip-wrapper`：48px，`rgba(0,0,0,0.5)`，显示文件名/在线预览标签/下载链接/关闭按钮
- Markdown 渲染容器：`margin: 56px auto 0; width: 90vw; height: calc(100vh-80px)`
- 只读渲染模式（非编辑器），使用 `markdown-it` 解析 + `highlight.js` 代码高亮
- 桌面（>768px）显示源码 + 渲染双栏，移动端仅渲染

**新增依赖：** `markdown-it`（Markdown 解析）+ `highlight.js`（代码高亮）

### 7. preview-office — OnlyOffice 集成

通过 OnlyOffice Document Server 在新标签页中预览/编辑 Office 和 PDF 文件。

**OnlyOffice 预览流程：**
1. 用户点击 Office/PDF 文件 → `handleOpenFile` 识别为 office 类型
2. 调用 `POST /api/v1/document/preview` 获取 PreviewConfigVO（包含 OnlyOffice DocEditor 配置）
3. 打开新标签页 `/preview/office?userFileId=xxx`，该页面加载 OnlyOffice API JS 并初始化 DocEditor

**新增页面 `views/OfficePreview.vue`：**
- 路由：`/preview/office`（query param `userFileId`）
- 页面加载 OnlyOffice API JS：`<script src="{onlyoffice-api-url}"></script>`
- 使用 `new DocsAPI.DocEditor('editor', config)` 初始化编辑器
- config 来自 `POST /document/preview` 返回的 PreviewConfigVO
- 编辑器占满全屏（100vw x 100vh）

**新增 API 函数：**
- `getDocumentPreviewConfig(userFileId)` — POST `/document/preview`，返回 PreviewConfigVO
- `getDocumentEditConfig(userFileId)` — POST `/document/edit`，返回 EditConfigVO
- `getDocumentHistory(userFileId)` — GET `/document/{userFileId}/history`
- `restoreDocumentVersion(userFileId, version)` — POST `/document/{userFileId}/history/{version}/restore`

**新增路由：**
- `/preview/office` — OfficePreview.vue（需登录）

## 不在范围内

- 代码/Markdown 在线编辑保存功能 — 后端暂无 `modifyfilecontent` 端点，编辑保存留待后续
- 文件搜索预览高亮
- PDF 独立预览（非 OnlyOffice）— 优先使用 OnlyOffice，备选浏览器内置 PDF 阅读器
- 压缩包内文件预览
- 文件预览分享（分享页面中的预览功能）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~14 个（5 预览组件 + 1 共享顶部栏 PreviewTopBar + 1 Office 页面 + 6 composable + 1 类型补充） |
| 修改文件 | 4 个（views/FileView.vue / api/file.ts +8 函数 / router/index.ts / types/file.ts） |
| 删除文件 | 0 个 |
| 新增依赖 | js-base64（歌词解码）、@codemirror/* 系列（代码预览）、markdown-it + highlight.js（Markdown 渲染） |
