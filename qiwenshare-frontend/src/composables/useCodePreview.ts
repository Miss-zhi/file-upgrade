import { ref, computed } from 'vue'
import type { PreviewFileItem } from '@/types/file'
import { fileSuffixCodeModeMap } from '@/types/file'
import { getFileText, modifyFileContent } from '@/api/file'
import { ElMessage } from 'element-plus'

/**
 * 代码预览 composable。
 * 管理代码内容、CodeMirror 配置、编辑/保存功能。
 */
export function useCodePreview() {
  const visible = ref(false)
  const file = ref<PreviewFileItem | null>(null)
  const codeContent = ref('')
  const loading = ref(false)
  const lineWrapping = ref(true)

  /** 语言模式 */
  const languageMode = ref('')

  /** 从 localStorage 恢复主题 */
  const theme = ref(
    localStorage.getItem('qiwen_file_codemirror_theme') || 'default',
  )

  /** 字号 */
  const fontSize = ref(14)

  /** 是否可编辑 */
  const editable = ref(false)

  /** 原始文本（用于脏检测） */
  const originalText = ref('')

  /** 是否已修改（脏状态） */
  const isModified = computed(() => originalText.value !== codeContent.value)

  /** 扩展名映射到 CodeMirror 语言 */
  function detectLanguage(ext: string): string {
    const key = ext?.toLowerCase()
    if (key === 'yml') return 'yaml'
    const mapping = (fileSuffixCodeModeMap as Record<string, { language: string; mime: string }>)[key]
    return mapping?.language || ''
  }

  /** 切换主题 */
  function setTheme(newTheme: string): void {
    theme.value = newTheme
    localStorage.setItem('qiwen_file_codemirror_theme', newTheme)
  }

  /** 设置语言模式 */
  function setLanguageMode(lang: string): void {
    languageMode.value = lang
  }

  /** 设置字号 */
  function setFontSize(size: number): void {
    fontSize.value = size
  }

  /** 切换自动换行 */
  function toggleLineWrapping(): void {
    lineWrapping.value = !lineWrapping.value
  }

  /**
   * 内部：打开预览的核心逻辑。
   * isEdit 控制是否进入编辑模式。
   * 所有状态原子设置，避免 watch 多次触发 buildEditor。
   */
  async function doOpen(f: PreviewFileItem, isEdit: boolean): Promise<void> {
    // 原子设置所有状态（在 async 操作之前）
    file.value = f
    visible.value = true
    editable.value = isEdit
    loading.value = true
    languageMode.value = detectLanguage(f.extendName)
    // 先清空，让编辑器先以空内容创建（避免后续内容到达时尺寸异常）
    codeContent.value = ''
    originalText.value = ''

    // 添加键盘事件
    document.addEventListener('keydown', handleKeydown)

    // 异步加载内容
    try {
      const text = await getFileText(f.userFileId)
      codeContent.value = text
      originalText.value = text
    } catch {
      codeContent.value = '// 加载文件内容失败'
      originalText.value = codeContent.value
    } finally {
      loading.value = false
    }
  }

  /** 打开预览（只读） */
  function open(f: PreviewFileItem): void {
    doOpen(f, false)
  }

  /** 打开编辑（可读写） */
  function openEdit(f: PreviewFileItem): void {
    doOpen(f, true)
  }

  /** 保存文件内容 */
  async function save(): Promise<void> {
    if (!file.value || !isModified.value) return
    loading.value = true
    try {
      await modifyFileContent(file.value.userFileId, codeContent.value)
      originalText.value = codeContent.value
      ElMessage.success('已保存')
    } catch (err: any) {
      ElMessage.error(err?.message || '保存失败')
    } finally {
      loading.value = false
    }
  }

  /** 关闭预览 */
  function close(): void {
    visible.value = false
    file.value = null
    codeContent.value = ''
    originalText.value = ''
    editable.value = false
    document.removeEventListener('keydown', handleKeydown)
  }

  /** 键盘事件 */
  function handleKeydown(e: KeyboardEvent): void {
    if (!visible.value) return
    if (e.key === 'Escape') close()
    // Ctrl+S 保存
    if (e.key === 's' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault()
      if (editable.value && isModified.value) {
        save()
      }
    }
  }

  return {
    visible,
    file,
    codeContent,
    loading,
    lineWrapping,
    languageMode,
    theme,
    fontSize,
    editable,
    isModified,
    detectLanguage,
    setTheme,
    setLanguageMode,
    setFontSize,
    toggleLineWrapping,
    open,
    openEdit,
    save,
    close,
  }
}
