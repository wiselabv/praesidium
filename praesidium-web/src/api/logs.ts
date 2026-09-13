/**
 * 审计日志接口（audit 上下文，对齐 AuditController / LoginLogController）。
 */
import { request } from './http'
import type { PageData } from './common'

/** 操作日志列表项（对应 OperationLogItem） */
export interface OperationLogItem {
  id: number
  time: string
  user: string
  asset: string
  account: string
  action: string
  result: string
  risk: string
  detail: string
  sourceIp: string
}

/** 登录日志列表项（对应 LoginLogItem） */
export interface LoginLogItem {
  id: number
  time: string
  user: string
  sourceIp: string
  location: string | null
  method: string
  result: string
  reason: string | null
  client: string | null
}

/** 分页查询操作日志 */
export function listOperationLogs(params: {
  page?: number
  size?: number
  keyword?: string
  risk?: string
  result?: string
}): Promise<PageData<OperationLogItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.risk) query.set('risk', params.risk)
  if (params.result) query.set('result', params.result)
  return request<PageData<OperationLogItem>>(`/api/logs/operations?${query.toString()}`)
}

/** 最近 N 条操作（总览页「最近动态」） */
export function recentOperations(limit = 10): Promise<OperationLogItem[]> {
  return request<OperationLogItem[]>(`/api/logs/operations/recent?limit=${limit}`)
}

/** 分页查询登录日志 */
export function listLoginLogs(params: {
  page?: number
  size?: number
  keyword?: string
  result?: string
}): Promise<PageData<LoginLogItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.result) query.set('result', params.result)
  return request<PageData<LoginLogItem>>(`/api/logs/login?${query.toString()}`)
}
