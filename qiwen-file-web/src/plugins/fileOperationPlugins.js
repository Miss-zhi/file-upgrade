/**
 * 命令式弹窗 / 浮层服务
 * Vue 3 使用 createApp 替代 Vue 2 的 Vue.extend
 */
import { createApp } from 'vue'

/**
 * 打开弹窗组件
 * @param {Component} component - 弹窗组件
 * @param {Object} props - 组件 props
 * @returns {Promise} resolve 时返回弹窗结果
 */
function openDialog(component, props = {}) {
  return new Promise((resolve, reject) => {
    const container = document.createElement('div')
    document.body.appendChild(container)

    const app = createApp(component, {
      ...props,
      onClose: (result) => {
        app.unmount()
        document.body.removeChild(container)
        result instanceof Error ? reject(result) : resolve(result)
      }
    })

    // 注入全局依赖（pinia、router、element-plus 等由 main.ts 全局注册，这里通过 provide 传递）
    app.mount(container)
  })
}

/**
 * 打开浮层组件
 * @param {Component} component - 浮层组件
 * @param {Object} props - 组件 props
 * @returns {Promise}
 */
function openBox(component, props = {}) {
  return openDialog(component, { ...props, mode: 'box' })
}

/**
 * 注册全局命令式弹窗方法
 */
export function setupFileOperationPlugins(app) {
  app.config.globalProperties.$openDialog = {
    open: openDialog
  }
  app.config.globalProperties.$openBox = {
    open: openBox
  }
}

export { openDialog, openBox }
