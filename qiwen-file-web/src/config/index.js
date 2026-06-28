/**
 * 全局配置
 */
export default {
  /** 应用名称 */
  appName: '奇文网盘',

  /** API 基础路径 */
  baseURL: '/api',

  /** 请求超时时间（毫秒） */
  timeout: 30000,

  /** 文件预览最大大小（字节） */
  maxPreviewSize: 100 * 1024 * 1024,

  /** 上传分片大小（字节） */
  chunkSize: 5 * 1024 * 1024
}
