/** 统一 API 响应结构 */
export interface RestResult<T> {
  code: number
  errorCode: string | null
  message: string
  data: T
  timestamp?: number
}
