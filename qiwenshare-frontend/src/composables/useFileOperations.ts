import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FileInfo, BatchOperationResult } from '@/types/file'
import {
  renameFile as renameFileApi,
  moveFile as moveFileApi,
  batchMoveFile as batchMoveFileApi,
  copyFile as copyFileApi,
  batchCopyFile as batchCopyFileApi,
  deleteFile as deleteFileApi,
  batchDeleteFile as batchDeleteFileApi,
  restoreFile as restoreFileApi,
  deletePermanent as deletePermanentApi,
  createShare as createShareApi,
  createFolder as createFolderApi,
} from '@/api/file'
import type { ShareCreateDTO } from '@/types/file'

/**
 * 文件操作 composable。
 * 封装 delete/move/copy/rename/share 等操作的 loading 状态和错误处理。
 */
export function useFileOperations() {
  const operationLoading = ref(false)

  /** 重命名文件 */
  async function rename(userFileId: number, newName: string): Promise<boolean> {
    operationLoading.value = true
    try {
      await renameFileApi({ userFileId, newName })
      ElMessage.success('重命名成功')
      return true
    } catch {
      ElMessage.error('重命名失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 移动文件 */
  async function move(userFileId: number, targetFolderId: number | null): Promise<boolean> {
    operationLoading.value = true
    try {
      await moveFileApi({ userFileId, targetFolderId })
      ElMessage.success('移动成功')
      return true
    } catch {
      ElMessage.error('移动失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 批量移动文件 */
  async function batchMove(userFileIds: number[], targetFolderId: number | null): Promise<BatchOperationResult> {
    operationLoading.value = true
    try {
      const result = await batchMoveFileApi({ userFileIds, targetFolderId })
      if (result.failedItems.length > 0) {
        ElMessage.warning(`成功 ${result.successCount} 个，失败 ${result.failedItems.length} 个`)
      } else {
        ElMessage.success(`成功移动 ${result.successCount} 个文件`)
      }
      return result
    } catch {
      ElMessage.error('批量移动失败')
      return { successCount: 0, failedItems: [] }
    } finally {
      operationLoading.value = false
    }
  }

  /** 复制文件 */
  async function copy(userFileId: number, targetFolderId: number | null): Promise<boolean> {
    operationLoading.value = true
    try {
      await copyFileApi({ userFileId, targetFolderId })
      ElMessage.success('复制成功')
      return true
    } catch {
      ElMessage.error('复制失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 批量复制文件 */
  async function batchCopy(userFileIds: number[], targetFolderId: number | null): Promise<BatchOperationResult> {
    operationLoading.value = true
    try {
      const result = await batchCopyFileApi({ userFileIds, targetFolderId })
      if (result.failedItems.length > 0) {
        ElMessage.warning(`成功 ${result.successCount} 个，失败 ${result.failedItems.length} 个`)
      } else {
        ElMessage.success(`成功复制 ${result.successCount} 个文件`)
      }
      return result
    } catch {
      ElMessage.error('批量复制失败')
      return { successCount: 0, failedItems: [] }
    } finally {
      operationLoading.value = false
    }
  }

  /** 软删除文件 */
  async function remove(userFileId: number): Promise<boolean> {
    operationLoading.value = true
    try {
      await deleteFileApi({ userFileId })
      ElMessage.success('删除成功，文件已移至回收站')
      return true
    } catch {
      ElMessage.error('删除失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 批量软删除 */
  async function batchRemove(userFileIds: number[]): Promise<boolean> {
    operationLoading.value = true
    try {
      await batchDeleteFileApi({ userFileIds })
      ElMessage.success('批量删除成功，文件已移至回收站')
      return true
    } catch {
      ElMessage.error('批量删除失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 恢复文件 */
  async function restore(userFileIds: number[]): Promise<boolean> {
    operationLoading.value = true
    try {
      await restoreFileApi({ userFileIds })
      ElMessage.success('恢复成功')
      return true
    } catch {
      ElMessage.error('恢复失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 永久删除 */
  async function permanentDelete(userFileIds: number[]): Promise<boolean> {
    operationLoading.value = true
    try {
      await deletePermanentApi({ userFileIds })
      ElMessage.success('永久删除成功')
      return true
    } catch {
      ElMessage.error('永久删除失败')
      return false
    } finally {
      operationLoading.value = false
    }
  }

  /** 创建分享 */
  async function share(dto: ShareCreateDTO): Promise<import('@/types/file').ShareInfo | null> {
    operationLoading.value = true
    try {
      const result = await createShareApi(dto)
      ElMessage.success('分享创建成功')
      return result
    } catch {
      ElMessage.error('创建分享失败')
      return null
    } finally {
      operationLoading.value = false
    }
  }

  /** 创建文件夹 */
  async function addFolder(folderName: string, filePath: string): Promise<number | null> {
    operationLoading.value = true
    try {
      const id = await createFolderApi({ folderName, filePath })
      ElMessage.success('文件夹创建成功')
      return id
    } catch {
      ElMessage.error('创建文件夹失败')
      return null
    } finally {
      operationLoading.value = false
    }
  }

  return {
    operationLoading,
    rename,
    move,
    batchMove,
    copy,
    batchCopy,
    remove,
    batchRemove,
    restore,
    permanentDelete,
    share,
    addFolder,
  }
}
