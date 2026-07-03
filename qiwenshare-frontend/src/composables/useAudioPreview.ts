import { ref, computed, watch } from 'vue'
import type { PreviewFileItem, MusicMetadata } from '@/types/file'
import { getFileContent, getFileDetail } from '@/api/file'
import { Base64 } from 'js-base64'
import { formatFileSize } from '@/utils/file'

/** LRC 歌词行解析结果 */
interface LyricLine {
  time: number // 毫秒
  text: string
}

/**
 * 解析 LRC 格式歌词
 */
function parseLRC(lrcText: string): LyricLine[] {
  const lines = lrcText.split('\n')
  const result: LyricLine[] = []
  const timeRegex = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]/g

  for (const line of lines) {
    let match: RegExpExecArray | null
    const texts: string[] = []
    // 提取文本（最后一个时间标签后的内容）
    const text = line.replace(timeRegex, '').trim()
    if (!text) continue

    // 重新匹配所有时间标签
    timeRegex.lastIndex = 0
    while ((match = timeRegex.exec(line)) !== null) {
      const min = parseInt(match[1] ?? '0')
      const sec = parseInt(match[2] ?? '0')
      const ms = match[3] ? parseInt(match[3].padEnd(3, '0')) : 0
      result.push({ time: min * 60000 + sec * 1000 + ms, text })
    }
  }

  return result.sort((a, b) => a.time - b.time)
}

/**
 * 音频预览 composable。
 */
