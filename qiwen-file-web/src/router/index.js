import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('_v/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('_v/Register.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('_v/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('_v/Home.vue'), meta: { title: '首页' } },
      { path: 'file', name: 'File', component: () => import('_v/FileManager.vue'), meta: { title: '文件管理' } },
      { path: 'share', name: 'Share', component: () => import('_v/Share.vue'), meta: { title: '文件分享' } },
      { path: 'admin', name: 'Admin', component: () => import('_v/Admin.vue'), meta: { title: '用户管理', admin: true } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('_v/Dashboard.vue'), meta: { title: '管理面板', admin: true } },
      { path: 'recycle', name: 'Recycle', component: () => import('_v/RecycleBin.vue'), meta: { title: '回收站' } },
      { path: 'logs', name: 'OperationLog', component: () => import('_v/OperationLog.vue'), meta: { title: '操作日志', admin: true } }
    ]
  },
  { path: '/search', name: 'Search', component: () => import('_v/SearchResult.vue'), meta: { title: '搜索结果' } },
  { path: '/onlyoffice/:fileId', name: 'OnlyOffice', component: () => import('_v/OnlyOfficeEditor.vue'), meta: { title: '文档编辑' } }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
import { setupGuards } from './guards'
setupGuards(router)
export default router
