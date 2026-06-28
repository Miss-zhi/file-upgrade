/// <reference types="vite/client" />

// Vue SFC 类型声明
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

// Element Plus 中文语言包
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: any
  export default zhCn
}

// JS 模块（allowJs 已开启，此声明为兜底）
declare module '_api/*' {
  const content: unknown
  export = content
}

declare module '@/*' {
  const content: unknown
  export = content
}
