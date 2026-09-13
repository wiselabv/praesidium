/**
 * 资产账号接口（asset 上下文，对齐 AssetAccountController）：全局账号清单、修改、删除。
 */
import { request } from './http'
import type { PageData } from './common'

/** 资产账号列表项（对应 AssetAccountItem） */
export interface AssetAccountItem {
  id: number
  assetId: number
  assetName: string
  name: string
  type: string
  privileged: boolean
  source: string | null
  credentialId: number | null
  credentialName: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

/** 新建 / 修改资产账号请求（对应 AssetAccountRequest） */
export interface AssetAccountRequest {
  name: string
  type: string
  privileged: boolean
  source?: string | null
  credentialId?: number | null
  enabled: boolean
}

/** 分页查询资产账号 */
export function listAssetAccounts(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<PageData<AssetAccountItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  return request<PageData<AssetAccountItem>>(`/api/asset-accounts?${query.toString()}`)
}

/** 修改资产账号 */
export function updateAssetAccount(id: number, payload: AssetAccountRequest): Promise<AssetAccountItem> {
  return request<AssetAccountItem>(`/api/asset-accounts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/** 删除资产账号 */
export function deleteAssetAccount(id: number): Promise<void> {
  return request<void>(`/api/asset-accounts/${id}`, { method: 'DELETE' })
}
