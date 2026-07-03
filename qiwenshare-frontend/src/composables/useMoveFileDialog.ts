import { createDialogComposable } from './useDialog'
import type { FileInfo } from '@/types/file'

export interface MoveFileParams {
  files: FileInfo[]
}

export function useMoveFileDialog() {
  return createDialogComposable<MoveFileParams>()
}