export function useAudioPreview() {
  const visible = ref(false)
  const currentIndex = ref(0)
  const fileList = ref<PreviewFileItem[]>([])
  const currentFile = computed(() => fileList.value[currentIndex.value] ?? null)
  const total = computed(() => fileList.value.length)
  const previewUrl = ref('')
  const isPlaying = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const volume = ref(1)

  /** 循环模式: 1=列表循环, 2=单曲循环, 3=随机 */
  const loopMode = ref<1 | 2 | 3>(1)

  /** 音乐元数据 */
  const musicMeta = ref<MusicMetadata>({})
  const albumImageUrl = ref('')
  const lyrics = ref<LyricLine[]>([])
  const currentLyricIndex = ref(-1)

  /** 音频元素引用 */
  const audioRef = ref<HTMLAudioElement | null>(null)

  /** 格式化进度时间 */
  const formattedCurrentTime = computed(() => formatTime(currentTime.value))
  const formattedDuration = computed(() => formatTime(duration.value))

  function formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60)
    const s = Math.floor(seconds % 60)
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  /** 切换到指定索引 */
  function goTo(index: number): void {
    if (index >= 0 && index < total.value) {
      currentIndex.value = index
    }
  }

  /** 上一首 */
  function prev(): void {
    if (total.value <= 1) return
    if (loopMode.value === 3) {
      // 随机
      currentIndex.value = Math.floor(Math.random() * total.value)
    } else if (currentIndex.value > 0) {
      currentIndex.value--
    } else {
      currentIndex.value = total.value - 1
    }
  }

  /** 下一首 */
  function next(): void {
    if (total.value <= 1) return
    if (loopMode.value === 3) {
      currentIndex.value = Math.floor(Math.random() * total.value)
    } else if (currentIndex.value < total.value - 1) {
      currentIndex.value++
    } else {
      currentIndex.value = 0
    }
  }

  /** 播放/暂停 */
  function togglePlay(): void {
    if (!audioRef.value) return
    if (isPlaying.value) {
      audioRef.value.pause()
    } else {
      audioRef.value.play()
    }
  }

  /** 切换循环模式 */
  function toggleLoopMode(): void {
    if (loopMode.value === 1) loopMode.value = 2
    else if (loopMode.value === 2) loopMode.value = 3
    else loopMode.value = 1
  }

  /** 设置音量 */
  function setVolume(value: number): void {
    volume.value = Math.max(0, Math.min(1, value))
    if (audioRef.value) audioRef.value.volume = volume.value
  }

  /** 设置进度 */
  function seek(time: number): void {
    if (audioRef.value) {
      audioRef.value.currentTime = time
    }
  }

  /** 音频时间更新 */
  function onTimeUpdate(): void {
    if (audioRef.value) {
      currentTime.value = audioRef.value.currentTime
    }
  }

  /** 音频元数据加载完成 */
  function onLoadedMetadata(): void {
    if (audioRef.value) {
      duration.value = audioRef.value.duration
    }
  }

  /** 音频播放状态变化 */
  function onPlay(): void { isPlaying.value = true }
  function onPause(): void { isPlaying.value = false }

  /** 音频播放结束 */
  function onEnded(): void {
    if (loopMode.value === 2) {
      // 单曲循环
      if (audioRef.value) {
        audioRef.value.currentTime = 0
        audioRef.value.play()
      }
    } else {
      next()
    }
  }

  /** 打开预览 */
  async function open(file: PreviewFileItem, list?: PreviewFileItem[]): Promise<void> {
    fileList.value = list && list.length > 0 ? list : [file]
    const idx = fileList.value.findIndex((f) => f.userFileId === file.userFileId)
    currentIndex.value = idx >= 0 ? idx : 0
    visible.value = true
    musicMeta.value = {}
    albumImageUrl.value = ''
    lyrics.value = []
    currentLyricIndex.value = -1

    try {
      await loadCurrentFile()
    } catch {
      // ignore
    }

    document.addEventListener('keydown', handleKeydown)
  }

  /** 关闭预览 */
  function close(): void {
    visible.value = false
    if (audioRef.value) {
      audioRef.value.pause()
    }
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = ''
    document.removeEventListener('keydown', handleKeydown)
  }

  /** 加载当前文件 */
  async function loadCurrentFile(): Promise<void> {
    const file = currentFile.value
    if (!file) return

    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    const blob = await getFileContent(file.userFileId)
    previewUrl.value = URL.createObjectURL(blob)

    // 获取音乐元数据
    try {
      const detail = await getFileDetail(file.userFileId)
      const music = (detail as unknown as { music?: MusicMetadata }).music
      if (music) {
        musicMeta.value = music

        if (music.albumImage) {
          albumImageUrl.value = `data:image/jpeg;base64,${music.albumImage}`
        }
        if (music.lyrics) {
          try {
            const lrcText = Base64.decode(music.lyrics)
            lyrics.value = parseLRC(lrcText)
          } catch {
            lyrics.value = []
          }
        }
      }
    } catch {
      // 元数据获取失败不影响播放
    }
  }

  /** 更新歌词高亮 */
  function updateLyrics(currentSeconds: number): void {
    const ms = currentSeconds * 1000
    let idx = -1
    for (let i = 0; i < lyrics.value.length; i++) {
      const lyric = lyrics.value[i]
      if (lyric && lyric.time <= ms) {
        idx = i
      } else {
        break
      }
    }
    currentLyricIndex.value = idx
  }

  // 监听播放进度更新歌词
  watch(currentTime, (val) => {
    updateLyrics(val)
  })

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
      case 'ArrowUp':
        setVolume(volume.value + 0.1)
        break
      case 'ArrowDown':
        setVolume(volume.value - 0.1)
        break
      case ' ':
        e.preventDefault()
        togglePlay()
        break
    }
  }

  // 监听 currentIndex 变化
  watch(currentIndex, () => {
    if (visible.value && currentFile.value) {
      loadCurrentFile().then(() => {
        if (audioRef.value && previewUrl.value) {
          audioRef.value.load()
          audioRef.value.play()
          isPlaying.value = true
        }
      })
    }
  })

  return {
    visible,
    currentIndex,
    currentFile,
    fileList,
    total,
    previewUrl,
    isPlaying,
    currentTime,
    duration,
    volume,
    loopMode,
    musicMeta,
    albumImageUrl,
    lyrics,
    currentLyricIndex,
    formattedCurrentTime,
    formattedDuration,
    audioRef,
    goTo,
    prev,
    next,
    togglePlay,
    toggleLoopMode,
    setVolume,
    seek,
    onTimeUpdate,
    onLoadedMetadata,
    onPlay,
    onPause,
    onEnded,
    open,
    close,
  }
}
