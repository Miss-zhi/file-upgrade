import { ref, computed, watch } from 'vue'
import type { PreviewFileItem } from '@/types/file'
import { getFileContent } from '@/api/file'

/**
 * 图片预览 composable。
 * 管理全屏图片预览的状态和交互逻辑。
 */
export function useImagePreview() {
  const visible = ref(false)
  const currentIndex = ref(0)
  const fileList = ref<PreviewFileItem[]>([])
  const currentFile = computed(() => fileList.value[currentIndex.value] ?? null)
  const total = computed(() => fileList.value.length)

  /** 缩放比例 (1-200%) */
  const zoom = ref(100)
  /** 旋转角度 (0/90/180/270) */
  const rotation = ref(0)
  /** 缩略图侧栏折叠状态 */
  const sidebarCollapsed = ref(
    localStorage.getItem('qiwen_file_img_preview_show_min') === 'true',
  )

  const previewUrl = ref<string>('')
  const thumbnailUrls = ref<Map<number, string>>(new Map())
  const loading = ref(false)

  /** 加载文件内容并创建 Object URL */
  async function loadFileContent(file: PreviewFileItem): Promise<string> {
    const blob = await getFileContent(file.userFileId)
    return URL.createObjectURL(blob)
  }

  /** 切换到指定索引 */
  function goTo(index: number): void {
    if (index >= 0 && index < total.value) {
      currentIndex.value = index
    }
  }

  /** 上一张 */
  function prev(): void {
    if (currentIndex.value > 0) {
      currentIndex.value--
    }
  }

  /** 下一张 */
  function next(): void {
    if (currentIndex.value < total.value - 1) {
      currentIndex.value++
    }
  }

  /** 通过鼠标滚轮缩放 */
  function zoomByWheel(delta: number): void {
    zoom.value = Math.max(1, Math.min(200, zoom.value + (delta > 0 ? -10 : 10)))
  }

  /** 设置缩放 */
  function setZoom(value: number): void {
    zoom.value = Math.max(1, Math.min(200, value))
  }

  /** 旋转 */
  function rotate(): void {
    rotation.value = (rotation.value + 90) % 360
  }

  /** 切换侧栏 */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('qiwen_file_img_preview_show_min', String(sidebarCollapsed.value))
  }

  /** 打开预览 */
  async function open(file: PreviewFileItem, list?: PreviewFileItem[]): Promise<void> {
    fileList.value = list && list.length > 0 ? list : [file]
    const idx = fileList.value.findIndex((f) => f.userFileId === file.userFileId)
    currentIndex.value = idx >= 0 ? idx : 0
    zoom.value = 100
    rotation.value = 0
    visible.value = true
    loading.value = true

    try {
      // 预加载所有图片的 Object URL
      for (const f of fileList.value) {
        if (!thumbnailUrls.value.has(f.userFileId)) {
          try {
            const url = await loadFileContent(f)
            thumbnailUrls.value.set(f.userFileId, url)
          } catch {
            // 单个文件加载失败不阻断整体流程
          }
        }
      }
      previewUrl.value = thumbnailUrls.value.get(currentFile.value?.userFileId ?? 0) ?? ''
    } catch {
      // 加载失败
    } finally {
      loading.value = false
    }

    document.addEventListener('keydown', handleKeydown)
  }

  /** 关闭预览 */
  function close(): void {
    visible.value = false
    // 清理 Object URLs
    for (const url of thumbnailUrls.value.values()) {
      URL.revokeObjectURL(url)
    }
    thumbnailUrls.value.clear()
    previewUrl.value = ''
    document.removeEventListener('keydown', handleKeydown)
  }

  /** 键盘事件处理 */
  function handleKeydown(e: KeyboardEvent): void {
    if (!visible.value) return
    switch (e.key) {
      case 'Escape':
        close()
        break
      case 'ArrowLeft':
        prev()
        break
      case 'ArrowRight':
        next()
        break
    }
  }

  // 监听 currentIndex 变化更新预览 URL
  watch(currentIndex, () => {
    const file = currentFile.value
    if (!file) return
    if (thumbnailUrls.value.has(file.userFileId)) {
      previewUrl.value = thumbnailUrls.value.get(file.userFileId) ?? ''
    } else {
      loadFileContent(file).then((url) => {
        thumbnailUrls.value.set(file.userFileId, url)
        previewUrl.value = url
      })
    }
  })

  return {
    visible,
    currentIndex,
    currentFile,
    fileList,
    total,
    zoom,
    rotation,
    sidebarCollapsed,
    previewUrl,
    thumbnailUrls,
    loading,
    goTo,
    prev,
    next,
    zoomByWheel,
    setZoom,
    rotate,
    toggleSidebar,
    open,
    close,
  }
}
