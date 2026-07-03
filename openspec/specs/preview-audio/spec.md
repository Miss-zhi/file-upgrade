# preview-audio — 音频预览

## Purpose

提供全屏覆盖层音频预览组件，包含模糊专辑背景、曲目列表、专辑封面、LRC 歌词滚动和底部控制栏。支持三种循环模式、键盘快捷键和音乐元数据展示。

## Requirements

### Requirement: 全屏覆盖层音频预览

系统 SHALL 提供全屏覆盖层音频预览组件，包含模糊专辑背景、曲目列表、专辑封面、歌词滚动和控制栏。全局强调色 MUST 为橙色 `$Warning(#E6A23C)`，非蓝色。

#### Scenario: 打开音频预览
- **WHEN** 用户触发音频预览
- **THEN** 系统显示全屏覆盖层，z-index: 3，背景色为 PrimaryText，模糊背景使用专辑封面图

#### Scenario: 整体背景
- **WHEN** 渲染音频预览
- **THEN** 全屏，背景 `#303133`，文字颜色 `#DCDFE6`，z-index 3

#### Scenario: 模糊背景图
- **WHEN** 渲染模糊背景
- **THEN** `position: fixed; top: -50%; left: 0; width: 100vw; filter: blur(65px); opacity: 0.6; z-index: -1`

#### Scenario: 关闭按钮位置和样式
- **WHEN** 渲染关闭按钮
- **THEN** `position: fixed; top: 16px; right: 32px`，字号30px，hover颜色变为 `$Warning(#E6A23C)`

#### Scenario: 主内容区布局
- **WHEN** 渲染主内容区
- **THEN** `width: 85%`，`height: calc(100vh - 120px)`，`padding-top: 32px`，flex左右分布

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

#### Scenario: 音频列表表头
- **WHEN** 渲染音频列表
- **THEN** MUST 显示表头行 `.audio-list-header`，包含列：文件名、大小、路径，高56px，border-radius 8px，padding `0 16px`

#### Scenario: 音频列表项
- **WHEN** 渲染音频列表项
- **THEN** 高56px，border-radius 8px，padding `0 16px`，hover背景 `rgba(0,0,0,0.1)`
- **WHEN** 列表项为当前播放
- **THEN** 背景 `rgba(0,0,0,0.1)`，文字颜色 MUST 为 `$Warning(#E6A23C)`（非蓝色）

#### Scenario: 播放动画
- **WHEN** 列表项正在播放
- **THEN** 文件名旁 MUST 显示 `wave.gif` 声波动画图标（12×12px）

#### Scenario: 点击曲目切换
- **WHEN** 用户点击曲目列表中的某首曲目
- **THEN** 播放器切换到该曲目

### Requirement: 专辑封面与歌词

系统 SHALL 在右侧 340px 区域显示 160x160 专辑封面、曲名/歌手/专辑信息和自动滚动的歌词。

#### Scenario: 显示专辑封面和元数据
- **WHEN** 音频预览打开且文件详情包含 music 字段
- **THEN** 右侧显示 160x160 专辑封面、曲名、歌手、专辑名

#### Scenario: 歌词容器渐变遮罩
- **WHEN** 渲染歌词容器
- **THEN** MUST 应用 `-webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(255,255,255,0.6) 15%, #fff 25%, #fff 75%, rgba(255,255,255,0.6) 85%, transparent)`

#### Scenario: 歌词自动滚动高亮
- **WHEN** 音频播放中且歌词（LRC 格式）存在
- **THEN** 系统解码 Base64 歌词，解析 LRC 时间戳，当前播放行自动高亮并滚动到可视区域

#### Scenario: 歌词行样式
- **WHEN** 渲染歌词行
- **THEN** `line-height: 40px`，非active hover变白色
- **WHEN** 歌词行为当前行
- **THEN** 颜色 MUST 为 `$Warning(#E6A23C)`（非蓝色）

#### Scenario: 无歌词时隐藏歌词区域
- **WHEN** 文件详情不包含 lyrics 字段
- **THEN** 歌词区域显示"暂无歌词"

### Requirement: 底部控制栏

系统 SHALL 在底部显示 120px 高的控制栏，包含上一首/播放暂停/下一首、进度滑块、时间、循环模式、音量控制。

#### Scenario: 播放控制
- **WHEN** 用户点击播放/暂停按钮或按 Space 键
- **THEN** 音频播放或暂停

#### Scenario: 播放控制按钮使用iconfont
- **WHEN** 渲染播放控制按钮
- **THEN** MUST 使用iconfont图标：上一首(icon-shangyishou)、播放(icon-icon-7)、暂停(icon-icon-3)、下一首(icon-xiayishou)，字号40px，hover颜色 `$Warning(#E6A23C)`

#### Scenario: 切换曲目
- **WHEN** 用户点击上一首/下一首按钮或按 ←/→ 方向键
- **THEN** 播放器切换到上一首/下一首曲目

#### Scenario: 循环模式切换
- **WHEN** 用户点击循环模式按钮
- **THEN** 循环模式在列表循环(1)/单曲循环(2)/随机(3)之间切换

#### Scenario: 循环模式图标和颜色
- **WHEN** 渲染循环模式按钮
- **THEN** MUST 使用iconfont图标：列表循环(icon-xunhuanbofang)、单曲循环(icon-danquxunhuan1)、随机(icon-suijibofang1)，hover颜色 `$Warning(#E6A23C)`

#### Scenario: 音量调节
- **WHEN** 用户拖动音量滑块或按 ↑/↓ 方向键
- **THEN** 音量相应调整

#### Scenario: 进度拖动
- **WHEN** 用户拖动进度滑块
- **THEN** 播放进度跳转到对应位置

#### Scenario: 控制栏布局
- **WHEN** 渲染底部控制栏
- **THEN** `width: 85%; height: 120px; padding: 24px 0 32px`，分为左右两部分：
  - `control-left`(flex:1)：上一首/播放/暂停/下一首(字号40px) + 进度条(flex:1) + 时间
  - `control-right`(宽340px)：循环模式 + 下载 + 分享 + 音量图标 + 音量滑块(宽100px)

#### Scenario: 进度条样式
- **WHEN** 渲染进度条(el-slider)
- **THEN** 滑轨高度2px，进度条颜色 `$Warning(#E6A23C)`，滑块无border

#### Scenario: 音量条样式
- **WHEN** 渲染音量条(el-slider)
- **THEN** 宽100px，滑轨高度2px，进度条颜色 `$Warning(#E6A23C)`，滑块无border

### Requirement: 音乐元数据获取

系统 SHALL 通过 `getFileDetail(userFileId)` API 获取音乐元数据，包括 `music.trackLength`、`music.albumImage`（Base64）、`music.lyrics`（Base64 编码 LRC）、`music.artist`、`music.album`。

#### Scenario: 获取音乐元数据
- **WHEN** 音频预览打开
- **THEN** 系统调用文件详情 API，解析 music 字段中的 trackLength/albumImage/lyrics/artist/album
