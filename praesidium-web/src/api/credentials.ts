/**
 * 凭据接口（asset 上下文，对齐 CredentialController）：CRUD + 单次明文查看。
 */
import { request } from './http'
import type { PageData } from './common'

/** 凭据列表项（对应 CredentialItem） */
export interface CredentialItem {
  id: number
  name: string
  type: string
  secretMasked: string
  boundAccounts: number
  createdAt: string
  updatedAt: string
}

/** 新建 / 修改凭据请求（对应 CredentialRequest，修改时 secret 可省略表示不轮换） */
export interface CredentialRequest {
  name: string
  type: string
  secret?: string
}

/** 明文查看响应（对应 SecretRevealResponse，展示后即弃） */
export interface SecretReveal {
  secret: string
}

/** 分页查询凭据 */
export function listCredentials(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<PageData<CredentialItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  return request<PageData<CredentialItem>>(`/api/credentials?${query.toString()}`)
}

/** 新建凭据 */
export function createCredential(payload: CredentialRequest): Promise<CredentialItem> {
  return request<CredentialItem>('/api/credentials', { method: 'POST', body: JSON.stringify(payload) })
}

/** 修改凭据 */
export function updateCredential(id: number, payload: CredentialRequest): Promise<CredentialItem> {
  return request<CredentialItem>(`/api/credentials/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/** 删除凭据 */
export function deleteCredential(id: number): Promise<void> {
  return request<void>(`/api/credentials/${id}`, { method: 'DELETE' })
}

/** 单次明文查看 */
export function revealCredential(id: number): Promise<SecretReveal> {
  return request<SecretReveal>(`/api/credentials/${id}/secret`)
}
