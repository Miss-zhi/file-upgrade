# Design: frontend-scaffold

## 架构决策

### 1. Vite 5 替代 Vue CLI

**决策**：使用 Vite 5 作为构建工具。

**理由**：
- 原生 ESM 开发服务器，冷启动 < 1s
- Rollup 生产打包，Tree Shaking 更好
- Vue 3 官方推荐
- CI 已适配（`npx vite build`）

### 2. Element Plus 按需引入

**决策**：使用 `unplugin-vue-components` + `unplugin-auto-import` 实现按需引入，不全局注册。

**vite.config.ts 配置**：
```ts
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

AutoImport({
  resolvers: [ElementPlusResolver()],
})
Components({
  resolvers: [ElementPlusResolver()],
})
```

**效果**：组件自动导入，无需手动 `import { ElButton } from 'element-plus'`。

### 3. Pinia Setup Store 风格

**决策**：统一使用 Setup Store（Composition API 风格），不用 Options Store。

```javascript
export const useUserStore = defineStore('user', () => {
  const isLogin = ref(false)
  const login = async (credentials) => { ... }
  return { isLogin, login }
})
```

### 4. 命令式弹窗：createApp 替代 Vue.extend

**决策**：Vue 3 废弃 `Vue.extend`，改用 `createApp` 挂载弹窗组件。

```javascript
import { createApp } from 'vue'
import DialogComponent from './Dialog.vue'

export function openDialog(props) {
  const container = document.createElement('div')
  const app = createApp(DialogComponent, props)
  app.mount(container)
  document.body.appendChild(container)
  return app
}
```

### 5. Axios 双拦截器

**请求拦截器**：自动附加 `Authorization: Bearer <token>`（从 localStorage 读取）。
**响应拦截器**：401 → 跳转登录，其他错误 → `ElMessage.error`。

### 6. 路径别名

| 别名 | 路径 | 用途 |
|---|---|---|
| `@/` | `src/` | 源码根 |
| `_c/` | `src/components/` | 可复用组件 |
| `_v/` | `src/views/` | 页面组件 |
| `_a/` | `src/assets/` | 静态资源 |
| `_api/` | `src/api/` | API 调用 |

## 依赖版本矩阵

| 依赖 | 版本 |
|---|---|
| vue | ^3.4.21 |
| vite | ^5.2.0 |
| @vitejs/plugin-vue | ^5.0.4 |
| element-plus | ^2.5.0 |
| pinia | ^2.1.7 |
| vue-router | ^4.3.0 |
| axios | ^1.6.7 |
| stylus | ^0.62.0 |
| typescript | ^5.4.0 |
| vue-tsc | ^2.0.6 |
| eslint | ^8.57.0 |
| eslint-plugin-vue | ^9.22.0 |
| @typescript-eslint/parser | ^7.1.0 |
| unplugin-auto-import | ^0.17.5 |
| unplugin-vue-components | ^0.26.0 |

## 文件列表（预计新增）

```
qiwen-file-web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.node.json
├── eslint.config.js
├── index.html
├── .gitignore
└── src/
    ├── main.ts
    ├── App.vue
    ├── config/index.js
    ├── plugins/
    │   ├── element.js
    │   └── fileOperationPlugins.js
    ├── composables/
    ├── libs/
    ├── api/
    │   ├── http.js
    │   ├── user.js
    │   ├── file.js
    │   ├── home.js
    │   └── admin.js
    ├── router/
    │   ├── index.js
    │   └── guards.js
    ├── stores/
    │   ├── user.js
    │   ├── fileList.js
    │   ├── sideMenu.js
    │   ├── common.js
    │   └── uploadFile.js
    ├── components/
    │   ├── AppHeader.vue
    │   ├── AppFooter.vue
    │   ├── common/  （BreadCrumb, DragVerify, FileTable, MarkdownPreview）
    │   ├── file/    （AsideMenu, FileList, dialog/, box/）
    │   └── home/    （Banner, Notice, Function）
    ├── views/
    └── assets/styles/
```
