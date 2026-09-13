/**
 * 认证接口（identity 上下文，对齐 Java 管理服务 AuthController 的 DTO）。
 *
 * 统一响应信封在 http 层解包：调用方只拿到 data，业务失败抛 ApiError。
 */
import { request } from './http'

/** 当前用户（对应 MeResponse） */
export interface MeUser {
  id: number
  username: string
  displayName: string | null
  email: string | null
  mfaEnabled: boolean
}

/** 令牌对（对应 TokenResponse） */
export interface TokenPair {
  accessToken: string
  refreshToken: string
  /** 访问令牌有效期（秒），供前端计算续期时机 */
  expiresIn: number
  user: MeUser
}

/** 密码登录结果（对应 LoginPasswordResponse）：next=mfa 需二次验证；next=done 直接完成 */
export interface LoginPasswordResult {
  next: 'mfa' | 'done'
  /** 仅 next=mfa 时下发 */
  sessionToken: string | null
  /** 仅 next=done（未启用 MFA）时下发 */
  tokens: TokenPair | null
}

/** 阶段一：密码登录 */
export function loginPassword(username: string, password: string): Promise<LoginPasswordResult> {
  return request<LoginPasswordResult>('/api/auth/login/password', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

/** 阶段二：MFA（TOTP 6 位动态码）验证 */
export function loginMfa(sessionToken: string, code: string): Promise<TokenPair> {
  return request<TokenPair>('/api/auth/login/mfa', {
    method: 'POST',
    body: JSON.stringify({ sessionToken, code }),
  })
}

/** 刷新令牌对（旧刷新令牌轮换吊销，403 无权限语义由后端 1004 业务码表达） */
export function refreshSessionToken(refreshToken: string): Promise<TokenPair> {
  return request<TokenPair>(
    '/api/auth/refresh',
    {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    },
    // 不带 Authorization 头：本地 access token 可能已失效，带坏令牌会被安全层提前拦截
    { skipAuthHeader: true, skipAuthRetry: true },
  )
}

/** 登出：吊销刷新令牌（后端幂等） */
export function logoutRemote(refreshToken: string): Promise<void> {
  return request<void>('/api/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

/** 当前用户信息（Bearer token 由 http 层注入） */
export function fetchMe(): Promise<MeUser> {
  return request<MeUser>('/api/auth/me')
}

/** 验证码登录（短信/邮件验证码，演示固定 123456），直接签发令牌对 */
export function loginByCode(username: string, code: string): Promise<TokenPair> {
  return request<TokenPair>('/api/auth/login/code', {
    method: 'POST',
    body: JSON.stringify({ username, code }),
  })
}

/** 域账号（LDAP/AD）登录：username 形如 PRAESIDIUM\admin 或 admin@domain */
export function loginByDomain(username: string, password: string): Promise<TokenPair> {
  return request<TokenPair>('/api/auth/login/domain', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

/** 恢复码登录（MFA 设备不可用时的一次性登录） */
export function loginByRecovery(username: string, recoveryCode: string): Promise<TokenPair> {
  return request<TokenPair>('/api/auth/login/recover', {
    method: 'POST',
    body: JSON.stringify({ username, recoveryCode }),
  })
}

/** SSO 回调（演示）：按第三方绑定定位用户并签发令牌对 */
export function ssoCallback(provider: string): Promise<TokenPair> {
  return request<TokenPair>(`/api/auth/sso/${provider}/callback`)
}
