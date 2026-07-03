import { fileImgMap, fileSuffixCodeModeMap } from '@/types/file'

/**
 * 格式化文件大小为可读字符串。
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const size = (bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 1)
  return `${size} ${units[i]}`
}

/**
 * 判断文件是否为文件夹。
 */
export function isFolder(fileType: number): boolean {
  return fileType === 2
}

/**
 * 获取文件图标路径。
 */
export function getFileIcon(extendName: string, fileType: number): string {
  if (fileType === 2) return '/img/file/dir.png'
  return fileImgMap[extendName?.toLowerCase()] || '/img/file/file_open.png'
}

/**
 * 获取文件预览 URL（图片/视频返回服务端缩略图地址，其他文件返回静态图标）。
 *
 * <p>对齐旧项目 setFileImg 逻辑：图片和视频在文件列表中显示实际缩略图，
 * 其他文件类型显示静态图标。</p>
 */
export function getFileIconSrc(extendName: string, fileType: number, userFileId: number): string {
  if (fileType === 2) return '/img/file/dir.png'
  const ext = extendName?.toLowerCase() || ''
  // 图片和视频：返回服务端缩略图 URL（isMin=true）
  if (isImage(ext) || isVideo(ext)) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    return `${baseUrl}/filetransfer/preview/${userFileId}?isMin=true`
  }
  return fileImgMap[ext] || '/img/file/file_open.png'
}

/**
 * 判断文件是否为视频（用于模板中区分 <video> 和 <img> 渲染）。
 */
export function isVideoFile(extendName: string): boolean {
  return ['mp4', 'avi', 'mkv', 'mov', 'webm', 'flv'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为压缩格式。
 */
export function isArchive(extendName: string): boolean {
  return ['zip', 'rar', '7z', 'tar', 'gz'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为图片。
 */
export function isImage(extendName: string): boolean {
  return ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为视频。
 */
export function isVideo(extendName: string): boolean {
  return ['mp4', 'avi', 'mkv', 'mov', 'webm', 'flv'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为音频。
 */
export function isAudio(extendName: string): boolean {
  return ['mp3', 'flac', 'wav', 'aac', 'ogg'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为 Office 文档。
 */
export function isOffice(extendName: string): boolean {
  return ['ppt', 'pptx', 'doc', 'docx', 'xls', 'xlsx'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为 Markdown。
 */
export function isMarkdown(extendName: string): boolean {
  return ['md', 'markdown'].includes(extendName?.toLowerCase())
}

/**
 * 判断文件是否为代码文件。
 */
export function isCode(extendName: string): boolean {
  return extendName?.toLowerCase() in fileSuffixCodeModeMap
}

/**
 * 判断文件是否支持在线编辑（Office、Markdown、代码）。
 */
export function canEditOnline(extendName: string, fileType: number): boolean {
  // 文件夹不支持在线编辑
  if (isFolder(fileType)) return false
  const ext = extendName?.toLowerCase() || ''
  return isOffice(ext) || isMarkdown(ext) || isCode(ext)
}

/**
 * 非法文件名字符正则。
 */
export const invalidFileNameRegex = /[\\/:*?"<>|]/

/**
 * 校验文件名是否合法。
 */
export function isValidFileName(name: string): boolean {
  return name.length > 0 && !invalidFileNameRegex.test(name)
}
