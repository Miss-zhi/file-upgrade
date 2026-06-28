# AGENTS.md — 前端模块（qiwen-file-web）

> 本文件是前端 Vue 3 代码的模块级约定，补充根目录 AGENTS.md 中的通用规则。

## 技术栈

- Vue 3.4.x + Composition API（`<script setup>`）
- Element Plus 2.5.x
- Vite 5.x
- Pinia 2.x（Setup Store 风格）
- Vue Router 4.x
- Axios 1.x
- Stylus

## 目录结构

```
src/
├── main.js                      ← 入口（createApp + 插件注册）
├── App.vue                      ← 根组件
├── config/index.js              ← 全局配置
├── plugins/
│   ├── element.js               ← Element Plus 按需引入
│   └── fileOperationPlugins.js  ← 命令式弹窗/浮层注册
├── composables/                 ← 组合式函数（useXxx）
├── libs/                        ← 静态映射 + 工具函数
├── api/                         ← API 调用层
│   ├── http.js                  ← Axios 实例 + 拦截器
│   ├── user.js / file.js / home.js / admin.js / onlyoffice.js
├── router/
│   ├── index.js                 ← 路由定义
│   └── guards.js                ← 全局守卫
├── stores/                      ← Pinia Store（Setup Store）
│   ├── user.js / fileList.js / sideMenu.js / common.js / uploadFile.js
├── components/
│   ├── AppHeader.vue / AppFooter.vue
│   ├── common/                  ← BreadCrumb, DragVerify, FileTable, MarkdownPreview
│   ├── file/                    ← AsideMenu, FileList, dialog/, box/
│   └── home/                    ← Banner, Notice, Function
├── views/                       ← 页面组件
└── assets/styles/               ← Stylus 样式
```

## 组件模板

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useFileListStore } from '@/stores/fileList'

const props = defineProps({
  fileName: { type: String, required: true }
})
const emit = defineEmits(['update', 'delete'])

const fileListStore = useFileListStore()
const isLoading = ref(false)

const displayList = computed(() =>
  fileListStore.files.filter(f => f.fileName.includes(props.fileName))
)

onMounted(async () => {
  isLoading.value = true
  await fileListStore.fetchFiles()
  isLoading.value = false
})
</script>

<template>
  <div class="file-list"><!-- 内容 --></div>
</template>

<style lang="stylus" scoped>
.file-list
  // 样式
</style>
```

## 规则

- **必须 `<script setup>`**，不用 Options API
- **Props**：`defineProps()`；**Emits**：`defineEmits()`
- **Pinia Setup Store**：
  ```javascript
  export const useUserStore = defineStore('user', () => {
    const isLogin = ref(false)
    const login = async (credentials) => { ... }
    return { isLogin, login }
  })
  ```
- **组合式函数**以 `use` 开头，放 `composables/`
- **响应式**：`ref()` 简单值，`reactive()` 对象，`computed()` 计算值
- **命令式弹窗**用 `createApp` 替代 `Vue.extend`
- **Element Plus** 按需引入（unplugin-auto-import + unplugin-vue-components）
- **样式覆盖**用 `:deep()`，不用 `>>>` 或 `::v-deep`
- **路径别名**：`@/`、`_c/`、`_v/`、`_a/`、`_api/`
- **Tab 缩进**，无分号，单引号，无尾逗号
- **中文注释**

## API 调用模式

```javascript
import { getFileListByPath } from '_api/file'

const fetchData = async () => {
  const res = await getFileListByPath(params)
  if (res.success) {
    fileList.value = res.dataList
    total.value = Number(res.total)
  }
}
```

## 构建

- 开发：`npm run dev`（Vite，端口 5173）
- 构建：`npm run build`
- 类型检查：`npm run typecheck`（vue-tsc --noEmit）
- 代理：`/api` → `localhost:8080`
