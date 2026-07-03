import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import type { RestResult } from '@/types/api'
import router from '@/router'

/** 刷新锁：防止并发刷新 */
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []
/** 防止重复跳转登录页 */
let isRedirectingToLogin = false

/**
 * 创建 Axios 实例。
 *
 * - withCredentials: true（携带 httpOnly cookie）
 * - 响应拦截器处理 401 自动 refresh
 */
const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 响应拦截器：处理 401 自动刷新 token。
 */
client.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    const status = error.response?.status

    // 401 且非刷新/登录请求，且未重试过
    if (
      status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/login')
    ) {
      if (isRefreshing) {
        // 已在刷新中，排队等待
        return new Promise((resolve) => {
          pendingRequests.push((token: string) => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`
            resolve(client(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 调用 refresh 端点（cookie 会自动携带）
        const { data } = await axios.post<RestResult<null>>(
          `${client.defaults.baseURL}/auth/refresh`,
          null,
          { withCredentials: true },
        )

        if (data.code === 0) {
          // 刷新成功，重试所有排队请求
          pendingRequests.forEach((cb) => cb(''))
          pendingRequests = []
          return client(originalRequest)
        }
      } catch {
        // 刷新失败，清空排队请求
        pendingRequests = []
      } finally {
        isRefreshing = false
      }

      // 刷新失败，跳转到登录页（软跳转，避免硬刷新死循环）
      if (!isRedirectingToLogin && window.location.pathname !== '/login') {
        isRedirectingToLogin = true
        router.push({ name: 'login', query: { redirect: window.location.pathname } }).finally(() => {
          isRedirectingToLogin = false
        })
      }
      return Promise.reject(error)
    }

    return Promise.reject(error)
  },
)

export default client
