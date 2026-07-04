import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchFiles } from '@/api/search'
import type { SearchResultVO } from '@/types/search'
import type { FileInfo } from '@/types/file'
import { useFileListStore } from '@/stores/fileList'

/** 允许的 HTML 标签白名单（用于高亮渲染） */
const ALLOWED_TAGS = ['em']
const TAG_REGEX = /<\/?([a-z]+)[^>]*>/gi

/** Element Plus 排序方向 → 后端 sortOrder 映射 */
const SORT_ORDER_MAP: Record<string, string> = {
  ascending: 'asc',
  descending: 'desc',
}

/**
 * 过滤高亮 HTML，仅保留白名单标签。
 * 后端返回 `<em>keyword</em>` 格式，此处做 XSS 防护。
 */
function sanitizeHighlight(html: string): string {
  return html.replace(TAG_REGEX, (match, tag) => {
    return ALLOWED_TAGS.includes(tag.toLowerCase()) ? match : ''
  })
}

/**
 * 搜索状态与逻辑 composable。
 *
 * 搜索模式下接管 fileListStore 的 fileList / total / loading，
 * 使 FileTable / FileGrid / Pagination 无需修改即可展示搜索结果。
 * 高亮文件名通过 highlightMap 提供给子组件。
 */
export function useSearch() {
  const fileListStore = useFileListStore()

  const keyword = ref('')
  const isSearch = ref(false)
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const sortBy = ref('')
  const sortOrder = ref('')

  /** userFileId → 高亮 HTML 映射 */
  const highlightMap = ref<Map<number, string>>(new Map())

  /** 原始文件列表备份（进入搜索时保存，退出时恢复） */
  let savedFileList: FileInfo[] = []
  let savedTotal = 0
  let savedPage = 0

  /** 将 SearchResultVO 映射为 FileInfo */
  function toFileInfo(item: SearchResultVO): FileInfo {
    return {
      userFileId: item.userFileId,
      fileName: item.fileName,
      filePath: item.filePath,
      fileType: item.fileType, // 使用后端返回的 fileType（0=文件夹，1=文件）
      fileSize: item.fileSize,
      extendName: item.extendName,
      uploadTime: item.uploadTime,
      modifyTime: item.modifyTime,
      deleteStatus: 0,
    }
  }

  /** 执行搜索 */
  async function search(kw: string): Promise<void> {
    const trimmed = kw.trim()
    if (!trimmed) {
      clear()
      return
    }

    // 首次进入搜索，备份当前文件列表
    if (!isSearch.value) {
      savedFileList = [...fileListStore.fileList]
      savedTotal = fileListStore.total
      savedPage = fileListStore.currentPage
    }

    keyword.value = trimmed
    isSearch.value = true
    loading.value = true
    fileListStore.loading = true
    currentPage.value = 0

    try {
      const res = await searchFiles({
        keyword: trimmed,
        page: 0,
        size: fileListStore.pageSize,
        sortBy: sortBy.value || undefined,
        sortOrder: sortOrder.value || undefined,
      })

      // 构建高亮映射
      const map = new Map<number, string>()
      const files: FileInfo[] = []
      for (const item of res.items) {
        files.push(toFileInfo(item))
        if (item.highlightFileName) {
          map.set(item.userFileId, sanitizeHighlight(item.highlightFileName))
        }
      }

      highlightMap.value = map
      fileListStore.fileList = files
      fileListStore.total = res.total
      fileListStore.currentPage = 0
      total.value = res.total
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (status === 503) {
        ElMessage.error('搜索服务暂不可用，请稍后再试')
      } else {
        ElMessage.error('搜索失败，请稍后重试')
      }
    } finally {
      loading.value = false
      fileListStore.loading = false
    }
  }

  /** 搜索分页 */
  async function searchPageChange(page: number): Promise<void> {
    if (!isSearch.value) return
    loading.value = true
    fileListStore.loading = true
    currentPage.value = page

    try {
      const res = await searchFiles({
        keyword: keyword.value,
        page,
        size: fileListStore.pageSize,
        sortBy: sortBy.value || undefined,
        sortOrder: sortOrder.value || undefined,
      })

      const map = new Map<number, string>()
      const files: FileInfo[] = []
      for (const item of res.items) {
        files.push(toFileInfo(item))
        if (item.highlightFileName) {
          map.set(item.userFileId, sanitizeHighlight(item.highlightFileName))
        }
      }

      highlightMap.value = map
      fileListStore.fileList = files
      fileListStore.total = res.total
      fileListStore.currentPage = page
      total.value = res.total
    } catch {
      ElMessage.error('搜索失败，请稍后重试')
    } finally {
      loading.value = false
      fileListStore.loading = false
    }
  }

  /** 搜索排序 */
  async function searchSortChange(prop: string, order: string): Promise<void> {
    if (!isSearch.value) return
    sortBy.value = prop
    sortOrder.value = SORT_ORDER_MAP[order] || order
    loading.value = true
    fileListStore.loading = true
    currentPage.value = 0

    try {
      const res = await searchFiles({
        keyword: keyword.value,
        page: 0,
        size: fileListStore.pageSize,
        sortBy: prop,
        sortOrder: SORT_ORDER_MAP[order] || order,
      })

      const map = new Map<number, string>()
      const files: FileInfo[] = []
      for (const item of res.items) {
        files.push(toFileInfo(item))
        if (item.highlightFileName) {
          map.set(item.userFileId, sanitizeHighlight(item.highlightFileName))
        }
      }

      highlightMap.value = map
      fileListStore.fileList = files
      fileListStore.total = res.total
      fileListStore.currentPage = 0
      total.value = res.total
    } catch {
      ElMessage.error('搜索失败，请稍后重试')
    } finally {
      loading.value = false
      fileListStore.loading = false
    }
  }

  /** 清除搜索，恢复原始文件列表 */
  function clear(): void {
    if (isSearch.value) {
      // 恢复备份
      fileListStore.fileList = savedFileList
      fileListStore.total = savedTotal
      fileListStore.currentPage = savedPage
    }
    keyword.value = ''
    isSearch.value = false
    loading.value = false
    fileListStore.loading = false
    total.value = 0
    currentPage.value = 0
    sortBy.value = ''
    sortOrder.value = ''
    highlightMap.value = new Map()
  }

  /** 获取文件的高亮 HTML（供 FileTable / FileGrid 使用） */
  function getHighlight(userFileId: number): string | null {
    return highlightMap.value.get(userFileId) ?? null
  }

  return {
    keyword,
    isSearch,
    loading,
    total,
    currentPage,
    highlightMap,
    search,
    searchPageChange,
    searchSortChange,
    clear,
    getHighlight,
  }
}
