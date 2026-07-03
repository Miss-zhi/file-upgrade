# frontend-preview — 实施任务清单

## 1. 环境准备与依赖安装

- [x] 1.1 安装 CodeMirror 6 系列依赖：`@codemirror/view`、`@codemirror/state`、`@codemirror/language`、`@codemirror/commands`、`@codemirror/autocomplete`、`@codemirror/fold`、`@codemirror/theme-one-dark`
- [x] 1.2 安装 markdown-it + highlight.js：`markdown-it`、`highlight.js`
- [x] 1.3 安装 js-base64（音频歌词解码）
- [x] 1.4 验证所有依赖安装成功，`npm list` 无冲突

## 2. API 层扩展

- [x] 2.1 在 `api/file.ts` 新增 `getFileContent(userFileId)` — Axios GET 下载端点，`responseType: 'blob'`
- [x] 2.2 在 `api/file.ts` 新增 `getFileText(userFileId)` — Axios GET 下载端点，`responseType: 'text'`
- [x] 2.3 在 `api/file.ts` 新增 `getDocumentPreviewConfig(userFileId)` — POST `/document/preview`
- [x] 2.4 在 `api/file.ts` 新增 `getDocumentEditConfig(userFileId)` — POST `/document/edit`
- [x] 2.5 在 `api/file.ts` 新增 `getDocumentHistory(userFileId)` — GET `/document/{userFileId}/history`
- [x] 2.6 在 `api/file.ts` 新增 `restoreDocumentVersion(userFileId, version)` — POST `/document/{userFileId}/history/{version}/restore`

## 3. 类型定义补充

- [x] 3.1 在 `types/file.ts` 新增 `PreviewType` 枚举（image/video/audio/code/markdown/office/unknown）
- [x] 3.2 在 `types/file.ts` 扩展 `FileInfo` 接口添加可选字段 `music?: MusicMetadata`（含 trackLength/albumImage/lyrics/artist/album），供音频预览使用
- [x] 3.3 在 `types/file.ts` 新增 `OnlyOfficeConfig` 接口（document / editorConfig / token）

## 4. Composable 层 — 预览路由调度

- [x] 4.1 创建 `composables/usePreviewRouter.ts` — `openFilePreview(file, fileList?)` 根据扩展名路由到对应预览
- [x] 4.2 实现扩展名 → 预览类型映射逻辑（图片/视频/音频/Office/PDF/Markdown/代码/未知→下载）
- [x] 4.3 实现 `getFileExtension()` 辅助函数提取文件扩展名

## 5. Composable 层 — 图片预览

- [x] 5.1 创建 `composables/useImagePreview.ts` — `{ visible, open(file, fileList?), close }`
- [x] 5.2 实现文件内容加载（blob → Object URL）
- [x] 5.3 实现缩放控制（CSS zoom，1%-200% 范围）
- [x] 5.4 实现旋转控制（CSS transform rotate(Ndeg)）
- [x] 5.5 实现键盘事件（Escape/←/→）
- [x] 5.6 实现侧栏折叠状态 localStorage 持久化

## 6. 预览组件 — 图片预览

- [x] 6.1 创建 `components/preview/ImagePreview.vue` — 全屏覆盖层，z-index: 2
- [x] 6.2 实现顶部信息栏（文件名/序号/旋转/下载/关闭）
- [x] 6.3 实现左侧缩略图侧栏（80x80px，折叠/展开，高亮当前）
- [x] 6.4 实现主图显示区域（居中，缩放，旋转）
- [x] 6.5 实现底部缩放控制栏（600px 宽滑块）
- [x] 6.6 实现左右箭头导航按钮

## 7. Composable 层 — 视频预览

- [x] 7.1 创建 `composables/useVideoPreview.ts` — `{ visible, open(file, fileList?), close }`
- [x] 7.2 实现文件内容加载（blob → Object URL）
- [x] 7.3 实现播放速率控制（0.5x/1x/1.5x/2x）
- [x] 7.4 实现视频格式兼容性检测（canPlayType 降级）
- [x] 7.5 实现播放列表状态管理
- [x] 7.6 实现移动端自动折叠（≤768px）

## 8. 预览组件 — 视频预览

- [x] 8.1 创建 `components/preview/VideoPreview.vue` — 全屏覆盖层，z-index: 3
- [x] 8.2 实现顶部信息栏（文件名+大小/下载/折叠/关闭）
- [x] 8.3 实现 HTML5 `<video>` 播放器 + 原生 controls
- [x] 8.4 实现右侧播放列表侧栏（280px，可折叠）
- [x] 8.5 实现不支持的格式降级提示 + 下载链接

## 9. Composable 层 — 音频预览

- [x] 9.1 创建 `composables/useAudioPreview.ts` — `{ visible, open(file, fileList?), close }`
- [x] 9.2 实现音乐元数据获取（getFileDetail → music.trackLength/albumImage/lyrics/artist/album）
- [x] 9.3 实现 LRC 歌词解析（Base64 解码 → `[mm:ss.xx]text` 正则解析 → 当前行高亮滚动）
- [x] 9.4 实现三种循环模式切换（列表循环 1 / 单曲循环 2 / 随机 3）
- [x] 9.5 实现键盘事件（Escape/←/→/↑/↓/Space）

## 10. 预览组件 — 音频预览

