import { createApp } from 'vue'
import { createPinia } from 'pinia'
import persist from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import datav from '@iamzzg/data-view/dist/vue3/datav.map.vue.esm'
import '@/assets/main.css'

const app = createApp(App)

// 全局注册ElementPlus图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia()

// 使用Pinia插件实现状态持久化
pinia.use(persist)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(datav)

app.mount('#app')
