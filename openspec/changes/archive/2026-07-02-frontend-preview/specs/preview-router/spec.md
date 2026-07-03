# preview-router — 预览路由调度规格

## ADDED Requirements

### Requirement: 文件打开时根据扩展名路由到对应预览类型

系统 SHALL 在用户双击文件或点击"查看"菜单时，根据文件扩展名自动路由到对应的预览组件。

#### Scenario: 图片文件打开时启动图片预览
- **WHEN** 用户打开扩展名为 png/jpg/jpeg/gif/svg/webp/bmp 的文件
- **THEN** 系统启动 ImagePreview 全屏覆盖层，显示该图片

#### Scenario: 视频文件打开时启动视频预览
- **WHEN** 用户打开扩展名为 mp4/avi/mkv/mov/webm/flv 的文件
- **THEN** 系统启动 VideoPreview 全屏覆盖层，播放该视频

#### Scenario: 音频文件打开时启动音频预览
- **WHEN** 用户打开扩展名为 mp3/flac/wav/aac/ogg 的文件
- **THEN** 系统启动 AudioPreview 全屏覆盖层，播放该音频

#### Scenario: Office 文件打开时跳转 OnlyOffice 预览
- **WHEN** 用户打开扩展名为 doc/docx/xls/xlsx/ppt/pptx 的文件
- **THEN** 系统在新标签页打开 OnlyOffice 文档编辑器预览页面

#### Scenario: PDF 文件打开时跳转 OnlyOffice 预览
- **WHEN** 用户打开扩展名为 pdf 的文件
- **THEN** 系统在新标签页打开 OnlyOffice 预览页面

#### Scenario: Markdown 文件打开时启动 Markdown 预览
- **WHEN** 用户打开扩展名为 md/markdown 的文件
- **THEN** 系统启动 MarkdownPreview 全屏覆盖层，渲染该文件

#### Scenario: 代码文件打开时启动代码预览
- **WHEN** 用户打开扩展名在 fileSuffixCodeModeMap 中定义的文件
- **THEN** 系统启动 CodePreview 全屏覆盖层，以只读模式显示代码

#### Scenario: 未识别文件类型时执行下载
- **WHEN** 用户打开不属于任何预览类型的文件
- **THEN** 系统执行文件下载操作

### Requirement: 文件详情 API 获取文件元数据

系统 SHALL 在预览打开前，通过已有的 `getFileDetail(userFileId)` 函数（`GET /file/getfiledetail/{userFileId}`）获取文件的完整元数据（文件名、扩展名、大小、路径、music 字段）。

#### Scenario: 成功获取文件元数据
- **WHEN** 预览组件调用文件详情 API
- **THEN** 系统返回文件元数据，包含 fileName、extendName、fileSize、music 等字段

#### Scenario: 文件详情 API 失败时显示错误
- **WHEN** 文件详情 API 返回错误
- **THEN** 系统显示错误提示，不启动预览

### Requirement: 文件内容获取

系统 SHALL 通过下载端点获取文件内容用于浏览器端展示。图片/视频/音频使用 blob 模式，代码/Markdown 使用 text 模式。

#### Scenario: 获取二进制文件内容
- **WHEN** 图片/视频/音频预览组件调用 getFileContent
- **THEN** 系统通过 Axios `responseType: 'blob'` 获取文件内容并创建 Object URL

#### Scenario: 获取文本文件内容
- **WHEN** 代码/Markdown 预览组件调用 getFileText
- **THEN** 系统通过 Axios `responseType: 'text'` 获取文件文本内容

### Requirement: 文件列表批量预览

系统 SHALL 在分类视图（图片/视频/音频）中传入同目录所有同类型文件列表，支持前后切换。

#### Scenario: 分类视图中传入文件列表
- **WHEN** 用户在图片分类视图中打开某张图片
- **THEN** 预览组件接收当前目录所有图片文件列表，支持前后切换

#### Scenario: 非分类视图中仅预览单个文件
- **WHEN** 用户在非分类视图（全部文件/搜索结果）中打开文件
- **THEN** 预览组件仅显示当前文件，不支持前后切换
