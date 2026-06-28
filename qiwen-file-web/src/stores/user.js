import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getUserInfo } from '_api/user'
import { TOKEN_KEY } from '_api/http'
import router from '@/router'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // ---- state ----
  const isLogin = ref(!!localStorage.getItem(TOKEN_KEY))
  const userInfo = ref(null)
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')

  // ---- actions ----
  async function login(credentials) {
    try {
      const res = await loginApi(credentials)
      if (res.success) {
        token.value = res.data
        localStorage.setItem(TOKEN_KEY, res.data)
        isLogin.value = true
        await fetchUserInfo()
        ElMessage.success('登录成功')
        router.push('/home')
      } else {
        ElMessage.error(res.message || '登录失败')
      }
    } catch {
      // 错误已在 http.js 拦截器中处理
    }
  }

  async function register(data) {
    try {
      const res = await registerApi(data)
      if (res.success) {
        ElMessage.success('注册成功，请登录')
        return true
      } else {
        ElMessage.error(res.message || '注册失败')
        return false
      }
    } catch {
      return false
    }
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      if (res.success) {
        userInfo.value = res.data
      }
    } catch {
      // 静默失败
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    isLogin.value = false
    localStorage.removeItem(TOKEN_KEY)
    router.push('/login')
  }

  return { isLogin, userInfo, token, login, register, fetchUserInfo, logout }
})
