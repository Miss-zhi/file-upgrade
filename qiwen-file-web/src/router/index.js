import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('_v/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('_v/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('_v/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'file',
        name: 'File',
        component: () => import('_v/File.vue'),
        meta: { title: '文件管理' }
      },
      {
        path: 'share',
        name: 'Share',
        component: () => import('_v/Share.vue'),
        meta: { title: '文件分享' }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('_v/Admin.vue'),
        meta: { title: '管理后台', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 注册全局导航守卫
import './guards'

export default router
