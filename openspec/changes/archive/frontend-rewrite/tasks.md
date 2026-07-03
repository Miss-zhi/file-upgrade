## 1. 静态资源迁移

- [x] 1.1 从旧项目 `E:\file\qiwen-file-web\src\assets\images\file\` 复制全部60+文件类型图标到新项目 `src/assets/images/file/`
- [x] 1.2 从旧项目复制iconfont全套文件（iconfont.css, .eot, .woff, .ttf, .svg）到新项目 `src/assets/styles/iconfont/`
- [x] 1.3 从旧项目复制logo图片（logo_header.png, logo_header_xs.png, logo_footer.png）到新项目 `src/assets/images/common/`
- [x] 1.4 从旧项目复制其他图片资源（banner1.png, 404.png, wave.gif, 功能图标等）到新项目对应目录
- [x] 1.5 更新 `types/file.ts` 的 `fileImgMap` 映射表，确保所有文件扩展名指向正确的图标路径
- [x] 1.6 在 `main.ts` 中 import iconfont CSS 文件

## 2. API层对齐

- [x] 2.1 修改 `api/home.ts` 的 `getSystemParams()`：移除对 `/param/grouplist` 的调用，改为返回硬编码默认版权信息
- [x] 2.2 修改 `api/notice.ts`：移除对 `/notice/list` 和 `/notice/detail` 的调用，改为返回空数据（空数组/空对象）
- [x] 2.3 新增 `api/file.ts` 中的 `getQuotaInfo()` 函数，调用 `GET /quota/info`，返回 `QuotaInfoVO` 类型
- [x] 2.4 修改 `stores/sideMenu.ts` 的 `fetchStorage()`：优先调用 `getQuotaInfo()`，降级使用 `getStorage()`
- [x] 2.5 新增 `types/file.ts` 中的 `QuotaInfoVO` 接口定义（totalQuota, usedQuota, availableQuota）
- [x] 2.6 校验所有API函数的请求参数名与后端Controller的 `@RequestParam` 完全一致（重点检查分页参数 page/pageSize）
- [x] 2.7 校验 `types/file.ts` 的 `PageResult<T>` 字段与后端 Page 响应一致
- [x] 2.8 校验 `types/admin.ts` 的 `PageResponse<T>` 字段与后端 PageResponse 响应一致
- [x] 2.9 校验 `types/file.ts` 的 `FileInfo` 字段与后端 `FileListVO` 字段完全一致
- [x] 2.10 校验 `types/file.ts` 的 `ShareInfo` 字段与后端 `ShareInfoVO` 完全一致
- [x] 2.11 更新 HomeView/NoticeListView/NoticeDetailView 处理空数据的降级渲染逻辑

## 3. 缺失UI功能实现

- [x] 3.1 安装 `canvas-nest.js` 依赖：`npm install canvas-nest.js`
- [x] 3.2 创建 `composables/useCanvasNest.ts`：封装canvas-nest初始化和销毁逻辑，接受el ref、color、count参数
- [x] 3.3 修改 `views/LoginView.vue`：集成useCanvasNest，粒子颜色RGB(64,158,255)，数量99
- [x] 3.4 修改 `views/RegisterView.vue`：集成useCanvasNest，粒子颜色RGB(230,162,60)，数量99
- [x] 3.5 创建 `components/common/DragVerify.vue`：滑块验证组件，宽375px，handler背景#F5F7FA，v-model:verified双向绑定
- [x] 3.6 修改 `views/LoginView.vue`：集成DragVerify组件，验证成功后启用登录按钮
- [x] 3.7 修改 `views/RegisterView.vue`：集成DragVerify组件，验证成功后启用注册按钮
- [x] 3.8 实现FileView的拖拽上传全屏覆盖层：监听dragenter/dragover/dragleave/drop事件，显示fixed遮罩(z-index 19, border 5px dashed #8091a5)
- [x] 3.9 实现Ctrl+V截图粘贴上传：监听paste事件，从clipboard获取图片数据加入上传队列
- [x] 3.10 还原登录/注册页表单样式：表单宽375px, padding-top 50px, 标题30px/font-weight 300/color #000, 输入框图标前缀, 登录按钮全宽type primary

## 4. 布局框架样式对齐

- [x] 4.1 对照旧项目Header.vue的Stylus样式，修正AppHeader的padding/box-shadow/logo margin
- [x] 4.2 对照旧项目Footer.vue的Stylus样式，修正AppFooter的渐变背景/padding/logo宽度
- [x] 4.3 对照旧项目AsideMenu.vue的Stylus样式，修正AppAside的菜单项选中态背景(#ecf5ff)/折叠按钮样式(12px×100px, 圆角0 16px 16px 0, 背景#DCDFE6)/存储条高度66px/右侧padding 11px
- [x] 4.4 对照旧项目App.vue的Stylus样式，修正AppLayout的主内容区width 90%/min-height calc(100vh-70px)/margin 0 auto
- [x] 4.5 对照旧项目mediaScreenXs.styl，修正移动端Header/Footer/Aside的响应式细节

## 5. 文件管理组件样式对齐

- [x] 5.1 对照旧项目OperationMenu样式，修正工具栏的padding 16px 0/搜索框250px宽/按钮间距
- [x] 5.2 对照旧项目FileTable样式，修正表格高度calc(100vh-206px)/calc(100vh-211px)/列宽(类型80px/大小100px/日期160px)/滚动条(6px, #C0C4CC)
- [x] 5.3 对照旧项目FileGrid样式，修正网格项宽(gridSize+40px)/文件名高44px/hover效果(bg #F5F7FA, font-weight 550)
- [x] 5.4 对照旧项目BreadCrumb样式，修正高度30px/"当前位置："标签/箭头分隔符
- [x] 5.5 对照旧项目ContextMenu样式，修正菜单项高36px/padding 0 16px/hover bg #ecf5ff
- [x] 5.6 对照旧项目Pagination样式，修正高度44px/顶部边框1px solid #DCDFE6
- [x] 5.7 对照旧项目UploadPanel样式，修正定位right 16px bottom 16px/宽560px/圆角7px 7px 0 0/标题栏40px/列表240px
- [x] 5.8 修正FileTable列prop名与后端FileListVO字段名一致
- [x] 5.9 修正FileTable排序方法（sortMethod）确保可用

## 6. 首页和分享页样式对齐

- [x] 6.1 对照旧项目Banner.vue的Stylus样式，修正HomeBanner：el-carousel高度360px/渐变蓝色背景/左侧标题+描述+CTA/右侧装饰图(max-width 443px)/内容区85%
- [x] 6.2 对照旧项目Function.vue的Stylus样式，修正HomeFeatures：3列网格(每列32%)/max-width 1200px/100×100圆形图标容器/背景#ecf5ff/hover渐变蓝+白字
- [x] 6.3 对照旧项目Notice.vue的Stylus样式，修正HomeNotice：3条公告自动轮播/手动箭头暂停恢复/无数据时不渲染
- [x] 6.4 对照旧项目Share.vue的Stylus样式，修正ShareView：提取码验证弹窗流程/文件列表区域复用BreadCrumb+FileTable/"保存到网盘"按钮样式/未登录跳转

## 7. 弹窗样式对齐

- [x] 7.1 对照旧项目各dialog的Stylus样式，修正弹窗宽度(大部分550px, AddFolder 580px)/margin-top 9vh
- [x] 7.2 修正移动端弹窗宽度80%/圆角8px
- [x] 7.3 修正弹窗footer按钮样式（取消/确认按钮间距和大小）

## 8. 预览组件样式对齐

- [x] 8.1 对照旧项目imgPreview/BoxMask.vue，修正图片预览布局：遮罩rgba(0,0,0,0.8)/顶部栏48px/缩略图侧栏120px(80×80)/缩放滑块600px/箭头60px
- [x] 8.2 对照旧项目videoPreview/BoxMask.vue，修正视频预览布局：遮罩rgba(0,0,0,0.75)/顶部栏48px/播放列表侧栏280px
- [x] 8.3 对照旧项目audioPreview/BoxMask.vue，修正音频预览布局：背景#303133/右侧340px(160×160专辑图)/控制栏120px/键盘快捷键
- [x] 8.4 对照旧项目codePreview/BoxMask.vue，修正代码预览布局：编辑器90vw/calc(100vh-80px)/设置工具栏
- [x] 8.5 对照旧项目markdownPreview/BoxMask.vue，修正Markdown预览布局

## 9. 响应式全局对齐

- [x] 9.1 对照旧项目mediaScreenXs.styl，修正responsive.scss中768px断点下的所有组件样式变化
- [x] 9.2 修正Message/Notification在移动端的宽度(80%, max 480px)
- [x] 9.3 修正上传面板在≤520px时全宽
- [x] 9.4 修正DatePicker在移动端的宽度(350px)

## 10. Bug修复

- [x] 10.1 修复batchDownload：确认POST请求体为number[]数组，响应为Blob(ZIP)
- [x] 10.2 修复quota校验时序：确保上传前调用配额接口，不足时阻止上传
- [x] 10.3 修复spark-md5导入：确保TypeScript类型声明完整，移除所有any类型
- [x] 10.4 修复getStorage()调用路径：确认调用 `/filetransfer/getstorage`（非其他路径）

## 11. 验证

- [x] 11.1 运行 `npm run build` 确认TypeScript类型检查通过，无编译错误
- [x] 11.2 运行 `npm run dev` 启动开发服务器，逐页面验证渲染正常
- [x] 11.3 验证登录/注册页：粒子背景显示、滑块验证交互、表单提交
- [x] 11.4 验证首页：Banner轮播/功能区9卡片/公告滚动，降级时不显示空白
- [x] 11.5 验证文件管理页：侧栏菜单/文件列表三视图/工具栏/右键菜单/弹窗/上传面板
- [x] 11.6 验证预览组件：图片/视频/音频/代码/Markdown预览的布局和交互
- [x] 11.7 验证分享页：提取码验证流程/文件列表显示/保存到网盘按钮
- [x] 11.8 验证管理页面：用户/角色/配额/日志/配置5个子页面
- [x] 11.9 验证响应式：在768px断点下检查Header/Aside/Footer/弹窗/文件列表的适配
- [x] 11.10 验证API对齐：确认notice/home页面降级渲染正常，配额数据显示正确
