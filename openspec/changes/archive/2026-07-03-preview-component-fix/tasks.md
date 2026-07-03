## 1. 图片预览修复

- [x] 1.1 修复ImagePreview关闭按钮：在close-btn的el-link中添加 `<Close />` 图标组件，字号18px
- [x] 1.2 修复缩略图暗色遮罩：为非active的.min-img-item添加 `::after` 伪元素（`content:''; position:absolute; top:0; left:0; width:80px; height:80px; background:#000; opacity:0.4`），hover时opacity变0.2
- [x] 1.3 修正导航箭头位置：从 `left:20px/right:20px` 改为 `left:64px/right:64px`
- [x] 1.4 修正缩放滑块颜色：通过 `:deep()` 覆盖el-slider的 `.el-slider__bar` 和 `.el-slider__button` 颜色为 `#303133`

## 2. 视频预览修复

- [x] 2.1 修正顶部栏背景色：从 `rgba(0,0,0,0.5)` 改为 `#000`（纯黑）
- [x] 2.2 添加播放列表标题头：在.video-list-wrapper顶部添加 `.list-title`（内容"播放列表"，高40px，底部 `2px solid #606266`，padding `0 16px`，白色文字）
- [x] 2.3 修正列表项active样式：active项背景改为 `#000`，文字颜色 `#409EFF`（去掉半透明蓝色背景）
- [x] 2.4 修正列表项hover样式：hover时文字变蓝色 `#409EFF`（无背景变化）
- [x] 2.5 添加播放列表自定义滚动条：宽8px，轨道 `#EBEEF5`，滑块 `#909399`，border-radius 2em

## 3. 音频预览修复（最大改动）

- [x] 3.1 全局主题色替换：将AudioPreview.vue中所有 `#409EFF` 替换为 `#E6A23C`（橙色），涉及active项、hover、进度条、歌词高亮、音量条等约30+处
- [x] 3.2 控制按钮改为iconfont：将 ⏮▶️⏭ emoji替换为 `<i class="iconfont icon-shangyishou">` / `<i class="iconfont icon-icon-7">` / `<i class="iconfont icon-icon-3">` / `<i class="iconfont icon-xiayishou">`，字号从28px改为40px
- [x] 3.3 循环模式按钮改为iconfont：将循环/单曲/随机emoji替换为 `<i class="iconfont icon-xunhuanbofang">` / `icon-danquxunhuan1` / `icon-suijibofang1`
- [x] 3.4 音量图标改为iconfont：将音量emoji替换为 `<i class="iconfont icon-yinliang101">` / `icon-jingyin01`
- [x] 3.5 添加音频列表表头：在.audio-list顶部添加 `.audio-list-header`（列：文件名/大小/路径，高56px，border-radius 8px）
- [x] 3.6 添加播放动画wave.gif：从旧项目复制 `wave.gif` 到 `src/assets/images/audio/`，正在播放的曲目显示12×12的wave动画图标
- [x] 3.7 添加歌词渐变遮罩：为.lyrics-container添加 `-webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(255,255,255,0.6) 15%, #fff 25%, #fff 75%, rgba(255,255,255,0.6) 85%, transparent)`
- [x] 3.8 修正歌词行样式：设置 `line-height: 40px`，active颜色改为 `#E6A23C`
- [x] 3.9 重构控制栏布局：从三段式(control-buttons/progress-area/control-options)改为左右分栏(control-left: flex:1 含按钮+进度条+时间, control-right: 340px 含循环/下载/分享/音量)
- [x] 3.10 修正进度条样式：通过 `:deep()` 覆盖el-slider，滑轨高度2px，进度条颜色 `#E6A23C`，滑块无border
- [x] 3.11 修正音量条样式：宽100px，滑轨高度2px，进度条颜色 `#E6A23C`，滑块无border
- [x] 3.12 修正关闭按钮位置：从 `right:16px` 改为 `right:32px`，hover颜色改为 `#E6A23C`

## 4. 代码预览修复

- [x] 4.1 修改工具栏外观：将.operate-wrapper从暗色半透明(`rgba(255,255,255,0.05)`)改为白色背景(`#fff`)、圆角(`8px 8px 0 0`)、底部边框(`1px solid #DCDFE6`)
- [x] 4.2 修正编辑器容器尺寸：margin-top从84px改为56px，高度从 `calc(100vh-84px)` 改为 `calc(100vh-80px)`
- [x] 4.3 修正编辑器内部高度：CodeMirror编辑器高度改为 `calc(100vh - 129px)`，圆角 `0 0 8px 8px`
- [x] 4.4 添加编辑器自定义滚动条：宽12px，透明轨道，`#C0C4CC` 滑块，border-radius 2em
- [x] 4.5 修正编辑器字体：确保使用 `SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace`
- [x] 4.6 安装CodeMirror 6主题包：`npm install @highlightjs/codemirror-theme` 或等效主题包
- [x] 4.7 扩展主题选择：在主题select中注册10+个主题选项（Dracula、Solarized Light/Dark、GitHub Dark、Monokai、Nord、Material等），更新themeExtension映射逻辑
- [x] 4.8 修正工具栏表单项宽度：字号select 96px、语言select 120px、主题select 190px

## 5. Markdown预览修复

- [x] 5.1 为渲染区域设置浅色背景：`.render-pane` 背景改为 `#fff`，padding 16px，圆角 `8px`
- [x] 5.2 编写GitHub风格浅色CSS：为 `.markdown-body` 内的h1-h6、p、code、pre、table、a、blockquote、ul/ol等元素编写浅色主题样式（代码块背景 `#f6f8fa`、文字 `#24292e`、表格边框 `#dfe2e5`、链接 `#409EFF`、引用 `#6a737d`）
- [x] 5.3 修正容器margin-top：从56px确认与旧项目一致（旧项目 `margin: 56px auto`）
- [x] 5.4 修正源码区域样式：`.source-pane` 背景 `rgba(0,0,0,0.3)`，文字 `rgba(255,255,255,0.8)`，等宽字体，右侧分隔线 `1px solid rgba(255,255,255,0.1)`
- [x] 5.5 在useMarkdownPreview composable中添加 `fontSize` 状态（默认14，选项[12,14,16,18,20]），持久化到localStorage key `qiwen_file_markdown_fontsize`
- [x] 5.6 在MarkdownPreview顶部栏添加字号select控件（el-select，选项12/14/16/18/20px），`.render-pane` 和 `.source-code` 的font-size通过 `v-bind` 绑定到fontSize

## 6. 验证

- [x] 6.1 打开图片预览：验证关闭按钮可见、缩略图暗色遮罩效果、箭头位置、缩放滑块颜色
- [x] 6.2 打开视频预览：验证顶部栏纯黑、播放列表标题存在、列表项active/hover样式
- [x] 6.3 打开音频预览：验证全局橙色主题、iconfont图标、列表表头、播放动画、歌词遮罩、控制栏左右布局、进度条/音量条样式
- [x] 6.4 打开代码预览：验证工具栏白色背景、编辑器尺寸、滚动条样式、主题切换(10+选项)
- [x] 6.5 打开Markdown预览：验证渲染区域浅色背景、GitHub风格样式、源码+渲染双栏布局、字号切换(5档)且刷新后保持
- [x] 6.6 运行 `npm run build` 确认无TypeScript编译错误
