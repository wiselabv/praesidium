import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface AuthUser {
  id: string
  name: string
  roles: string[]
}

/**
 * 认证状态。
 *
 * 令牌由 Java 管理服务签发（登录 / SSO / MFA），前端连接 Rust 终端网关时携带；
 * Rust 侧负责验签与授权判定 —— 认证中心始终是 Java，此处仅保存客户端状态。
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref('')
  const user = ref<AuthUser | null>(null)

  function setToken(token: string) {
    accessToken.value = token
  }

  function setUser(next: AuthUser | null) {
    user.value = next
  }

  function logout() {
    accessToken.value = ''
    user.value = null
  }

  return { accessToken, user, setToken, setUser, logout }
})
