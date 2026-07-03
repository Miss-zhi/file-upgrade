# preview-image — 图片预览规格

## ADDED Requirements

### Requirement: 全屏覆盖层图片预览

系统 SHALL 提供全屏覆盖层图片预览组件，还原旧项目的视觉和交互体验。

#### Scenario: 打开图片预览
- **WHEN** 用户触发图片预览
- **THEN** 系统显示全屏覆盖层，背景从透明动画过渡到 rgba(0,0,0,0.8)，z-index: 2

#### Scenario: 关闭图片预览
- **WHEN** 用户点击关闭按钮、按 Escape 键或点击背景区域
- **THEN** 系统关闭全屏覆盖层

### Requirement: 顶部信息栏

系统 SHALL 在覆盖层顶部显示 48px 高的半透明信息栏，包含折叠按钮、文件名、序号、旋转按钮、下载链接和关闭按钮。

#### Scenario: 顶部栏显示文件信息
- **WHEN** 图片预览覆盖层打开
- **THEN** 顶部栏显示：当前文件名、序号输入框（当前/总数）、旋转按钮、下载链接、关闭按钮

#### Scenario: 序号跳转
- **WHEN** 用户在序号输入框中输入数字并确认
- **THEN** 系统切换到对应序号的图片

### Requirement: 左侧缩略图侧栏

系统 SHALL 在左侧显示可折叠的缩略图侧栏，每项 80x80px，可折叠状态持久化到 localStorage `qiwen_file_img_preview_show_min`。

#### Scenario: 缩略图侧栏显示
- **WHEN** 图片预览打开且存在多张图片
- **THEN** 左侧显示缩略图列表，每项 80x80px，当前图片高亮

#### Scenario: 折叠/展开缩略图侧栏
- **WHEN** 用户点击折叠按钮
- **THEN** 侧栏折叠，主图区域扩展至全宽，状态保存到 localStorage

#### Scenario: 通过缩略图切换图片
- **WHEN** 用户点击某张缩略图
- **THEN** 主图区域切换到对应图片

### Requirement: 主图显示区域

系统 SHALL 在主区域居中显示当前图片，支持缩放和旋转。

#### Scenario: 主图显示
- **WHEN** 图片预览打开
- **THEN** 主图区域（top:48px, right:0, left:120px, bottom:0）居中显示图片

#### Scenario: 鼠标滚轮缩放
- **WHEN** 用户在图片上滚动鼠标滚轮
- **THEN** 图片按 1%-200% 范围缩放，CSS zoom 属性实现

#### Scenario: 滑块缩放
- **WHEN** 用户拖动底部缩放滑块
- **THEN** 图片缩放比例随滑块变化

#### Scenario: 旋转图片
- **WHEN** 用户点击旋转按钮
- **THEN** 图片顺时针旋转 90 度，使用 CSS transform: rotate

### Requirement: 键盘导航

系统 SHALL 支持键盘快捷键操作：Escape 关闭、← 上一张、→ 下一张。

#### Scenario: Escape 关闭预览
- **WHEN** 用户按下 Escape 键
- **THEN** 图片预览关闭

#### Scenario: 方向键切换图片
- **WHEN** 用户按下左/右方向键
- **THEN** 主图切换到上一张/下一张图片

### Requirement: 底部缩放控制栏

系统 SHALL 在底部居中显示缩放控制栏，包含缩放滑块和缩放比例显示。

#### Scenario: 缩放栏显示
- **WHEN** 图片预览打开
- **THEN** 底部显示 600px 宽的缩放栏，包含滑块和当前缩放百分比
