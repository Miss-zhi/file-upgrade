import { createDialogComposable } from './useDialog'
import type { FileInfo } from '@/types/file'

export function useRenameDialog() {
  return createDialogComposable<FileInfo>()
}
