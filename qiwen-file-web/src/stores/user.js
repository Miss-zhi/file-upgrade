import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, login as loginApi, register as registerApi } from '_api/user'
import { TOKEN_KEY } from '_api/http'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // ---- state ----
  const isLogin = ref(false)
  const userInfo = ref(null)
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')

  // ---- actions ----
  async function login(credentials) {
    const res = await loginApi(credentials)
    if (res.success) {
      token.value = res.data
      localStorage.setItem(TOKEN_KEY, res.data)
      isLogin.value = true
      await fetchUserInfo()
      router.push('/home')
    }
  }

  async function register(data) {
    const res = await registerApi(data)
    if (res.success) {
      return res
    }
    return res
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    if (res.success) {
      userInfo.value = res.data
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
