import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import ArcoVueIcon from '@arco-design/web-vue/es/icon'
import '@arco-design/web-vue/dist/arco.css'
// 设计语言层：先 tokens（变量定义）再 reset（基础样式），顺序不可颠倒
import './design/tokens.css'
import './design/reset.css'
import App from './App.vue'
import router from './router'
import { bindAuth } from './api/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(createPinia()).use(router).use(ArcoVue).use(ArcoVueIcon)

// 认证绑定：http 层 401 自动续期与登录态失效处理
const auth = useAuthStore()
bindAuth({
  getAccessToken: () => auth.accessToken,
  refreshSession: () => auth.refresh(),
  onSessionExpired: () => {
    auth.clearSession()
    if (router.currentRoute.value.name !== 'login') {
      router.replace({ name: 'login' })
    }
  },
})

app.mount('#app')
