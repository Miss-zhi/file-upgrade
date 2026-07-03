/// <reference types="vite/client" />

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    hideHeader?: boolean
    hideFooter?: boolean
    /** 所需权限码（admin 子路由使用） */
    permission?: string
  }
}
