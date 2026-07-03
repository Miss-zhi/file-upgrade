# 前端编码规范

## 组件风格

所有组件使用 `<script setup lang="ts">`。不使用 Options API（`export default { data(), methods: {} }`）。

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const props = defineProps<{
  title: string
  modelValue?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit'): void
}>()

const loading = ref(false)
const isVisible = computed(() => props.modelValue ?? false)
</script>
```

## 类型安全

TypeScript 严格模式（`strict: true` in `tsconfig.json`）。禁止 `any` 类型。

API 响应类型在 `types/` 目录统一定义：

```typescript
// types/api.ts
export interface RestResult<T> {
  code: number | string
  message: string
  data: T
  timestamp: number
}

// types/auth.ts
export interface LoginRequest {
  telephone: string
  password: string
}

export interface LoginResponse {
  userId: string
  username: string
  roles: string[]
  permissions: string[]
}

export interface UserInfoResponse {
  userId: string
  username: string
  telephone: string
  imageUrl: string
  roles: Array<{ roleKey: string; roleName: string }>
  permissions: string[]
}
```

Axios 请求和响应必须带泛型：

```typescript
const { data } = await apiClient.get<RestResult<UserInfoResponse>>('/api/v1/auth/me')
```

## API 请求层

按模块拆分（`api/auth.ts`、`api/file.ts`、`api/admin.ts` 等），统一封装 Axios 实例。

```typescript
// api/client.ts
import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,  // httpOnly cookie 自动携带
  timeout: 10000,
})

// 请求拦截器：可选，用于添加自定义 header
apiClient.interceptors.request.use((config) => {
  return config
})

// 响应拦截器：401 自动 refresh + 统一错误提示
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        await apiClient.post('/api/v1/auth/refresh')
        return apiClient(original)
      } catch {
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }
    // 统一错误提示
    const message = error.response?.data?.message ?? '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default apiClient
```

## 状态管理

Pinia stores 按功能模块拆分，使用 `defineStore` + setup 函数风格：

```typescript
// stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfoResponse | null>(null)
  const isAuthenticated = computed(() => user.value !== null)

  async function login(req: LoginRequest) {
    const { data } = await apiClient.post<RestResult<LoginResponse>>('/api/v1/auth/login', req)
    user.value = data.data
  }

  async function logout() {
    await apiClient.post('/api/v1/auth/logout')
    user.value = null
  }

  async function fetchMe() {
    try {
      const { data } = await apiClient.get<RestResult<UserInfoResponse>>('/api/v1/auth/me')
      user.value = data.data
    } catch {
      user.value = null
    }
  }

  return { user, isAuthenticated, login, logout, fetchMe }
})
```

持久化状态使用 `pinia-plugin-persistedstate`（如有需要）。

## 路由守卫

```typescript
// composables/useAuthGuard.ts
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useAuthGuard() {
  const router = useRouter()
  const authStore = useAuthStore()

  router.beforeEach(async (to) => {
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      await authStore.fetchMe()
      if (!authStore.isAuthenticated) {
        return { name: 'login', query: { redirect: to.fullPath } }
      }
    }
  })
}
```

## 组合式函数（Composables）

替代 Vue 2 的 mixins。对话弹窗等动态组件改用 composable 模式：

```typescript
// composables/useConfirmDialog.ts
export function useConfirmDialog() {
  const visible = ref(false)
  const title = ref('')
  const resolve: Ref<((value: boolean) => void) | null> = ref(null)

  function open(opts: { title: string }) {
    title.value = opts.title
    visible.value = true
    return new Promise<boolean>((r) => { resolve.value = r })
  }

  function confirm() { visible.value = false; resolve.value?.(true) }
  function cancel() { visible.value = false; resolve.value?.(false) }

  return { visible, title, open, confirm, cancel }
}
```

## 样式

使用 SCSS（不用 Stylus），Scoped 样式 + 全局变量。Element Plus 主题通过 CSS 变量覆盖：

```scss
// styles/variables.scss
:root {
  --el-color-primary: #409eff;
  --app-header-height: 60px;
  --app-sidebar-width: 220px;
}
```

## Vue 3 迁移要点

| Vue 2 写法 | Vue 3 写法 |
|-----------|-----------|
| `export default { data() {} }` | `<script setup lang="ts">` + `ref()` / `reactive()` |
| `this.$store` | `useXxxStore()` (Pinia) |
| `this.$router` / `this.$route` | `useRouter()` / `useRoute()` |
| `this.$emit('event')` | `defineEmits` + `emit('event')` |
| `beforeDestroy` / `destroyed` | `onBeforeUnmount` / `onUnmounted` |
| `Vue.extend` 动态实例化 | composable 函数或 `createApp` |
| `el-dialog :visible.sync` | `el-dialog v-model` |
| Vuex modules | Pinia stores（每个 store 独立文件） |
| `require.context` 批量导入 | `import.meta.glob` |
