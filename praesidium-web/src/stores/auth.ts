import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchMe,
  loginByCode,
  loginByDomain,
  loginByRecovery,
  loginMfa,
  loginPassword,
  logoutRemote,
  refreshSessionToken,
  ssoCallback,
} from '../api/auth'
import type { TokenPair } from '../api/auth'
import { ApiError } from '../api/http'

/** token 持久化键（Java 管理服务签发） */
const TOKEN_KEY = 'praesidium.token'
/** 刷新令牌持久化键：与 token 同步存取，保证刷新后状态一致 */
const REFRESH_KEY = 'praesidium.refresh'
/** 用户信息持久化键：与 token 同步存取，保证刷新后状态一致 */
const USER_KEY = 'praesidium.user'

/** 当前登录用户（对齐后端 MeResponse） */
export interface AuthUser {
  id: number
  username: string
  displayName: string | null
  email: string | null
  mfaEnabled: boolean
}

/** MFA 验证方式 */
export type MfaMethod = 'totp' | 'sms' | 'email' | 'recovery'
/** SSO 身份提供方（OIDC 覆盖 GitHub/企业 IdP；SAML/CAS 为企业级单点；钉钉/飞书/企微为国内扫码） */
export type SsoProvider = 'github' | 'oidc' | 'saml' | 'cas' | 'dingtalk' | 'feishu' | 'wecom'
/** 主认证源：本地账号 / 外部目录（LDAP/AD 域账号） */
export type AuthSource = 'local' | 'ldap'
/** 主认证凭据类型：用户名 / 手机号 / 邮箱（密码与验证码登录共用） */
export type AuthIdentity = 'username' | 'phone' | 'email'

/**
 * 识别登录账号类型：11 位手机号 / 含 @ 视为邮箱 / 其他视为用户名。
 * 登录框不做类型选择，由内容自动识别（真实后端同样按此规则路由认证）。
 */
export function detectIdentity(account: string): AuthIdentity {
  const value = account.trim()
  if (/^1\d{10}$/.test(value)) return 'phone'
  if (value.includes('@')) return 'email'
  return 'username'
}

/** 用户展示名：优先显示名，回退用户名 */
export function displayNameOf(user: AuthUser | null): string {
  return user ? (user.displayName ?? user.username) : '未登录'
}

/** 从 localStorage 恢复用户信息（损坏数据降级为 null） */
function loadStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

/** MFA 待验证态（密码通过后、MFA 完成前，仅内存暂存，刷新即失效防绕过） */
interface PendingMfa {
  sessionToken: string
  username: string
  remember: boolean
}

/**
 * 认证状态（多方式登录）。
 *
 * 令牌由 Java 管理服务签发（登录 / MFA），前端连接 Rust 终端网关时携带；
 * Rust 侧负责验签与授权判定 —— 认证中心始终是 Java，此处仅保存客户端状态。
 *
 * 登录路径：
 *   1. 密码登录：login → 后端返回 next=done（未启用 MFA，直接完成）
 *      或 next=mfa（暂存 sessionToken 进 MFA 页，TOTP 验证后完成）；
 *   2. SSO / 验证码登录：后端未实现，前端入口保留并提示即将上线。
 *
 * 会话恢复：restore() 有 token 时调 /me 校验（http 层自动续期），
 * 失效清态；refresh() 供 http 层 401 自动续期调用。
 */
