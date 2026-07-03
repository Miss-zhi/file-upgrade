# missing-ui-features — 缺失UI功能补全

## Description

补全前端缺失的 UI 功能，包括粒子背景、滑块验证、iconfont 图标、文件类型图标资源、拖拽上传覆盖层、登录注册表单样式还原。

## Requirements

### Requirement: Canvas-nest粒子背景
登录页和注册页 SHALL 使用canvas-nest.js实现粒子动画背景，1:1还原旧项目视觉效果。

#### Scenario: 登录页粒子效果
- **WHEN** 用户访问登录页
- **THEN** 页面背景 MUST 渲染canvas-nest粒子动画，粒子颜色为RGB(64,158,255)（蓝色/Primary），粒子数量99个

#### Scenario: 注册页粒子效果
- **WHEN** 用户访问注册页
- **THEN** 页面背景 MUST 渲染canvas-nest粒子动画，粒子颜色为RGB(230,162,60)（橙色/Warning），粒子数量99个

#### Scenario: 页面离开清理
- **WHEN** 用户离开登录/注册页
- **THEN** canvas-nest实例 MUST 被销毁，不留内存泄漏

### Requirement: DragVerify滑块验证
登录和注册表单 SHALL 集成DragVerify滑块验证组件，用户滑动验证成功后才启用提交按钮。

#### Scenario: 滑块验证交互
- **WHEN** 用户填写完登录/注册表单
- **THEN** 显示DragVerify滑块（宽375px，handler背景#F5F7FA），用户需从左滑到右完成验证

#### Scenario: 验证成功
- **WHEN** 用户成功滑动到右端
- **THEN** 验证状态变为已完成，登录/注册按钮变为可点击

#### Scenario: 验证重置
- **WHEN** 登录/注册失败或页面刷新
- **THEN** 滑块验证 MUST 重置为初始状态

### Requirement: 自定义Iconfont图标
系统 SHALL 引入旧项目的iconfont图标集（项目id 2613341），用于音频播放器控制等场景。

#### Scenario: 音频控制图标
- **WHEN** 渲染音频预览组件
- **THEN** MUST 使用iconfont图标：播放(icon-icon-7)、暂停(icon-icon-3)、上一首(icon-shangyishou)、下一首(icon-xiayishou)

#### Scenario: 循环模式图标
- **WHEN** 渲染音频循环模式切换按钮
- **THEN** MUST 使用iconfont图标：列表循环(icon-xunhuanbofang)、单曲循环(icon-danquxunhuan1)、随机播放(icon-suijibofang1)

#### Scenario: 音量图标
- **WHEN** 渲染音量控制
- **THEN** MUST 使用iconfont图标：音量开(icon-yinliang101)、静音(icon-jingyin01)

#### Scenario: 列表模式图标
- **WHEN** 渲染工具栏视图模式切换
- **THEN** 列表模式 MUST 使用iconfont图标(icon-liebiao1)

### Requirement: 文件类型图标资源
系统 SHALL 包含60+个文件类型图标资源，覆盖所有常见文件格式。

#### Scenario: 图标资源完整
- **WHEN** 文件列表渲染文件图标
- **THEN** MUST 从 assets/images/file/ 加载对应图标，覆盖：Word(SVG)、Excel(SVG)、PPT(SVG)、PDF、TXT、ZIP、RAR、7Z、TAR、MP3、FLAC、各种代码文件(C/C++/Java/JS/Python/Go/Rust/Kotlin/Swift/PHP等)

#### Scenario: 未知类型降级
- **WHEN** 文件扩展名不在图标映射表中
- **THEN** MUST 显示 `file_unknown.png` 默认图标

#### Scenario: 目录图标
- **WHEN** 文件项为文件夹
- **THEN** MUST 显示 `dir.png` 图标

### Requirement: 拖拽上传全屏覆盖层
FileView页面 SHALL 实现拖拽上传的全屏覆盖层UI。

#### Scenario: 拖拽进入显示覆盖层
- **WHEN** 用户拖拽文件进入浏览器窗口（且当前在FileView页面）
- **THEN** 显示全屏fixed覆盖层，z-index 19，边框5px虚线#8091a5，半透明白色背景

#### Scenario: 释放文件触发上传
- **WHEN** 用户在覆盖层上释放拖拽的文件
- **THEN** 关闭覆盖层，将文件加入上传队列

#### Scenario: Ctrl+V截图粘贴
- **WHEN** 用户在FileView页面按Ctrl+V粘贴截图
- **THEN** 从clipboard获取图片数据，加入上传队列

### Requirement: 登录注册表单样式还原
登录和注册页 SHALL 1:1还原旧项目的表单布局和样式。

#### Scenario: 表单容器
- **WHEN** 渲染登录/注册页
- **THEN** 表单容器宽375px，padding-top 50px，最小高度 calc(100vh - 189px)

#### Scenario: 标题样式
- **WHEN** 渲染标题"登录"/"注册"
- **THEN** 字号30px，font-weight 300，颜色#000；副标题（站点名）颜色#999

#### Scenario: 输入框图标
- **WHEN** 渲染登录表单
- **THEN** 手机号输入框前缀图标为el-icon-mobile-phone，密码为el-icon-lock(show-password)；注册多一个用户名(el-icon-user)

#### Scenario: 登录按钮
- **WHEN** 渲染登录按钮
- **THEN** 全宽，type primary，padding 10px 90px，字号16px
