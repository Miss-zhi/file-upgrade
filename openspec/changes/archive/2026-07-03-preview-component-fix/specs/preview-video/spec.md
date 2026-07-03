## MODIFIED Requirements

### Requirement: 视频预览遮罩与布局
全屏视频播放器，包含顶部信息栏和视频播放区+播放列表侧栏。

#### Scenario: 遮罩层
- **WHEN** 打开视频预览
- **THEN** 全屏遮罩，背景 `rgba(0,0,0,0.75)`，z-index 3

#### Scenario: 顶部信息栏背景
- **WHEN** 渲染顶部栏
- **THEN** 高度48px，背景 MUST 为 `#000`（纯黑，非半透明），底部margin 8px

#### Scenario: 顶部栏内容
- **WHEN** 渲染顶部栏内容
- **THEN** 左侧：文件名+文件大小(color `#909399`, 12px)；右侧：下载图标+折叠图标+关闭图标（各24px，白色，hover opacity 0.6，间距margin-left 8px）

#### Scenario: 播放区域
- **WHEN** 渲染播放区域
- **THEN** `height: calc(100vh - 60px)`，flex布局，播放器flex:1

#### Scenario: 播放列表标题头
- **WHEN** 渲染播放列表侧栏（280px宽，黑色背景）
- **THEN** MUST 显示"播放列表"标题头，高40px，底部2px solid `#606266` 边框，padding `0 16px`

#### Scenario: 播放列表项样式
- **WHEN** 渲染播放列表项
- **THEN** padding `8px 16px`，字号12px，hover变蓝色(`#409EFF`)
- **WHEN** 列表项为当前播放
- **THEN** 背景 `#000`，文字颜色 `#409EFF`

#### Scenario: 播放列表滚动条
- **WHEN** 播放列表内容溢出
- **THEN** 自定义滚动条（宽8px，轨道`#EBEEF5`，滑块`#909399`，border-radius 2em）
