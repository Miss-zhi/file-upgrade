import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import config from '@/config'

const TOKEN_KEY = 'qiwen-token'

// 创建 Axios 实例
const http = axios.create({
  baseURL: config.baseURL,
  timeout: config.timeout,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== 请求拦截器 ====================
http.interceptors.request.use(
  (reqConfig) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      reqConfig.headers.Authorization = `Bearer ${token}`
    }
    return reqConfig
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================
http.interceptors.response.use(
  (response) => {
    const res = response.data
    // 兼容 RestResult 格式
    if (typeof res === 'object' && res !== null && 'success' in res) {
      if (!res.success) {
        ElMessage.error(res.message || '请求失败')
      }
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    let message = '网络错误'

    switch (status) {
      case 400:
        message = '请求参数错误'
        break
      case 401:
        message = '登录已过期，请重新登录'
        localStorage.removeItem(TOKEN_KEY)
        router.push('/login')
        break
      case 403:
        message = '没有访问权限'
        break
      case 404:
        message = '请求资源不存在'
        break
      case 500:
        message = '服务器内部错误'
        break
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
export { TOKEN_KEY }
