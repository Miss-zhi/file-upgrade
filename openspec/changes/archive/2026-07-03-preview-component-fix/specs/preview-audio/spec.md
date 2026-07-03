## MODIFIED Requirements

### Requirement: 音频预览主题色与布局
全屏音频播放器，左侧曲目列表+右侧专辑封面歌词+底部控制栏。全局强调色 MUST 为橙色 `$Warning(#E6A23C)`，非蓝色。

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

#### Scenario: 控制按钮使用iconfont
- **WHEN** 渲染播放控制按钮
- **THEN** MUST 使用iconfont图标：上一首(icon-shangyishou)、播放(icon-icon-7)、暂停(icon-icon-3)、下一首(icon-xiayishou)，字号40px，hover颜色 `$Warning(#E6A23C)`

#### Scenario: 循环模式图标和颜色
- **WHEN** 渲染循环模式按钮
- **THEN** MUST 使用iconfont图标：列表循环(icon-xunhuanbofang)、单曲循环(icon-danquxunhuan1)、随机(icon-suijibofang1)，hover颜色 `$Warning(#E6A23C)`

#### Scenario: 歌词容器渐变遮罩
- **WHEN** 渲染歌词容器
- **THEN** MUST 应用 `-webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(255,255,255,0.6) 15%, #fff 25%, #fff 75%, rgba(255,255,255,0.6) 85%, transparent)`

#### Scenario: 歌词行样式
- **WHEN** 渲染歌词行
- **THEN** `line-height: 40px`，非active hover变白色
- **WHEN** 歌词行为当前行
- **THEN** 颜色 MUST 为 `$Warning(#E6A23C)`（非蓝色）

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
