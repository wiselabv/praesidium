import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'terminal',
      component: () => import('../features/terminal/views/TerminalView.vue'),
    },
  ],
})

export default router
