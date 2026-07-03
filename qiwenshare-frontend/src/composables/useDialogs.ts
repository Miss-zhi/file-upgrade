import { createDialogComposable } from './useDialog'
import type { FileInfo, ShareInfo } from '@/types/file'

export function useShareDialog() {
  return createDialogComposable<FileInfo>()
}

export function useFileDetailDialog() {
  return createDialogComposable<FileInfo>()
}

export interface RestoreParams {
  userFileIds: number[]
}

export function useRestoreDialog() {
  return createDialogComposable<RestoreParams>()
}

export function useUnzipDialog() {
  return createDialogComposable<FileInfo>()
}

export function useSaveShareDialog() {
  return createDialogComposable<{ shareCode: string; fileName: string }>()
}
