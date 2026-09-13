/**
 * 系统设置接口（settings 上下文，对齐 SettingsController）：全量读取 / 按分组读取与保存。
 *
 * 分组 config 为后端 JSONB 反序列化对象，前端以 Record<string, unknown> 承载，
 * 页面按已知分组键渲染表单。
 */
import { request } from './http'

/** 全量配置：分组名 → 配置对象 */
export function fetchSettings(): Promise<Record<string, Record<string, unknown>>> {
  return request<Record<string, Record<string, unknown>>>('/api/settings')
}

/** 按分组读取配置 */
export function fetchSetting(section: string): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>(`/api/settings/${section}`)
}

/** 按分组保存配置（整体替换） */
export function saveSetting(
  section: string,
  config: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>(`/api/settings/${section}`, {
    method: 'PUT',
    body: JSON.stringify(config),
  })
}
