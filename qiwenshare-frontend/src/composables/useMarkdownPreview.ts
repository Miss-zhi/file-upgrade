import { ref, computed } from 'vue'
import type { PreviewFileItem } from '@/types/file'
import { getFileText, modifyFileContent } from '@/api/file'
import { ElMessage } from 'element-plus'

/**
 * Markdown 预览/编辑 composable。
 * 使用 md-editor-v3 实现所见即所得编辑。
 */
export function useMarkdownPreview() {
  const visible = ref(false)
  const file = ref<PreviewFileItem | null>(null)
  const markdownText = ref('')
  const loading = ref(false)

  /** 是否可编辑 */
  const editable = ref(false)

  /** 原始文本（用于脏检测） */
  const originalText = ref('')

  /** 是否已修改 */
  const isModified = computed(() => originalText.value !== markdownText.value)

  /**
   * 内部：打开预览的核心逻辑。
   */
  async function doOpen(f: PreviewFileItem, isEdit: boolean): Promise<void> {
    file.value = f
    visible.value = true
    editable.value = isEdit
    loading.value = true
    markdownText.value = ''
    originalText.value = ''

    document.addEventListener('keydown', handleKeydown)

    try {
      const text = await getFileText(f.userFileId)
      markdownText.value = text
      originalText.value = text
    } catch {
      markdownText.value = '# 加载文件内容失败'
      originalText.value = markdownText.value
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
      await modifyFileContent(file.value.userFileId, markdownText.value)
      originalText.value = markdownText.value
      ElMessage.success('已保存')
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '保存失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  }

  /** 关闭预览 */
  function close(): void {
    visible.value = false
    file.value = null
    markdownText.value = ''
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
    markdownText,
    loading,
    editable,
    isModified,
    open,
    openEdit,
    save,
    close,
  }
}
