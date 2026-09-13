import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题：面包屑自动消费 */
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 登录（空白布局）：密码 → MFA 两阶段
    {
      path: '/login',
      component: () => import('../layouts/BlankLayout.vue'),
      children: [
        {
          path: '',
          name: 'login',
          component: () => import('../features/auth/views/LoginView.vue'),
          meta: { title: '登录' },
        },
        {
          path: 'mfa',
          name: 'mfa',
          component: () => import('../features/auth/views/MfaView.vue'),
          meta: { title: '两步验证' },
        },
      ],
    },
    // 主框架（侧边栏 + Header + Footer）
    {
      path: '/',
      component: () => import('../layouts/BasicLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../features/dashboard/views/DashboardView.vue'),
          meta: { title: '总览' },
        },
        {
          path: 'assets/inventory',
          name: 'assets-inventory',
          component: () => import('../features/assets/views/AssetsView.vue'),
          meta: { title: '资产列表' },
        },
        {
          path: 'assets/accounts',
          name: 'assets-accounts',
          component: () => import('../features/assets/views/AccountsView.vue'),
          meta: { title: '账号' },
        },
        {
          path: 'assets/credentials',
          name: 'assets-credentials',
          component: () => import('../features/assets/views/CredentialsView.vue'),
          meta: { title: '凭据' },
        },
        {
          path: 'terminal',
          name: 'terminal',
          component: () => import('../features/terminal/views/TerminalView.vue'),
          meta: { title: 'Web 终端' },
        },
        {
          path: 'access/policies',
          name: 'access-policies',
          component: () => import('../features/access/views/AccessPoliciesView.vue'),
          meta: { title: '授权策略' },
        },
        {
          path: 'sessions/online',
          name: 'sessions-online',
          component: () => import('../features/sessions/views/OnlineSessionsView.vue'),
          meta: { title: '在线会话' },
        },
        {
          path: 'sessions/recordings',
          name: 'sessions-recordings',
          component: () => import('../features/sessions/views/SessionRecordingsView.vue'),
          meta: { title: '录像回放' },
        },
        {
          path: 'sessions/web-access',
          name: 'sessions-web-access',
          component: () => import('../features/sessions/views/WebAccessView.vue'),
          meta: { title: 'Web 访问' },
        },
        {
          path: 'audit/operations',
          name: 'audit-operations',
          component: () => import('../features/audit/views/OperationLogsView.vue'),
          meta: { title: '操作日志' },
        },
        {
          path: 'audit/logins',
          name: 'audit-logins',
          component: () => import('../features/audit/views/LoginLogsView.vue'),
          meta: { title: '登录日志' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('../features/settings/views/SettingsView.vue'),
          meta: { title: '系统设置' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../features/auth/views/ProfileView.vue'),
          meta: { title: '个人中心' },
        },
      ],
    },
  ],
})

// 路由守卫：会话恢复 / 登录态 / MFA 待验证态 / 回跳地址
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 首次导航前恢复登录态：有 token 调 /me 校验（过期由 http 层自动续期，失败清态）
  if (!auth.restored) {
    await auth.restore()
  }

  // 已登录用户访问登录页 → 直达总览
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'dashboard' }
  }
  // MFA 页必须处于「密码已通过、待验证码」状态（防跳过密码直接打码）
  if (to.name === 'mfa' && !auth.isPendingMfa) {
    return { name: 'login' }
  }
  // 未登录访问业务页 → 登录页（携带回跳地址）
  if (to.name !== 'login' && to.name !== 'mfa' && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
