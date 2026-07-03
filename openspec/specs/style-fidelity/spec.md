# style-fidelity — 样式精确度对齐

## Description

确保前端各组件的尺寸、间距、颜色、响应式断点与旧项目完全一致，从"大致像"提升到"像素级对齐"。

## Requirements

### Requirement: Header精确尺寸对齐
AppHeader SHALL 与旧项目的尺寸、间距、阴影完全一致。

#### Scenario: Header整体尺寸
- **WHEN** 渲染AppHeader
- **THEN** 高度 MUST 为61px（含logo margin），全宽，padding 0 20px，box-shadow与旧项目一致

#### Scenario: Logo尺寸和间距
- **WHEN** 渲染Header左侧Logo
- **THEN** Logo图片高度40px，margin 14px 24px

#### Scenario: 移动端Header
- **WHEN** screenWidth ≤ 768px
- **THEN** Logo切换为紧凑版(logo_header_xs.png)，高度24px，margin 12px 8px；菜单项高度缩至48px

### Requirement: Footer渐变和间距对齐
AppFooter SHALL 与旧项目的渐变背景、logo尺寸、间距完全一致。

#### Scenario: Footer背景渐变
- **WHEN** 渲染AppFooter
- **THEN** 背景 MUST 为 `linear-gradient(to right, #409EFF, #66b1ff)`

#### Scenario: Footer Logo和版权
- **WHEN** 渲染Footer内容
- **THEN** Logo宽240px（≤920px屏幕160px），版权文字在Logo下方

#### Scenario: 移动端Footer
- **WHEN** screenWidth ≤ 768px
- **THEN** Footer内容垂直堆叠，居中对齐

### Requirement: Aside菜单项样式对齐
AppAside的菜单项样式 SHALL 与旧项目完全一致。

#### Scenario: 菜单项选中态
- **WHEN** 菜单项为当前选中
- **THEN** 背景色 MUST 为 #ecf5ff

#### Scenario: 折叠按钮样式
- **WHEN** 渲染侧栏折叠切换条
- **THEN** 绝对定位右侧，宽12px，高100px，圆角 `0 16px 16px 0`，背景 #DCDFE6

#### Scenario: 存储容量条
- **WHEN** 渲染底部存储容量条
- **THEN** 高度66px，进度条颜色阈值: ≤50%绿色($success)、≤80%橙色($warning)、>80%红色($danger)

### Requirement: FileTable精确样式对齐
FileTable SHALL 与旧项目的表格高度、列宽、滚动条样式完全一致。

#### Scenario: 表格高度精确
- **WHEN** fileType=0（全部文件）
- **THEN** 表格高度 MUST 为 `calc(100vh - 206px)`
- **WHEN** fileType=6（回收站）
- **THEN** 表格高度 MUST 为 `calc(100vh - 211px)`（注意5px差异）
- **WHEN** fileType=8（分享）
- **THEN** 表格高度 MUST 为 `calc(100vh - 109px)`

#### Scenario: 自定义滚动条
- **WHEN** 表格内容溢出
- **THEN** 滚动条宽6px，透明轨道，#C0C4CC滑块

#### Scenario: 列宽定义
- **WHEN** 渲染表格列
- **THEN** 扩展名(类型)列80px，文件大小列100px，修改日期列160px

### Requirement: FileGrid精确样式对齐
FileGrid SHALL 与旧项目的网格项尺寸、hover效果完全一致。

#### Scenario: 网格项尺寸
- **WHEN** 渲染文件网格项
- **THEN** 每项宽度 MUST 为 gridSize + 40px，padding 8px，居中

#### Scenario: 文件名截断
- **WHEN** 渲染文件名
- **THEN** 2行省略号，12px字体，高度44px

#### Scenario: Hover效果
- **WHEN** 鼠标悬停
- **THEN** 背景 #F5F7FA，font-weight 550

### Requirement: 右键菜单样式对齐
ContextMenu SHALL 与旧项目的尺寸和交互效果一致。

#### Scenario: 菜单容器
- **WHEN** 渲染右键菜单
- **THEN** 白色背景，边框1px solid #EBEEF5，圆角4px，阴影

#### Scenario: 菜单项
- **WHEN** 渲染菜单项
- **THEN** 高度36px，padding 0 16px，hover背景 #ecf5ff + 蓝色文字

### Requirement: 弹窗统一样式
所有el-dialog弹窗 SHALL 保持统一的宽度和间距。

#### Scenario: 弹窗宽度
- **WHEN** 渲染文件操作弹窗
- **THEN** 大部分弹窗宽度550px，AddFolder弹窗580px

#### Scenario: 弹窗间距
- **WHEN** 渲染弹窗
- **THEN** margin-top 9vh

#### Scenario: 移动端弹窗
- **WHEN** screenWidth ≤ 768px
- **THEN** 弹窗宽度变为80%，圆角8px

### Requirement: 图片预览精确布局
图片预览组件 SHALL 与旧项目的布局尺寸完全一致。

#### Scenario: 遮罩层
- **WHEN** 打开图片预览
- **THEN** 全屏遮罩 rgba(0,0,0,0.8)，带淡入动画

#### Scenario: 顶部信息栏
- **WHEN** 渲染预览顶部栏
- **THEN** 高度48px，半透明黑色背景

#### Scenario: 缩略图侧栏
- **WHEN** 渲染缩略图侧栏
- **THEN** 宽120px，每张缩略图80×80，可折叠

#### Scenario: 缩放控制
- **WHEN** 渲染缩放滑块
- **THEN** 底部居中，宽600px，范围1%-200%

#### Scenario: 导航箭头
- **WHEN** 渲染前后导航箭头
- **THEN** 字号60px

