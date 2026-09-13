/**
 * 个人信息接口（identity 上下文，对齐 ProfileController）：查看 / 更新当前用户资料。
 */
import { request } from './http'

/** 个人信息（对应 ProfileResponse） */
export interface ProfileResponse {
  id: number
  username: string
  displayName: string
  email: string | null
  mfaEnabled: boolean
  status: string
  avatar: string | null
  phone: string | null
  department: string | null
  title: string | null
  bio: string | null
  lastLoginAt: string | null
  lastLoginIp: string | null
  createdAt: string
}

/** 更新个人信息请求（对应 UpdateProfileRequest） */
export interface UpdateProfileRequest {
  displayName: string
  email?: string | null
  avatar?: string | null
  phone?: string | null
  department?: string | null
  title?: string | null
  bio?: string | null
}

/** 当前用户资料 */
export function fetchProfile(): Promise<ProfileResponse> {
  return request<ProfileResponse>('/api/profile')
}

/** 更新当前用户资料 */
export function updateProfile(payload: UpdateProfileRequest): Promise<ProfileResponse> {
  return request<ProfileResponse>('/api/profile', { method: 'PUT', body: JSON.stringify(payload) })
}
