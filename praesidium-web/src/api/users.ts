/**
 * 用户接口（identity 上下文）：简单清单，供授权策略表单的用户下拉。
 */
import { request } from './http'

/** 用户下拉选项（对应 UserOption） */
export interface UserOption {
  id: number
  username: string
  displayName: string | null
}

/** 全量用户清单（演示规模，不分页） */
export function listUsers(): Promise<UserOption[]> {
  return request<UserOption[]>('/api/users')
}
