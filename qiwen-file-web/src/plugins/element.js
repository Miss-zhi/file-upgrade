/**
 * Element Plus 插件
 * 按需引入由 unplugin-vue-components + unplugin-auto-import 在 vite.config.ts 中处理
 * 此文件仅用于全局配置 Element Plus 中文语言包等初始化操作
 */

import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

export function setupElementPlus(app) {
  app.config.globalProperties.$ELEMENT = { size: 'default', zIndex: 3000 }
}

export { zhCn }
