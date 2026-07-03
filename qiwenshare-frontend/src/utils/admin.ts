/**
 * 配额单位转换工具函数。
 * 后端配额统一使用字节（Byte），前端展示时可读化处理。
 */

const UNITS = ['B', 'KB', 'MB', 'GB', 'TB']

/**
 * 字节 → 可读格式（自动选择最合适的单位）。
 *
 * @param bytes 字节数
 * @param decimals 小数位数，默认 2
 * @returns 可读字符串，如 "1.50 MB"
 */
export function formatBytes(bytes: number, decimals: number = 2): string {
  if (bytes === 0) return '0 B'
  if (bytes < 0) return '0 B'

  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const idx = Math.min(i, UNITS.length - 1)
  const value = bytes / Math.pow(k, idx)

  return `${value.toFixed(idx === 0 ? 0 : decimals)} ${UNITS[idx]}`
}

/**
 * 字节 → MB（用于配额输入框）。
 *
 * @param bytes 字节数
 * @returns MB 数值
 */
export function bytesToMB(bytes: number): number {
  return Math.round(bytes / (1024 * 1024))
}

/**
 * MB → 字节。
 *
 * @param mb MB 数值
 * @returns 字节数
 */
export function mbToBytes(mb: number): number {
  return mb * 1024 * 1024
}

/**
 * 字节 → GB（用于大额配额展示）。
 *
 * @param bytes 字节数
 * @param decimals 小数位数
 * @returns GB 数值字符串
 */
export function bytesToGB(bytes: number, decimals: number = 2): string {
  return (bytes / (1024 * 1024 * 1024)).toFixed(decimals)
}

/**
 * 计算用量百分比。
 *
 * @param used 已用字节
 * @param total 总配额字节
 * @returns 百分比整数 (0-100)
 */
export function calcUsagePercent(used: number, total: number): number {
  if (total <= 0) return 0
  return Math.round((used / total) * 100)
}

/**
 * 用量百分比 → 进度条颜色。
 *
 * @param percent 百分比 (0-100)
 * @returns CSS 颜色值
 */
export function usageColor(percent: number): string {
  if (percent <= 50) return '#67C23A'
  if (percent <= 80) return '#E6A23C'
  return '#F56C6C'
}
