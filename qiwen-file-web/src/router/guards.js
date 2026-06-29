import { TOKEN_KEY } from '_api/constants'

const LOGIN_PATH = '/login'

/**
 * 注册路由守卫
 * 使用函数参数注入 router，避免循环依赖（guards.js ←→ router/index.js）
 */
export function setupGuards(router) {
  router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem(TOKEN_KEY)
    const { noAuth } = to.meta

    // 已登录用户访问登录/注册页 → 重定向到首页
    if (token && noAuth) {
      return next('/home')
    }

    // 管理员路由检查
    if (to.meta.admin) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      if (userInfo.role !== 'ADMIN') {
        return next('/home')
      }
    }

    // 未登录访问需要认证的页面 → 跳转登录
    if (!token && !noAuth) {
      return next({ path: LOGIN_PATH, query: { redirect: to.fullPath } })
    }

    next()
  })

  router.afterEach((to) => {
    document.title = to.meta.title ? `${to.meta.title} - 奇文网盘` : '奇文网盘'
  })
}
