/**
 * SSO 提供商配置接口（identity 上下文，对齐 SsoProviderController）：
 * 配置 CRUD + 授权发起 / 回调（系统设置-认证分组与登录页使用）。
 */
import { request } from './http'

/** SSO 提供商配置条目（对应 SsoProviderItem，clientSecret 只回显是否配置） */
export interface SsoProviderItem {
  id: number
  provider: string
  displayName: string
  enabled: boolean
  clientId: string | null
  secretConfigured: boolean
  authorizeUrl: string | null
  tokenUrl: string | null
  userinfoUrl: string | null
  scopes: string | null
  extraConfig: string | null
  updatedAt: string
}

/** 保存 SSO 提供商配置请求（对应 SsoProviderSaveRequest，clientSecret 为空表示保留原值） */
export interface SsoProviderSaveRequest {
  provider: string
  displayName: string
  enabled: boolean
  clientId?: string | null
  clientSecret?: string | null
  authorizeUrl?: string | null
  tokenUrl?: string | null
  userinfoUrl?: string | null
  scopes?: string | null
  extraConfig?: string | null
}

/** 授权发起响应（对应 SsoAuthorizeResponse，演示授权 URL） */
export interface SsoAuthorize {
  provider: string
  displayName: string
  authorizeUrl: string
  state: string
}

/** SSO 提供商清单 */
export function listSsoProviders(): Promise<SsoProviderItem[]> {
  return request<SsoProviderItem[]>('/api/sso/providers')
}

/** 单个 SSO 提供商 */
export function getSsoProvider(provider: string): Promise<SsoProviderItem> {
  return request<SsoProviderItem>(`/api/sso/providers/${provider}`)
}

/** 保存（新建或更新）SSO 提供商 */
export function saveSsoProvider(payload: SsoProviderSaveRequest): Promise<SsoProviderItem> {
  return request<SsoProviderItem>('/api/sso/providers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** 删除 SSO 提供商 */
export function deleteSsoProvider(provider: string): Promise<void> {
  return request<void>(`/api/sso/providers/${provider}`, { method: 'DELETE' })
}

/** 授权发起：返回演示授权 URL 供跳转 */
export function ssoAuthorize(provider: string): Promise<SsoAuthorize> {
  return request<SsoAuthorize>(`/api/auth/sso/${provider}/authorize`)
}
