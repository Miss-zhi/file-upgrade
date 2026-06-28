# 前端骨架 — 项目结构与基础设施

## Purpose

定义奇文网盘前端模块（qiwen-file-web）的技术骨架：Vite 5 构建 + Vue 3.4 Composition API + Element Plus 2.5 按需引入 + Pinia 2 Setup Store + Vue Router 4 + Axios JWT 拦截器，确保 ESLint + 生产构建通过、CI 兼容。

## ADDED Requirements

### Requirement: package.json 依赖声明
package.json SHALL declare all dependencies at specified versions, and npm install SHALL complete without errors

#### Scenario: npm install 成功

- **WHEN** 执行 npm install
- **THEN** 无安装错误，node_modules 生成

### Requirement: Vite 构建配置
vite.config.ts SHALL configure five path aliases (@/ _c/ _v/ _a/ _api/), dev proxy /api → localhost:8080, and Element Plus on-demand imports via unplugin

#### Scenario: dev server 启动

- **GIVEN** vite.config.ts 存在且配置正确
- **WHEN** 执行 npx vite --port 5173
- **THEN** 服务启动，无配置错误

#### Scenario: 路径别名解析

- **GIVEN** import 使用 @/ _c/ _v/ _a/ _api/ 别名
- **WHEN** 执行 npx vite build
- **THEN** 别名正确解析，构建成功

### Requirement: Axios HTTP 封装
Axios 实例 SHALL have baseURL /api, request interceptor SHALL attach JWT token from localStorage, response interceptor SHALL handle 401 and other errors uniformly

#### Scenario: 请求拦截器附加 JWT

- **GIVEN** localStorage 中有 token
- **WHEN** 发起 Axios 请求
- **THEN** 请求头包含 Authorization: Bearer <token>

#### Scenario: 响应拦截器处理 401

- **GIVEN** 服务端返回 401
- **WHEN** 响应拦截器捕获错误
- **THEN** 清除 token 并跳转登录页

### Requirement: Vue Router 4 路由
Vue Router 4 SHALL define base routes (Login, Layout, File, Home, Share, Admin) with navigation guards checking authentication

#### Scenario: 路由表可构建

- **GIVEN** router/index.js 和 guards.js 存在
- **WHEN** 执行 npx vite build
- **THEN** 路由导入无编译错误

### Requirement: Pinia 2 Setup Store
Five Pinia stores SHALL be created in Setup Store style, each exposing reactive state + async actions

#### Scenario: Stores 可编译

- **GIVEN** 所有 5 个 Store 文件存在
- **WHEN** 执行 npx vite build
- **THEN** 无导入/编译错误

### Requirement: 命令式弹窗服务
Command-style dialogs SHALL use createApp instead of deprecated Vue.extend, registered via this.$openDialog and this.$openBox

#### Scenario: 弹窗插件可导入

- **GIVEN** fileOperationPlugins.js 存在
- **WHEN** 使用 createApp 挂载组件
- **THEN** 无 Vue.extend 引用

### Requirement: Element Plus 按需引入
Element Plus SHALL be imported on-demand via unplugin-vue-components + unplugin-auto-import plugins in vite.config.ts

#### Scenario: 组件自动导入

- **GIVEN** vite.config.ts 包含 ElementPlusResolver
- **WHEN** 模板中使用 <el-button>
- **THEN** 无需手动 import，构建成功

### Requirement: ESLint + 类型检查
ESLint + vue-tsc SHALL be configured and produce zero errors on the initial skeleton code

#### Scenario: ESLint 无错误

- **GIVEN** eslint.config.js 存在
- **WHEN** 执行 npx eslint src/ --ext .vue,.js,.ts
- **THEN** 0 errors, 0 warnings

#### Scenario: vue-tsc 无错误

- **GIVEN** tsconfig.json 存在
- **WHEN** 执行 npx vue-tsc --noEmit
- **THEN** 0 类型错误

### Requirement: CI 构建验证
Dev server and production build SHALL both succeed, proving the skeleton is CI-ready

#### Scenario: 生产构建成功

- **GIVEN** 所有源文件就绪
- **WHEN** 执行 npx vite build
- **THEN** dist/ 目录生成，BUILD SUCCESS
