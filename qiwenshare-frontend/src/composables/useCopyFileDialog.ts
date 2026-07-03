import { createDialogComposable } from './useDialog'
import type { FileInfo } from '@/types/file'

export function useCopyFileDialog() {
  return createDialogComposable<FileInfo>()
}
