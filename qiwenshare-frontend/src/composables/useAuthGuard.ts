import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由守卫 composable。
 *
 * <p>检查登录状态，未登录则跳转 /login。</p>
 */
export function useAuthGuard() {
  const router = useRouter()
  const authStore = useAuthStore()
  const isAuthenticated = computed(() => authStore.isLoggedIn)

  /**
   * 检查是否需要登录。如果未登录，跳转到登录页并记录目标路径。
   *
   * @returns 是否已认证
   */
  function checkAuth(): boolean {
    if (!isAuthenticated.value) {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return false
    }
    return true
  }

  return {
    isAuthenticated,
    checkAuth,
  }
}
