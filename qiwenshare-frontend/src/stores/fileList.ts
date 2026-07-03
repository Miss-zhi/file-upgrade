import { ref } from 'vue'
import { defineStore } from 'pinia'
import { FileViewMode, FileType } from '@/types/file'
import { allColumnList } from '@/types/file'
import type { FileInfo, PageResult } from '@/types/file'
import {
  getFileList,
  getFileListByCategory,
  getRecycleList,
  getMyShares,
} from '@/api/file'

const LS_SELECTED_COLUMNS = 'qiwen_selected_columns'
const LS_FILE_MODEL = 'qiwen_file_model'
const LS_GRID_SIZE = 'qiwen_grid_size'

/** fileType → category 映射 */
const categoryMap: Record<number, string> = {
  [FileType.IMAGE]: 'image',
  [FileType.DOCUMENT]: 'document',
  [FileType.VIDEO]: 'video',
  [FileType.MUSIC]: 'music',
  [FileType.OTHER]: 'other',
}

/**
 * 文件列表状态管理。
 * 包含文件数据、分页、加载状态，以及偏好持久化。
 */
export const useFileListStore = defineStore('fileList', () => {
  // ---- 偏好持久化 ----

  const selectedColumnList = ref<string[]>(
    JSON.parse(localStorage.getItem(LS_SELECTED_COLUMNS) || 'null') ?? [...allColumnList],
  )

  const fileModel = ref<FileViewMode>(
    Number(localStorage.getItem(LS_FILE_MODEL)) || FileViewMode.LIST,
  )

  const gridSize = ref<number>(
    Number(localStorage.getItem(LS_GRID_SIZE)) || 80,
  )

  // ---- 文件数据 ----

  const fileList = ref<FileInfo[]>([])
  const total = ref(0)
  const loading = ref(false)
  const currentPage = ref(0)

  // ---- 选择状态 ----

  const selectedFiles = ref<FileInfo[]>([])
  const isBatchOperation = ref(false)

  // ---- 偏好 setters ----

  function setSelectedColumnList(columns: string[]): void {
    selectedColumnList.value = columns
    localStorage.setItem(LS_SELECTED_COLUMNS, JSON.stringify(columns))
  }

  function setFileModel(mode: FileViewMode): void {
    fileModel.value = mode
    localStorage.setItem(LS_FILE_MODEL, String(mode))
  }

  function setGridSize(size: number): void {
    gridSize.value = size
    localStorage.setItem(LS_GRID_SIZE, String(size))
  }

  // ---- 数据操作 ----

  /**
   * 根据 fileType 路由到不同 API 获取文件列表。
   */
  async function fetchFileList(params: {
    fileType: number
    filePath?: string
    page?: number
    size?: number
    order?: string
    sort?: string
  }): Promise<void> {
    loading.value = true
    try {
      const { fileType, filePath = '/', page = 0, size = 20, order, sort } = params
      let result: PageResult<FileInfo>

      if (fileType === FileType.ALL) {
        result = await getFileList({ filePath, page, size, order, sort })
      } else if (fileType === FileType.RECYCLE) {
        result = await getRecycleList(page, size)
      } else if (fileType === FileType.SHARE) {
        const shares = await getMyShares()
        // 将 ShareInfo[] 适配为 PageResult<FileInfo>
        result = {
          content: shares.map((s) => ({
            userFileId: s.userFileId,
            fileName: s.fileName,
            filePath: '',
            fileType: 1,
            fileSize: s.fileSize,
            extendName: '',
            uploadTime: s.createTime,
            modifyTime: s.createTime,
            deleteStatus: 0,
          })),
          totalElements: shares.length,
          totalPages: 1,
          number: 0,
          size: shares.length,
        }
      } else {
        const category = categoryMap[fileType]
        if (!category) {
          throw new Error(`Unknown fileType: ${fileType}`)
        }
        result = await getFileListByCategory({ category, page, size })
      }

      fileList.value = result.content
      total.value = result.totalElements
      currentPage.value = result.number
    } finally {
      loading.value = false
    }
  }

  /** 设置文件列表（用于外部直接设置） */
  function setFileList(data: FileInfo[]): void {
    fileList.value = data
  }

  /** 清空文件列表 */
  function clearFileList(): void {
    fileList.value = []
    total.value = 0
    currentPage.value = 0
  }

  /** 清空选择 */
  function clearSelection(): void {
    selectedFiles.value = []
    isBatchOperation.value = false
  }

  return {
    // 偏好
    selectedColumnList,
    fileModel,
    gridSize,
    setSelectedColumnList,
    setFileModel,
    setGridSize,
    // 数据
    fileList,
    total,
    loading,
    currentPage,
    fetchFileList,
    setFileList,
    clearFileList,
    // 选择
    selectedFiles,
    isBatchOperation,
    clearSelection,
  }
})
