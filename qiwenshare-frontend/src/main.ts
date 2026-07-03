import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// iconfont 图标字体（音频控制等场景）
import './assets/styles/iconfont/iconfont.css'

// 全局样式（顺序：reset → element-override → responsive）
// variables.scss 通过 vite.config.ts additionalData 全局注入
import './assets/styles/reset.scss'
import './assets/styles/element-override.scss'
import './assets/styles/responsive.scss'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
