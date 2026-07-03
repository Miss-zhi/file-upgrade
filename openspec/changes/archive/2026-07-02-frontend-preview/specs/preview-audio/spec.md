# preview-audio — 音频预览规格

## ADDED Requirements

### Requirement: 全屏覆盖层音频预览

系统 SHALL 提供全屏覆盖层音频预览组件，包含模糊专辑背景、曲目列表、专辑封面、歌词滚动和控制栏。

#### Scenario: 打开音频预览
- **WHEN** 用户触发音频预览
- **THEN** 系统显示全屏覆盖层，z-index: 3，背景色为 PrimaryText，模糊背景使用专辑封面图

#### Scenario: 关闭音频预览
- **WHEN** 用户点击关闭按钮或按 Escape 键
- **THEN** 音频播放停止，覆盖层关闭

### Requirement: 模糊专辑背景

系统 SHALL 使用专辑封面图作为全屏模糊背景（filter: blur(65px), opacity: 0.6）。

#### Scenario: 显示模糊背景
- **WHEN** 音频预览打开且文件详情包含 albumImage（Base64）
- **THEN** 系统解码 albumImage 并设为模糊背景

#### Scenario: 无专辑封面时使用默认背景
- **WHEN** 文件详情不包含 albumImage
- **THEN** 系统使用默认纯色背景

### Requirement: 曲目列表

系统 SHALL 在左侧显示可滚动的曲目列表，每行包含名称、播放暂停按钮、下载、大小、路径信息。

#### Scenario: 曲目列表显示
- **WHEN** 音频预览打开且存在多个音频文件
- **THEN** 左侧显示曲目列表，当前播放曲目高亮

#### Scenario: 点击曲目切换
- **WHEN** 用户点击曲目列表中的某首曲目
- **THEN** 播放器切换到该曲目

### Requirement: 专辑封面与歌词

系统 SHALL 在右侧 340px 区域显示 160x160 专辑封面、曲名/歌手/专辑信息和自动滚动的歌词。

#### Scenario: 显示专辑封面和元数据
- **WHEN** 音频预览打开且文件详情包含 music 字段
- **THEN** 右侧显示 160x160 专辑封面、曲名、歌手、专辑名

#### Scenario: 歌词自动滚动高亮
- **WHEN** 音频播放中且歌词（LRC 格式）存在
- **THEN** 系统解码 Base64 歌词，解析 LRC 时间戳，当前播放行自动高亮并滚动到可视区域

#### Scenario: 无歌词时隐藏歌词区域
- **WHEN** 文件详情不包含 lyrics 字段
- **THEN** 歌词区域显示"暂无歌词"

### Requirement: 底部控制栏

系统 SHALL 在底部显示 120px 高的控制栏，包含上一首/播放暂停/下一首、进度滑块、时间、循环模式、音量控制。

#### Scenario: 播放控制
- **WHEN** 用户点击播放/暂停按钮或按 Space 键
- **THEN** 音频播放或暂停

#### Scenario: 切换曲目
- **WHEN** 用户点击上一首/下一首按钮或按 ←/→ 方向键
- **THEN** 播放器切换到上一首/下一首曲目

#### Scenario: 循环模式切换
- **WHEN** 用户点击循环模式按钮
- **THEN** 循环模式在列表循环(1)/单曲循环(2)/随机(3)之间切换

#### Scenario: 音量调节
- **WHEN** 用户拖动音量滑块或按 ↑/↓ 方向键
- **THEN** 音量相应调整

#### Scenario: 进度拖动
- **WHEN** 用户拖动进度滑块
- **THEN** 播放进度跳转到对应位置

### Requirement: 音乐元数据获取

系统 SHALL 通过 `getFileDetail(userFileId)` API 获取音乐元数据，包括 `music.trackLength`、`music.albumImage`（Base64）、`music.lyrics`（Base64 编码 LRC）、`music.artist`、`music.album`。

#### Scenario: 获取音乐元数据
- **WHEN** 音频预览打开
- **THEN** 系统调用文件详情 API，解析 music 字段中的 trackLength/albumImage/lyrics/artist/album
