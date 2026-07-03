import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, fetchMe as fetchMeApi } from '@/api/auth'
import type { LoginParams, UserInfo } from '@/api/auth'

/**
 * Task 12.1: 清理旧 localStorage token 存储。
 * 移除所有 auth 相关的 localStorage key，确保无残留。
 */
function cleanLegacyTokenStorage(): void {
  const keysToRemove: string[] = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && (key.includes('token') || key.includes('refreshToken') || key.includes('auth'))) {
      keysToRemove.push(key)
    }
  }
  keysToRemove.forEach((key) => localStorage.removeItem(key))
}

/**
 * 认证状态管理（Pinia Composition API 风格）。
 */
export const useAuthStore = defineStore('auth', () => {
  // 清理旧版 localStorage token（Task 12.1）
  cleanLegacyTokenStorage()

  const user = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => user.value !== null)
  const roles = computed(() => user.value?.roles ?? [])
  const permissions = computed(() => user.value?.permissions ?? [])

  /**
   * 用户登录。
   */
  async function login(params: LoginParams): Promise<void> {
    const result = await loginApi(params)
    user.value = {
      userId: result.userId,
      username: result.username,
      telephone: '',
      avatar: null,
      roles: result.roles,
      permissions: result.permissions,
      registerTime: '',
    }
  }

  /**
   * 用户登出。
   */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      user.value = null
    }
  }

  /**
   * 获取当前用户信息（从服务端刷新）。
   */
  async function fetchMe(): Promise<void> {
    try {
      user.value = await fetchMeApi()
    } catch {
      user.value = null
      throw new Error('获取用户信息失败')
    }
  }

  /**
   * 检查是否有指定权限。
   */
  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm)
  }

  /**
   * 检查是否有指定角色。
   */
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    user,
    isLoggedIn,
    roles,
    permissions,
    login,
    logout,
    fetchMe,
    hasPermission,
    hasRole,
  }
})
