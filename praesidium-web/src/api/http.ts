/**
 * 统一 HTTP 请求层（fetch 封装，零第三方依赖）。
 *
 * 约定（对齐 Java 管理服务）：
 *   - 响应体统一 { code, message, data }，code=0 成功，非 0 为业务错误码；
 *   - 业务失败仍以 HTTP 200 返回；仅 401/403 由安全层映射为对应 HTTP 状态码；
 *   - access token 失效（HTTP 401）时自动用 refresh token 续期并重放请求（单飞防并发），
 *     续期失败触发 onSessionExpired 回调（清登录态并跳回登录页）。
 */

/** 统一响应信封（对应后端 ApiResponse<T>） */
interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** API 错误：code 为后端业务码或 HTTP 状态码；0 表示网络层失败 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/** 认证绑定（由入口 main.ts 注入，避免 http 层反向依赖 store） */
interface AuthBinding {
  /** 当前 access token（无则返回空串） */
  getAccessToken: () => string
  /** 用 refresh token 续期，成功返回 true */
  refreshSession: () => Promise<boolean>
  /** 登录态已失效：清本地状态并跳回登录页 */
  onSessionExpired: () => void
}

let binding: AuthBinding | null = null
/** 续期单飞锁：并发 401 只触发一次 refresh，其余请求等待复用结果 */
let refreshing: Promise<boolean> | null = null

export function bindAuth(next: AuthBinding): void {
  binding = next
}

function tryRefresh(): Promise<boolean> {
  if (!binding) return Promise.resolve(false)
  if (!refreshing) {
    refreshing = binding.refreshSession().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

interface RequestOptions {
  /** 401 时跳过自动续期重放（refresh 接口自身使用，防递归） */
  skipAuthRetry?: boolean
  /** 不注入 Authorization 头（refresh 接口自身使用：携带已失效的旧 access token 会污染请求） */
  skipAuthHeader?: boolean
}

/** 发起请求：成功返回 data；业务码非 0 / HTTP 错误 / 网络失败抛 ApiError */
export async function request<T>(
  path: string,
  init: RequestInit = {},
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = binding?.getAccessToken() ?? ''
  if (token && !options.skipAuthHeader) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, '网络连接失败，请检查服务是否可用')
  }

  // 401：尝试续期后重放一次；续期失败或不可续期则视为登录态失效
  if (response.status === 401) {
    const renewed = !options.skipAuthRetry ? await tryRefresh() : false
    if (renewed) {
      return request<T>(path, init, { skipAuthRetry: true })
    }
    binding?.onSessionExpired()
    throw new ApiError(401, '登录已过期，请重新登录')
  }

  let body: ApiEnvelope<T> | null = null
  try {
    body = (await response.json()) as ApiEnvelope<T>
  } catch {
    body = null
  }

  if (!response.ok || !body) {
    const message = body?.message ?? `请求失败（HTTP ${response.status}）`
    throw new ApiError(response.status, message)
  }
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message)
  }
  return body.data
}
