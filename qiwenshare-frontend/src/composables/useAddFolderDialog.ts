import { createDialogComposable } from './useDialog'

export interface AddFolderParams {
  filePath: string
}

export function useAddFolderDialog() {
  return createDialogComposable<AddFolderParams>()
}
