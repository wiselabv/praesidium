/**
 * 安全设置接口（identity 上下文，对齐 SecurityController）：
 * TOTP 绑定/解绑、恢复码、WebAuthn、短信/邮件验证码。
 */
import { request } from './http'

/** 安全设置摘要（对应 SecuritySummaryResponse） */
export interface SecuritySummary {
  mfaEnabled: boolean
  smsPhone: string | null
  smsEnabled: boolean
  emailMfaEnabled: boolean
  webauthnCount: number
  recoveryCodesLeft: number
}

/** TOTP 绑定阶段一响应（对应 MfaSetupResponse） */
export interface MfaSetup {
  secret: string
  otpAuthUri: string
}

/** 恢复码生成响应（对应 RecoveryCodesResponse，明文仅此一次下发） */
export interface RecoveryCodes {
  codes: string[]
  remaining: number
}

/** WebAuthn 设备项（对应 WebauthnItem） */
export interface WebauthnItem {
  id: number
  name: string
  credentialId: string
  signCount: number
  addedAt: string
}

/** 安全设置摘要 */
export function fetchSecuritySummary(): Promise<SecuritySummary> {
  return request<SecuritySummary>('/api/profile/security/summary')
}

/** TOTP 绑定阶段一：生成密钥与 otpauth URI */
export function mfaSetup(): Promise<MfaSetup> {
  return request<MfaSetup>('/api/profile/security/mfa/setup', { method: 'POST' })
}

/** TOTP 绑定阶段二：校验动态码后启用 */
export function mfaBind(secret: string, code: string): Promise<void> {
  return request<void>('/api/profile/security/mfa/bind', {
    method: 'POST',
    body: JSON.stringify({ secret, code }),
  })
}

/** 停用 TOTP */
export function mfaDisable(): Promise<void> {
  return request<void>('/api/profile/security/mfa/disable', { method: 'POST' })
}

/** 重新生成恢复码（明文仅此一次下发） */
export function generateRecoveryCodes(): Promise<RecoveryCodes> {
  return request<RecoveryCodes>('/api/profile/security/recovery-codes', { method: 'POST' })
}

/** WebAuthn 设备列表 */
export function listWebauthn(): Promise<WebauthnItem[]> {
  return request<WebauthnItem[]>('/api/profile/security/webauthn')
}

/** 注册 WebAuthn 设备（演示：后端生成凭据元数据） */
export function registerWebauthn(name: string): Promise<WebauthnItem> {
  return request<WebauthnItem>('/api/profile/security/webauthn', {
    method: 'POST',
    body: JSON.stringify({ name }),
  })
}

/** 删除 WebAuthn 设备 */
export function deleteWebauthn(id: number): Promise<void> {
  return request<void>(`/api/profile/security/webauthn/${id}`, { method: 'DELETE' })
}

/** 绑定短信验证手机号（演示验证码固定 123456） */
export function bindSms(phone: string, code: string): Promise<void> {
  return request<void>('/api/profile/security/sms/bind', {
    method: 'POST',
    body: JSON.stringify({ phone, code }),
  })
}

/** 解绑短信验证 */
export function unbindSms(): Promise<void> {
  return request<void>('/api/profile/security/sms/unbind', { method: 'POST' })
}

/** 启用/停用邮件验证码 */
export function toggleEmailMfa(enabled: boolean): Promise<void> {
  return request<void>('/api/profile/security/email-mfa', {
    method: 'PUT',
    body: JSON.stringify({ enabled }),
  })
}

/** 发送验证码（演示：固定 123456，打印到服务端日志） */
export function sendCode(channel: string, target: string): Promise<void> {
  return request<void>('/api/profile/security/code/send', {
    method: 'POST',
    body: JSON.stringify({ channel, target }),
  })
}
