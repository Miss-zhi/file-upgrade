import { ref, computed, watch } from 'vue'
import type { PreviewFileItem } from '@/types/file'
import { getFileContent } from '@/api/file'

/**
 * 视频预览 composable。
 */
export function useVideoPreview() {
  const visible = ref(false)
  const currentIndex = ref(0)
  const fileList = ref<PreviewFileItem[]>([])
  const currentFile = computed(() => fileList.value[currentIndex.value] ?? null)
  const total = computed(() => fileList.value.length)
  const previewUrl = ref('')
  const playbackRate = ref(1)
  const playbackRates = [0.5, 1, 1.5, 2]
  const playlistCollapsed = ref(window.innerWidth <= 768)
  const unsupported = ref(false)
  const loading = ref(false)

  /** 视频元素引用 */
  const videoRef = ref<HTMLVideoElement | null>(null)

  /** 切换到指定索引 */
  function goTo(index: number): void {
    if (index >= 0 && index < total.value) {
      currentIndex.value = index
    }
  }

  /** 上一首 */
  function prev(): void {
    if (currentIndex.value > 0) currentIndex.value--
  }

  /** 下一首 */
  function next(): void {
    if (currentIndex.value < total.value - 1) currentIndex.value++
  }

  /** 切换播放列表 */
  function togglePlaylist(): void {
    playlistCollapsed.value = !playlistCollapsed.value
  }

  /** 切换播放速率 */
  function setPlaybackRate(rate: number): void {
    playbackRate.value = rate
    if (videoRef.value) {
      videoRef.value.playbackRate = rate
    }
  }

  /** 打开预览 */
  async function open(file: PreviewFileItem, list?: PreviewFileItem[]): Promise<void> {
    fileList.value = list && list.length > 0 ? list : [file]
    const idx = fileList.value.findIndex((f) => f.userFileId === file.userFileId)
    currentIndex.value = idx >= 0 ? idx : 0
    playlistCollapsed.value = window.innerWidth <= 768
    unsupported.value = false
    visible.value = true
    loading.value = true

    try {
      await loadCurrentFile()
    } finally {
      loading.value = false
    }

    document.addEventListener('keydown', handleKeydown)
  }

  /** 关闭预览 */
  function close(): void {
    visible.value = false
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = ''
    videoRef.value = null
    document.removeEventListener('keydown', handleKeydown)
  }

  /** 加载当前文件 */
  async function loadCurrentFile(): Promise<void> {
    const file = currentFile.value
    if (!file) return

    // 检测浏览器兼容性
    const ext = file.extendName?.toLowerCase()
    const testVideo = document.createElement('video')
    const mimeMap: Record<string, string> = {
      mp4: 'video/mp4',
      webm: 'video/webm',
      mov: 'video/quicktime',
      avi: 'video/x-msvideo',
      mkv: 'video/x-matroska',
      flv: 'video/x-flv',
    }
    const mime = mimeMap[ext] || ''
    if (mime && testVideo.canPlayType(mime) === '') {
      unsupported.value = true
      loading.value = false
      return
    }

    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    const blob = await getFileContent(file.userFileId)
    previewUrl.value = URL.createObjectURL(blob)
    unsupported.value = false
  }

  /** 键盘事件 */
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

  // 监听 currentIndex 变化
  watch(currentIndex, () => {
    if (visible.value && currentFile.value) {
      loadCurrentFile()
    }
  })

  return {
    visible,
    currentIndex,
    currentFile,
    fileList,
    total,
    previewUrl,
    playbackRate,
    playbackRates,
    playlistCollapsed,
    unsupported,
    loading,
    videoRef,
    goTo,
    prev,
    next,
    togglePlaylist,
    setPlaybackRate,
    open,
    close,
  }
}