### Requirement: 音频预览布局对齐
音频预览 SHALL 与旧项目的布局和控件一致。

#### Scenario: 整体布局
- **WHEN** 渲染音频预览
- **THEN** 全屏，背景#303133深色，模糊专辑封面背景

#### Scenario: 左右分栏
- **WHEN** 渲染音频预览内容
- **THEN** 左侧音频列表（名/大小/路径列），右侧340px（160×160专辑图+歌词）

#### Scenario: 底部控制栏
- **WHEN** 渲染控制栏
- **THEN** 高120px，包含：上一首/播放暂停/下一首、进度滑块、时间、循环模式、下载、分享、音量滑块

#### Scenario: 键盘快捷键
- **WHEN** 音频预览处于打开状态
- **THEN** Esc关闭、Space播放暂停、方向键上一首/下一首/音量调节

### Requirement: 视频预览布局对齐
视频预览 SHALL 与旧项目的布局一致。

#### Scenario: 整体布局
- **WHEN** 渲染视频预览
- **THEN** 全屏遮罩 rgba(0,0,0,0.75)，顶部栏48px黑底

#### Scenario: 播放列表侧栏
- **WHEN** 渲染视频播放列表
- **THEN** 右侧280px，黑色背景

### Requirement: 代码预览布局对齐
代码预览 SHALL 与旧项目的编辑器尺寸和工具栏一致。

#### Scenario: 编辑器尺寸
- **WHEN** 渲染代码预览
- **THEN** CodeMirror编辑器宽90vw，高 calc(100vh - 80px)，等宽字体

#### Scenario: 设置工具栏
- **WHEN** 渲染编辑器设置栏
- **THEN** 包含：自动换行checkbox、字号select、语言select、主题select

### Requirement: 分页组件样式对齐
分页 SHALL 与旧项目的高度和边框一致。

#### Scenario: 分页容器
- **WHEN** 渲染分页组件
- **THEN** 高度44px，顶部边框1px solid #DCDFE6

#### Scenario: 分页内容
- **WHEN** 渲染分页内容
- **THEN** 左侧"当前页X条"，中间el-pagination

### Requirement: 上传面板样式对齐
UploadPanel SHALL 与旧项目的定位和尺寸一致。

#### Scenario: 面板定位和尺寸
- **WHEN** 渲染上传面板
- **THEN** 固定定位 right 16px, bottom 16px, 宽560px, z-index 20

#### Scenario: 面板外观
- **WHEN** 渲染面板
- **THEN** 白色背景，圆角 7px 7px 0 0，阴影，标题栏40px，文件列表240px可滚动

#### Scenario: 移动端上传面板
- **WHEN** screenWidth ≤ 520px
- **THEN** 上传面板全宽

### Requirement: 首页Banner样式对齐
HomeBanner SHALL 与旧项目的轮播样式完全一致。

#### Scenario: 轮播容器
- **WHEN** 渲染HomeBanner
- **THEN** el-carousel高度360px，渐变蓝色背景

#### Scenario: 内容布局
- **WHEN** 渲染轮播内容
- **THEN** 左侧标题+3行描述+CTA按钮，右侧装饰图片(max-width 443px)，内容区宽度85%

#### Scenario: 移动端Banner
- **WHEN** screenWidth ≤ 768px
- **THEN** Banner内容垂直堆叠，装饰图片隐藏或缩小

### Requirement: 首页功能区样式对齐
HomeFeatures SHALL 与旧项目的功能卡片样式完全一致。

#### Scenario: 卡片网格布局
- **WHEN** 渲染功能区
- **THEN** 3列网格，每列宽32%，最大宽度1200px居中

#### Scenario: 单个卡片
- **WHEN** 渲染功能卡片
- **THEN** 100×100圆形图标容器(内部70×70图标)，下方标题+描述，背景#ecf5ff

#### Scenario: 卡片Hover效果
- **WHEN** 鼠标悬停功能卡片
- **THEN** 背景过渡为渐变蓝色+白色文字

### Requirement: 首页公告区样式对齐
HomeNotice SHALL 与旧项目的公告滚动样式一致。

#### Scenario: 公告列表
- **WHEN** 渲染HomeNotice
- **THEN** 获取最新3条公告，自动轮播(setInterval)，手动箭头暂停/恢复

#### Scenario: 无数据降级
- **WHEN** 公告数据为空（降级状态）
- **THEN** 不渲染公告区域，不显示空白占位

### Requirement: 分享页样式对齐
ShareView SHALL 与旧项目的分享页布局和交互样式一致。

#### Scenario: 提取码验证流程
- **WHEN** 用户访问分享页
- **THEN** 先检查过期→检查类型(公开/私密)→私密时显示提取码输入框→验证通过后显示文件列表

#### Scenario: 文件列表区域
- **WHEN** 验证通过显示分享文件
- **THEN** 复用BreadCrumb+FileTable组件，样式与主文件管理页一致，额外显示"保存到网盘"按钮

#### Scenario: 未登录用户保存
- **WHEN** 未登录用户点击"保存到网盘"
- **THEN** 提示需要登录，跳转到登录页并携带redirect参数

### Requirement: 响应式断点细节对齐
所有响应式变化 SHALL 在768px断点精确触发，行为与旧项目一致。

#### Scenario: 768px断点触发
- **WHEN** screenWidth ≤ 768px
- **THEN** Header logo切紧凑版、侧栏变Drawer(210px从左滑出)、弹窗宽80%圆角8px、Message宽80% max 480px、时间线图标隐藏

#### Scenario: Message宽度
- **WHEN** 显示Element Plus Message
- **THEN** 宽度80%，最大480px
