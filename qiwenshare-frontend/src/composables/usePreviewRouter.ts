import { ref } from 'vue'
import { isImage, isVideo, isAudio, isOffice, isMarkdown, isCode } from '@/utils/file'
import { PreviewType } from '@/types/file'
import type { FileInfo, PreviewFileItem } from '@/types/file'
import { downloadFile } from '@/api/file'

/**
 * 提取文件扩展名（不含点号，小写）
 */
export function getFileExtension(fileName: string): string {
  const idx = fileName.lastIndexOf('.')
  if (idx === -1) return ''
  return fileName.substring(idx + 1).toLowerCase()
}

/**
 * 预览文件项转换：将 FileInfo → PreviewFileItem
 */
export function toPreviewFileItem(file: FileInfo): PreviewFileItem {
  return {
    userFileId: file.userFileId,
    fileName: file.fileName,
    filePath: file.filePath,
    extendName: file.extendName,
    fileSize: file.fileSize,
    fileType: file.fileType,
  }
}

/**
 * 预览路由调度 composable。
 * 根据文件扩展名路由到对应的预览类型。
 */
export function usePreviewRouter() {
  /** 当前预览类型 */
  const currentPreviewType = ref<PreviewType>(PreviewType.UNKNOWN)

  /**
   * 打开 Office 预览（新标签页）
   */
  function openOfficePreview(file: FileInfo, isEdit = false): void {
    const mode = isEdit ? 'edit' : 'preview'
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    window.open(`${baseUrl}/preview/office?userFileId=${file.userFileId}&mode=${mode}`, '_blank')
  }

  /**
   * 打开文件预览：根据扩展名路由到对应处理。
   * 返回 `true` 表示已被预览处理，`false` 表示需要下载。
   */
  function openFilePreview(
    file: FileInfo,
    fileList?: FileInfo[],
    callbacks?: {
      onImage?: (file: PreviewFileItem, list?: PreviewFileItem[]) => void
      onVideo?: (file: PreviewFileItem, list?: PreviewFileItem[]) => void
      onAudio?: (file: PreviewFileItem, list?: PreviewFileItem[]) => void
      onCode?: (file: PreviewFileItem) => void
      onMarkdown?: (file: PreviewFileItem) => void
    },
  ): boolean {
    const ext = file.extendName?.toLowerCase() || getFileExtension(file.fileName)

    // Office 文件
    if (isOffice(ext)) {
      currentPreviewType.value = PreviewType.OFFICE
      openOfficePreview(file)
      return true
    }

    // PDF
    if (ext === 'pdf') {
      currentPreviewType.value = PreviewType.OFFICE
      openOfficePreview(file)
      return true
    }

    const previewItem = toPreviewFileItem(file)

    // 图片：只传递同类型（图片）文件列表，避免上下切换跳到非同类型文件
    if (isImage(ext)) {
      currentPreviewType.value = PreviewType.IMAGE
      const sameTypeList = fileList
        ?.filter((f) => isImage(f.extendName?.toLowerCase() || getFileExtension(f.fileName)))
        .map(toPreviewFileItem)
      callbacks?.onImage?.(previewItem, sameTypeList)
      return true
    }

    // 视频：只传递同类型（视频）文件列表
    if (isVideo(ext)) {
      currentPreviewType.value = PreviewType.VIDEO
      const sameTypeList = fileList
        ?.filter((f) => isVideo(f.extendName?.toLowerCase() || getFileExtension(f.fileName)))
        .map(toPreviewFileItem)
      callbacks?.onVideo?.(previewItem, sameTypeList)
      return true
    }

    // 音频：只传递同类型（音频）文件列表
    if (isAudio(ext)) {
      currentPreviewType.value = PreviewType.AUDIO
      const sameTypeList = fileList
        ?.filter((f) => isAudio(f.extendName?.toLowerCase() || getFileExtension(f.fileName)))
        .map(toPreviewFileItem)
      callbacks?.onAudio?.(previewItem, sameTypeList)
      return true
    }

    // Markdown
    if (isMarkdown(ext)) {
      currentPreviewType.value = PreviewType.MARKDOWN
      callbacks?.onMarkdown?.(previewItem)
      return true
    }

    // 代码
    if (isCode(ext)) {
      currentPreviewType.value = PreviewType.CODE
      callbacks?.onCode?.(previewItem)
      return true
    }

    // 未知类型：下载
    currentPreviewType.value = PreviewType.UNKNOWN
    downloadFile(file.userFileId)
    return false
  }

  return {
    currentPreviewType,
    openFilePreview,
    openOfficePreview,
  }
}
