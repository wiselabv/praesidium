/**
 * 会话接口（sessions 上下文，对齐 SessionController）：在线会话查询/断开、发起连接、录像查询。
 */
import { request } from './http'
import type { PageData } from './common'

/** 会话列表项（对应 SessionItem，durationSeconds 为已持续秒数） */
export interface SessionItem {
  id: number
  user: string
  asset: string
  account: string
  protocol: string
  status: string
  sourceIp: string
  startedAt: string
  durationSeconds: number
}

/** 录像列表项（对应 RecordingItem） */
export interface RecordingItem {
  id: number
  sessionId: number
  user: string
  asset: string
  protocol: string
  startedAt: string
  durationSeconds: number
  sizeBytes: number
}

/** 发起连接请求（对应 ConnectRequest） */
export interface ConnectRequest {
  assetId: number
  accountId?: number | null
  protocol: string
}

/** 分页查询会话（status 传 online/closed 等过滤） */
export function listSessions(params: {
  page?: number
  size?: number
  keyword?: string
  status?: string
}): Promise<PageData<SessionItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.status) query.set('status', params.status)
  return request<PageData<SessionItem>>(`/api/sessions?${query.toString()}`)
}

/** 断开在线会话 */
export function disconnectSession(id: number): Promise<SessionItem> {
  return request<SessionItem>(`/api/sessions/${id}/disconnect`, { method: 'POST' })
}

/** 发起连接（演示网关：创建一条在线会话记录） */
export function connectSession(payload: ConnectRequest): Promise<SessionItem> {
  return request<SessionItem>('/api/sessions/connect', { method: 'POST', body: JSON.stringify(payload) })
}

/** 会话录像分页 */
export function listRecordings(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<PageData<RecordingItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  return request<PageData<RecordingItem>>(`/api/sessions/recordings?${query.toString()}`)
}
