import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/file',
      name: 'file',
      component: () => import('../views/FileView.vue'),
      meta: { requiresAuth: true, hideFooter: true },
    },
    {
      path: '/share/:shareBatchNum',
      name: 'share',
      component: () => import('../views/ShareView.vue'),
      meta: { hideFooter: true },
    },
    {
      path: '/notice',
      name: 'notice',
      component: () => import('../views/notice/NoticeListView.vue'),
    },
    {
      path: '/notice/:noticeId',
      name: 'noticeDetail',
      component: () => import('../views/notice/NoticeDetailView.vue'),
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      redirect: { name: 'adminUsers' },
      children: [
        {
          path: 'users',
          name: 'adminUsers',
          component: () => import('../views/admin/AdminUserList.vue'),
          meta: { permission: 'admin:user-manage' },
        },
        {
          path: 'roles',
          name: 'adminRoles',
          component: () => import('../views/admin/AdminRoleList.vue'),
          meta: { permission: 'admin:role-manage' },
        },
        {
          path: 'quota',
          name: 'adminQuota',
          component: () => import('../views/admin/AdminQuota.vue'),
          meta: { permission: 'admin:quota-manage' },
        },
        {
          path: 'logs',
          name: 'adminLogs',
          component: () => import('../views/admin/AdminAuditLog.vue'),
          meta: { permission: 'admin:log-view' },
        },
        {
          path: 'config',
          name: 'adminConfig',
          component: () => import('../views/admin/AdminSystemConfig.vue'),
          meta: { permission: 'admin:config-manage' },
        },
      ],
    },
    {
      path: '/preview/office',
      name: 'officePreview',
      component: () => import('../views/OfficePreview.vue'),
      meta: { requiresAuth: true, hideHeader: true, hideFooter: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'error404',
      component: () => import('../views/ErrorPage.vue'),
      meta: { hideHeader: true, hideFooter: true },
    },
  ],
})

/**
 * 全局前置守卫：检查认证状态。
 *
 * - 首次加载时尝试从服务端恢复用户信息
 * - 需要认证的页面：未登录则跳转 /login
 * - 已登录用户访问 /login 或 /register：重定向到首页
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // 尝试从服务端恢复用户信息（首次加载时）
  if (!authStore.isLoggedIn && to.meta.requiresAuth !== false) {
    try {
      await authStore.fetchMe()
    } catch {
      // 未登录或 token 无效
    }
  }

  // 需要认证但未登录
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 已登录用户访问登录页或注册页
  if ((to.name === 'login' || to.name === 'register') && authStore.isLoggedIn) {
    return { name: 'home' }
  }

  // admin 子路由权限校验
  if (to.meta.permission && !authStore.hasPermission(to.meta.permission as string)) {
    return { name: 'error404' }
  }
})

export default router
