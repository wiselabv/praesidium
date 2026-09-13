/**
 * 第三方登录绑定接口（identity 上下文，对齐 ThirdPartyController）：查看 / 绑定 / 解绑。
 */
import { request } from './http'

/** 第三方绑定项（对应 ThirdPartyItem） */
export interface ThirdPartyItem {
  provider: string
  providerUserId: string
  nickname: string | null
  avatarUrl: string | null
  boundAt: string
}

/** 当前用户的第三方绑定清单 */
export function listThirdParty(): Promise<ThirdPartyItem[]> {
  return request<ThirdPartyItem[]>('/api/profile/third-party')
}

/** 绑定第三方账号（演示：直接完成绑定） */
export function bindThirdParty(provider: string): Promise<ThirdPartyItem> {
  return request<ThirdPartyItem>('/api/profile/third-party', {
    method: 'POST',
    body: JSON.stringify({ provider }),
  })
}

/** 解绑第三方账号 */
export function unbindThirdParty(provider: string): Promise<void> {
  return request<void>(`/api/profile/third-party/${provider}`, { method: 'DELETE' })
}
