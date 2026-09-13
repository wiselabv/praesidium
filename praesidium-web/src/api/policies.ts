/**
 * 授权策略接口（access 上下文，对齐 AccessPolicyController）：CRUD + 审批 / 撤销。
 */
import { request } from './http'
import type { PageData } from './common'

/** 授权策略列表项（对应 PolicyItem） */
export interface PolicyItem {
  id: number
  name: string
  userId: number
  userName: string
  assetIds: number[]
  assetNames: string
  accountIds: number[]
  accountNames: string
  protocol: string | null
  validFrom: string | null
  validTo: string | null
  status: string
  description: string | null
  createdAt: string
}

/** 新建 / 修改授权策略请求（对应 AccessPolicyRequest） */
export interface AccessPolicyRequest {
  name: string
  userId: number
  assetIds: number[]
  accountIds?: number[]
  protocol?: string | null
  validFrom?: string | null
  validTo?: string | null
  description?: string | null
}

/** 分页查询授权策略 */
export function listPolicies(params: {
  page?: number
  size?: number
  keyword?: string
  status?: string
}): Promise<PageData<PolicyItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.status) query.set('status', params.status)
  return request<PageData<PolicyItem>>(`/api/access-policies?${query.toString()}`)
}

/** 新建授权策略 */
export function createPolicy(payload: AccessPolicyRequest): Promise<PolicyItem> {
  return request<PolicyItem>('/api/access-policies', { method: 'POST', body: JSON.stringify(payload) })
}

/** 修改授权策略 */
export function updatePolicy(id: number, payload: AccessPolicyRequest): Promise<PolicyItem> {
  return request<PolicyItem>(`/api/access-policies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/** 删除授权策略 */
export function deletePolicy(id: number): Promise<void> {
  return request<void>(`/api/access-policies/${id}`, { method: 'DELETE' })
}

/** 审批通过 */
export function approvePolicy(id: number): Promise<PolicyItem> {
  return request<PolicyItem>(`/api/access-policies/${id}/approve`, { method: 'POST' })
}

/** 撤销（停用） */
export function revokePolicy(id: number): Promise<PolicyItem> {
  return request<PolicyItem>(`/api/access-policies/${id}/revoke`, { method: 'POST' })
}
