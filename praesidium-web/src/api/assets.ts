/**
 * 资产接口（asset 上下文，对齐 AssetController）：资产 CRUD、连通性测试、资产下账号。
 */
import { request } from './http'
import type { PageData } from './common'
import type { AssetAccountItem } from './accounts'

/** 资产列表项（对应 AssetItem） */
export interface AssetItem {
  id: number
  name: string
  type: string
  address: string
  protocol: string
  port: number | null
  groupPath: string | null
  description: string | null
  status: string
  accountCount: number
  createdAt: string
  updatedAt: string
}

/** 新建 / 修改资产请求（对应 AssetRequest） */
export interface AssetRequest {
  name: string
  type: string
  address: string
  protocol: string
  port?: number | null
  groupPath?: string | null
  description?: string | null
}

/** 连通性测试结果（对应 AssetTestResult） */
export interface AssetTestResult {
  reachable: boolean
  latencyMs: number
  message: string
}

/** 分页查询资产 */
export function listAssets(params: {
  page?: number
  size?: number
  keyword?: string
  type?: string
}): Promise<PageData<AssetItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.type) query.set('type', params.type)
  return request<PageData<AssetItem>>(`/api/assets?${query.toString()}`)
}

/** 新建资产 */
export function createAsset(payload: AssetRequest): Promise<AssetItem> {
  return request<AssetItem>('/api/assets', { method: 'POST', body: JSON.stringify(payload) })
}

/** 修改资产 */
export function updateAsset(id: number, payload: AssetRequest): Promise<AssetItem> {
  return request<AssetItem>(`/api/assets/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}

/** 删除资产 */
export function deleteAsset(id: number): Promise<void> {
  return request<void>(`/api/assets/${id}`, { method: 'DELETE' })
}

/** 连通性测试（TCP 连接，3 秒超时） */
export function testAsset(id: number): Promise<AssetTestResult> {
  return request<AssetTestResult>(`/api/assets/${id}/test`, { method: 'POST' })
}

/** 资产下账号清单 */
export function listAssetAccounts(assetId: number): Promise<AssetAccountItem[]> {
  return request<AssetAccountItem[]>(`/api/assets/${assetId}/accounts`)
}
