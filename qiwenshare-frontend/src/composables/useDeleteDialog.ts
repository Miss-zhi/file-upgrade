import { createDialogComposable } from './useDialog'
import type { FileInfo } from '@/types/file'

export interface DeleteParams {
  files: FileInfo[]
  mode: 1 | 2 // 1=软删除, 2=永久删除
}

export function useDeleteDialog() {
  return createDialogComposable<DeleteParams>()
}
