/**
 * 总览统计接口（dashboard 上下文，对齐 DashboardController）：卡片指标、分布、趋势、最近动态与在线会话。
 */
import { request } from './http'
import type { OperationLogItem } from './logs'
import type { SessionItem } from './sessions'

/** 总览卡片指标（对应 DashboardOverview） */
export interface DashboardOverview {
  totalAssets: number
  totalUsers: number
  onlineSessions: number
  todaySessions: number
  todayLogins: number
  todayFailedLogins: number
  todayOperations: number
  todayBlocked: number
}

/** 名称-数量分布项（对应 NameCount） */
export interface NameCount {
  name: string
  count: number
}

/** 会话趋势点（对应 TrendPoint） */
export interface TrendPoint {
  date: string
  count: number
}

/** 登录趋势点（对应 LoginTrendPoint） */
export interface LoginTrendPoint {
  date: string
  success: number
  failed: number
}

/** 总览卡片指标 */
export function fetchOverview(): Promise<DashboardOverview> {
  return request<DashboardOverview>('/api/dashboard/overview')
}

/** 资产类型分布 */
export function fetchAssetTypes(): Promise<NameCount[]> {
  return request<NameCount[]>('/api/dashboard/asset-types')
}

/** 协议分布 */
export function fetchProtocols(): Promise<NameCount[]> {
  return request<NameCount[]>('/api/dashboard/protocols')
}

/** 近 N 天会话趋势（默认 7 天） */
export function fetchSessionTrend(days = 7): Promise<TrendPoint[]> {
  return request<TrendPoint[]>(`/api/dashboard/session-trend?days=${days}`)
}

/** 近 N 天登录趋势（默认 7 天） */
export function fetchLoginTrend(days = 7): Promise<LoginTrendPoint[]> {
  return request<LoginTrendPoint[]>(`/api/dashboard/login-trend?days=${days}`)
}

/** 最近操作动态（默认 10 条） */
export function fetchRecentOperations(limit = 10): Promise<OperationLogItem[]> {
  return request<OperationLogItem[]>(`/api/dashboard/recent-operations?limit=${limit}`)
}

/** 在线会话列表（默认 5 条） */
export function fetchOnlineSessions(limit = 5): Promise<SessionItem[]> {
  return request<SessionItem[]>(`/api/dashboard/online-sessions?limit=${limit}`)
}
