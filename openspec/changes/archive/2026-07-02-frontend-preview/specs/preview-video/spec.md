# preview-video — 视频预览规格

## ADDED Requirements

### Requirement: 全屏覆盖层视频预览

系统 SHALL 提供全屏覆盖层视频预览组件，使用 HTML5 `<video>` 原生控件。

#### Scenario: 打开视频预览
- **WHEN** 用户触发视频预览
- **THEN** 系统显示全屏覆盖层，z-index: 3，背景 rgba(0,0,0,0.75)

#### Scenario: 关闭视频预览
- **WHEN** 用户点击关闭按钮或按 Escape 键
- **THEN** 视频播放停止，覆盖层关闭

### Requirement: 顶部信息栏

系统 SHALL 在覆盖层顶部显示 48px 高的纯黑信息栏，包含文件名/大小、下载链接、折叠播放列表按钮和关闭按钮。

#### Scenario: 顶部栏显示视频信息
- **WHEN** 视频预览打开
- **THEN** 顶部栏显示：文件名 + 文件大小、下载链接、折叠按钮、关闭按钮

### Requirement: 视频播放器

系统 SHALL 使用 HTML5 `<video>` 元素播放视频，支持播放速率选择（0.5x/1x/1.5x/2x）。

#### Scenario: 视频播放
- **WHEN** 视频预览打开
- **THEN** 视频自动开始加载，显示原生 controls（播放/暂停/进度/音量/全屏）

#### Scenario: 播放速率切换
- **WHEN** 用户选择播放速率（0.5x/1x/1.5x/2x）
- **THEN** 视频按选择速率播放

#### Scenario: 不支持的视频格式降级
- **WHEN** 浏览器不支持该视频格式（canPlayType 返回空）
- **THEN** 系统显示提示信息并提供下载链接

### Requirement: 右侧播放列表

系统 SHALL 在视频播放器右侧显示 280px 宽的播放列表侧栏，可折叠。

#### Scenario: 播放列表显示
- **WHEN** 视频预览打开且存在多个视频文件
- **THEN** 右侧显示 280px 宽的播放列表，列出所有视频

#### Scenario: 折叠播放列表
- **WHEN** 用户点击折叠播放列表按钮
- **THEN** 播放列表隐藏，视频播放器扩展至全宽

#### Scenario: 通过播放列表切换视频
- **WHEN** 用户点击播放列表中的某个视频
- **THEN** 播放器切换到该视频

### Requirement: 移动端适配

系统 SHALL 在屏幕宽度 ≤768px 时自动折叠播放列表。

#### Scenario: 移动端自动折叠播放列表
- **WHEN** 视口宽度 ≤768px
- **THEN** 播放列表默认折叠，视频播放器占满全宽
