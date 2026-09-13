/**
 * 通用接口类型（对齐 Java 管理服务的统一响应结构）。
 */

/** 统一分页响应体（对应后端 PageResponse<T>）：page 从 1 起 */
export interface PageData<T> {
  items: T[]
  total: number
  page: number
  size: number
}
