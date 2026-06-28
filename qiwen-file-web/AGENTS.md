# AGENTS.md — 前端模块（qiwen-file-web）

> 本文件是前端 Vue 代码的模块级约定，补充根目录 AGENTS.md 中的通用规则。

## 目录结构

```
src/
├── main.js                    ← 入口文件，全局注册插件和样式
├── App.vue                    ← 根组件（Header + router-view + Footer）
├── config/index.js            ← 全局配置（domain, siteName, baseContext, tokenKeyName）
├── plugins/
│   ├── element.js             ← Element UI 按需引入（~50 个组件）
│   └── fileOperationPlugins.js ← 命令式弹窗/浮层服务自动注册（require.context）
├── libs/
│   ├── map.js                 ← 静态映射表（文件类型图标、代码模式、列配置）
│   └── globalFunction/        ← 全局工具函数（common.js + file.js）
├── request/                   ← API 调用层（按领域拆分）
│   ├── http.js                ← Axios 实例、拦截器、get/post/put/delete 封装
│   ├── user.js                ← 用户相关 API（5 个端点）
│   ├── file.js                ← 文件相关 API（22 个端点）
│   ├── home.js                ← 首页 API（公告、系统参数）
│   ├── admin.js               ← 管理后台 API（4 个端点）
│   └── onlyoffice.js          ← OnlyOffice API（3 个端点）
├── router/
│   ├── router.js              ← 路由定义（history 模式，懒加载）
│   └── before.js              ← 全局前置守卫（登录检查、标题更新）
├── store/
│   ├── index.js               ← Vuex 根 store + root getters
│   └── module/
│       ├── user.js            ← 登录状态、用户信息
│       ├── fileList.js        ← 列配置、视图模式、选中文件、批量操作
│       ├── sideMenu.js        ← 存储用量
│       ├── common.js          ← 屏幕宽度（响应式）
│       └── uploadFile.js      ← 上传面板显隐
├── components/
│   ├── Header.vue             ← 顶部导航栏
│   ├── Footer.vue             ← 底部版权信息
│   ├── common/                ← 通用组件（BreadCrumb, DragVerify, FileTable, MarkdownPreview）
│   ├── file/                  ← 文件管理组件
│   │   ├── AsideMenu.vue      ← 左侧分类菜单
│   │   ├── FileList.vue       ← 文件列表主区域（编排者）
│   │   ├── components/        ← FileGrid, FileTimeLine, OperationMenu, SelectColumn
│   │   ├── dialog/            ← 命令式弹窗服务（11 个）
│   │   └── box/               ← 命令式浮层服务（7 个）
│   └── home/                  ← 首页组件（Banner, Notice, Function）
├── views/                     ← 页面组件
│   ├── Home.vue               ← 首页
│   ├── Login.vue              ← 登录页
│   ├── Register.vue           ← 注册页
│   ├── File.vue               ← 文件管理页
│   ├── Share.vue              ← 分享查看页
│   ├── OnlyOffice.vue         ← 文档编辑页
│   ├── AdminUserList.vue      ← 管理后台
│   ├── ErrorPage/404.vue      ← 404 页
│   └── notice/                ← 公告列表 + 详情
└── assets/
    ├── images/                ← 静态图片（文件类型图标、Logo、首页 Banner）
    └── styles/
        ├── varibles.styl      ← 主题变量（与 Element UI 色板对齐）
        ├── base.styl          ← 全局重置
        ├── mixins.styl        ← Stylus mixins（setScrollbar, setEllipsis）
        ├── elementCover.styl  ← Element UI 样式覆盖
        ├── mediaScreenXs.styl ← 响应式样式（≤768px）
        └── iconfont/          ← 图标字体文件
```

## 关键约定

### 新增页面组件

1. 文件放在 `views/` 下，PascalCase 命名
2. 在 `router/router.js` 中添加路由，使用懒加载：`() => import(/* webpackChunkName: "xxx" */ '_v/Xxx.vue')`
3. 需要登录的路由加 `meta: { requireAuth: true }`

### 新增可复用组件

1. 文件放在 `components/` 对应子目录下
2. 使用 `<style lang="stylus" scoped>`
3. 覆盖 Element UI 样式用 `::v-deep` 或 `>>>`
4. Props 必须声明类型和默认值

### 新增 API 调用

1. 在 `request/` 下对应领域文件中添加
2. 格式：`export const xxxApi = (p) => get('/endpoint', p)` 或 `post('/endpoint', p)`
3. 不在组件中直接写 axios 调用

### 新增 Vuex 状态

1. 在 `store/module/` 下对应模块中添加 state / mutation / action
2. 需要在根 getters 中暴露的，加到 `store/index.js` 的 getters
3. 需要持久化的状态，在 mutation 中同步写入 localStorage（key 前缀 `qiwen_`）

### 命令式弹窗/浮层

1. 弹窗组件放在 `components/file/dialog/xxx/` 下
2. 浮层组件放在 `components/file/box/xxx/` 下
3. 使用 `Vue.extend()` 创建实例，返回 Promise（`confirm` / `cancel`）
4. 会被 `fileOperationPlugins.js` 通过 `require.context()` 自动注册到 `this.$openDialog.*` 或 `this.$openBox.*`

### 样式规范

- 颜色使用 `varibles.styl` 中定义的变量，不硬编码颜色值
- Element UI 主色：`#409EFF`，成功：`#67C23A`，警告：`#E6A23C`，危险：`#F56C6C`
- 响应式断点：768px（通过 `$store.state.common.screenWidth` 判断）
- 多行文本截断用 `setEllipsis(line)` mixin
- 自定义滚动条用 `setScrollbar(width, trackColor, thumbColor)` mixin

### 代码风格

- **Tab 缩进**（Prettier `useTabs: true, tabWidth: 2`）
- **无分号**（Prettier `semi: false`）
- **单引号**（Prettier `singleQuote: true`）
- **无尾逗号**（Prettier `trailingComma: 'none'`）
- **路径别名**：`_c/`、`_v/`、`_a/`、`_r/`、`@/` 优先使用，不用相对路径
- **中文注释**：本项目注释使用中文

### API 响应格式

后端统一返回格式，前端按以下方式判断：

```javascript
xxxApi(params).then(res => {
  if (res.success) {
    // 成功：使用 res.data（单条）或 res.dataList（列表）+ res.total（分页总数）
  } else {
    // 失败：使用 res.message 展示错误信息
  }
})
```

## 构建与部署

- 开发：`npm run serve`（端口 8081，代理 `/api` → `localhost:8080`）
- 构建：`npm run build`（输出到 `dist/`，不生成 sourcemap）
- 部署：`dist/` 由 Nginx 托管，`/api` 反向代理到后端
- Nginx 配置见 `qiwen-file-web/nginx.conf`