- [x] 10.1 创建 `components/preview/AudioPreview.vue` — 全屏覆盖层，z-index: 3
- [x] 10.2 实现模糊专辑背景（filter: blur(65px), opacity: 0.6）
- [x] 10.3 实现右上角操作框（关闭按钮）
- [x] 10.4 实现左侧曲目列表（名称/播放暂停/下载/大小/路径）
- [x] 10.5 实现右侧专辑封面+歌词区域（340px，160x160 封面，曲名/歌手/专辑，歌词滚动）
- [x] 10.6 实现底部控制栏（上一首/播放暂停/下一首/进度/时间/循环模式/音量）

## 11. Composable 层 — 代码预览

- [x] 11.1 创建 `composables/useCodePreview.ts` — `{ visible, open(file, fileList?), close }`
- [x] 11.2 实现文件内容加载（getFileText → 文本内容）
- [x] 11.3 实现语言模式自动检测（fileSuffixCodeModeMap，yml→yaml 映射）
- [x] 11.4 实现主题选择 + localStorage 持久化 `qiwen_file_codemirror_theme`（⚠️ 旧项目 69 种 CodeMirror 5 主题不兼容 CM6，需使用 CM6 内置主题 + `cm6-themes` 社区包覆盖主要场景，至少提供 10 种可选主题）
- [x] 11.5 实现 CodeMirror 6 编辑器配置（tabSize: 4, lineNumbers: true, readOnly: true, foldGutter: true）

## 12. 预览组件 — 代码预览

- [x] 12.1 创建 `components/preview/CodePreview.vue` — 全屏覆盖层，z-index: 2
- [x] 12.2 实现顶部信息栏（文件名/"在线预览"/下载/关闭）
- [x] 12.3 实现工具栏（自动换行/字号/语言选择/主题选择）
- [x] 12.4 实现 CodeMirror 6 编辑器集成（90vw × calc(100vh-80px)）

## 13. Composable 层 — Markdown 预览

- [x] 13.1 创建 `composables/useMarkdownPreview.ts` — `{ visible, open(file, fileList?), close }`
- [x] 13.2 实现文件内容加载（getFileText → 文本内容）
- [x] 13.3 实现 markdown-it 解析配置（启用 highlight.js 代码高亮插件）

## 14. 预览组件 — Markdown 预览

- [x] 14.1 创建 `components/preview/MarkdownPreview.vue` — 全屏覆盖层，z-index: 2
- [x] 14.2 实现顶部信息栏（文件名/"在线预览"/下载/关闭）
- [x] 14.3 实现桌面双栏布局（>768px：源码左 + 渲染右）
- [x] 14.4 实现移动端单栏布局（≤768px：仅渲染）
- [x] 14.5 实现渲染容器（90vw × calc(100vh-80px)）

## 15. 共享组件

- [x] 15.1 创建 `components/preview/PreviewTopBar.vue` — 共享顶部栏（文件名/标签/操作按钮）
- [x] 15.2 统一各预览组件的顶部栏为 PreviewTopBar 复用

## 16. Office 预览页面

- [x] 16.1 创建 `views/OfficePreview.vue` — 独立页面
- [x] 16.2 实现 OnlyOffice API JS 动态加载（`<script>` 标签注入）
- [x] 16.3 实现 DocEditor 初始化（`new DocsAPI.DocEditor('editor', config)`）
- [x] 16.4 实现编辑器全屏布局（100vw × 100vh）
- [x] 16.5 实现编辑模式支持（"在线编辑"入口使用 edit config）

## 17. 路由更新

- [x] 17.1 在 `router/index.ts` 新增 `/preview/office` 路由 → OfficePreview.vue
- [x] 17.2 为 `/preview/office` 添加登录守卫（meta: { requiresAuth: true }）

## 18. FileView 整合

- [x] 18.1 修改 `views/FileView.vue` 的 `handleOpenFile`，替换 `downloadFile()` 占位为 `openFilePreview()` 调用
- [x] 18.2 处理分类视图场景：传入同目录同类型文件列表作为 fileList 参数
- [x] 18.3 处理右键菜单"查看"动作（已有 → handleOpenFile，无需额外修改）
- [x] 18.4 在 FileView.vue 中注册所有预览 composable 实例

## 19. 视觉样式还原

- [x] 19.1 图片预览样式与旧项目 1:1 对齐（顶部栏/侧栏/主图/底部缩放栏）
- [x] 19.2 视频预览样式与旧项目 1:1 对齐（顶部栏/播放器/播放列表）
- [x] 19.3 音频预览样式与旧项目 1:1 对齐（模糊背景/曲目列表/封面歌词/控制栏）
- [x] 19.4 代码预览样式与旧项目 1:1 对齐（顶部栏/工具栏/编辑器）
- [x] 19.5 Markdown 预览样式与旧项目 1:1 对齐（顶部栏/双栏布局/渲染区）

## 20. 验证测试

- [x] 20.1 手动测试图片预览（打开/关闭/缩放/旋转/键盘/侧栏折叠/序号跳转）
- [x] 20.2 手动测试视频预览（播放/暂停/速率/播放列表/折叠/不兼容格式降级）
- [x] 20.3 手动测试音频预览（播放/暂停/曲目切换/循环模式/音量/歌词滚动/元数据显示）
- [x] 20.4 手动测试代码预览（语法高亮/语言检测/主题切换/自动换行/字号调整）
- [x] 20.5 手动测试 Markdown 预览（渲染/代码块高亮/双栏/移动端单栏）
- [x] 20.6 手动测试 Office 预览（新标签页打开/编辑器加载/预览模式）
- [x] 20.7 手动测试未识别文件类型降级下载
- [x] 20.8 验证所有 localStorage 持久化（图片侧栏状态/代码主题）
- [x] 20.9 验证移动端响应式布局（视频/音频/Markdown ≤768px 适配）