export const useAuthStore = defineStore('auth', () => {
  // 记住我 → localStorage（长期）；不记住 → sessionStorage（关浏览器即失效）
  const accessToken = ref(
    localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY) ?? '',
  )
  const refreshTokenValue = ref(
    localStorage.getItem(REFRESH_KEY) ?? sessionStorage.getItem(REFRESH_KEY) ?? '',
  )
  const user = ref<AuthUser | null>(loadStoredUser())
  /** token 持久化位置（记住我与否），续期时沿用 */
  const persistent = ref(localStorage.getItem(TOKEN_KEY) !== null)
  const pending = ref<PendingMfa | null>(null)
  /** restore 是否已执行（路由守卫只触发一次） */
  const restored = ref(false)

  const isLoggedIn = computed(() => accessToken.value !== '' && user.value !== null)
  const isPendingMfa = computed(() => pending.value !== null)
  /** MFA 页面显示的待验证账号名 */
  const pendingName = computed(() => pending.value?.username ?? '')

  function persist(token: string, refresh: string, remember: boolean) {
    if (remember) {
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(REFRESH_KEY, refresh)
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(REFRESH_KEY)
    } else {
      sessionStorage.setItem(TOKEN_KEY, token)
      sessionStorage.setItem(REFRESH_KEY, refresh)
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_KEY)
    }
  }

  /** 签发令牌后写入会话态（登录/MFA/续期的公共收尾） */
  function setSession(pair: TokenPair, remember: boolean) {
    accessToken.value = pair.accessToken
    refreshTokenValue.value = pair.refreshToken
    user.value = pair.user
    persistent.value = remember
    persist(pair.accessToken, pair.refreshToken, remember)
    localStorage.setItem(USER_KEY, JSON.stringify(pair.user))
  }

  /** 清空本地登录态（不动后端吊销，供 401 失效/登出等场景） */
  function clearSession() {
    accessToken.value = ''
    refreshTokenValue.value = ''
    user.value = null
    pending.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
  }

  /**
   * 阶段一：密码登录。
   * 后端按账号 mfaEnabled 决定后续：next=done 直接完成登录，next=mfa 进入二次验证。
   *
   * @returns 'done' 已登录；'mfa' 需进入 MFA 页
   */
  async function login(account: string, password: string, remember: boolean): Promise<'done' | 'mfa'> {
    const result = await loginPassword(account, password)
    if (result.next === 'done') {
      if (!result.tokens) throw new Error('服务端响应异常，请稍后重试')
      setSession(result.tokens, remember)
      return 'done'
    }
    if (!result.sessionToken) throw new Error('服务端响应异常，请稍后重试')
    pending.value = { sessionToken: result.sessionToken, username: account, remember }
    return 'mfa'
  }

  /** 阶段二：TOTP 二次验证，通过后完成登录；会话过期（1003）清待验证态 */
  async function verifyMfa(code: string): Promise<void> {
    const current = pending.value
    if (!current) throw new Error('登录状态已失效，请重新登录')
    try {
      const pair = await loginMfa(current.sessionToken, code)
      setSession(pair, current.remember)
      pending.value = null
    } catch (error) {
      if (error instanceof ApiError && error.code === 1003) {
        pending.value = null
      }
      throw error
    }
  }

  /** 验证码登录（演示验证码固定 123456）：直接完成登录 */
  async function loginCode(account: string, code: string, remember: boolean): Promise<void> {
    const pair = await loginByCode(account, code)
    setSession(pair, remember)
  }

  /** 域账号（LDAP/AD）登录：直接完成登录 */
  async function loginDomain(account: string, password: string, remember: boolean): Promise<void> {
    const pair = await loginByDomain(account, password)
    setSession(pair, remember)
  }

  /** 恢复码登录（MFA 设备不可用时的一次性登录） */
  async function loginRecovery(account: string, recoveryCode: string, remember: boolean): Promise<void> {
    const pair = await loginByRecovery(account, recoveryCode)
    setSession(pair, remember)
  }

  /** SSO 登录（演示）：回调按第三方绑定定位用户并直接完成登录 */
  async function loginSso(provider: string, remember: boolean): Promise<void> {
    const pair = await ssoCallback(provider)
    setSession(pair, remember)
  }

  /**
   * 会话恢复：有 token 时调 /me 校验有效性并刷新用户信息。
   * access token 过期由 http 层自动续期；续期失败（401）清空本地态。
   * 网络异常时保留本地态（信任本地），由后续请求继续暴露。
   */
  async function restore(): Promise<boolean> {
    if (restored.value) return isLoggedIn.value
    restored.value = true
    if (!accessToken.value) return false
    try {
      user.value = await fetchMe()
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      return true
    } catch (error) {
      if (error instanceof ApiError && error.code === 401) {
        clearSession()
      }
      return isLoggedIn.value
    }
  }

  /** 用刷新令牌换新令牌对（供 http 层 401 自动续期调用，失败返回 false） */
  async function refresh(): Promise<boolean> {
    const current = refreshTokenValue.value
    if (!current) return false
    try {
      const pair = await refreshSessionToken(current)
      setSession(pair, persistent.value)
      return true
    } catch {
      return false
    }
  }

  /** 退出登录：先清本地态（即时反馈），再异步吊销后端刷新令牌（幂等，失败不阻塞） */
  async function logout(): Promise<void> {
    const current = refreshTokenValue.value
    const access = accessToken.value
    clearSession()
    if (current && access) {
      try {
        await logoutRemote(current)
      } catch {
        // 吊销失败忽略：本地态已清，刷新令牌有效期过后自然失效
      }
    }
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    isPendingMfa,
    pendingName,
    restored,
    restore,
    login,
    loginCode,
    loginDomain,
    loginRecovery,
    loginSso,
    verifyMfa,
    refresh,
    logout,
    clearSession,
  }
})
