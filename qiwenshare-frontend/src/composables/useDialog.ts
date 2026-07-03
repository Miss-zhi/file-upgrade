import { ref } from 'vue'

/**
 * 通用弹窗 composable 工厂。
 * 返回 visible/open/confirm/cancel 方法。
 */
export function createDialogComposable<T = void>() {
  const visible = ref(false)
  const paramsRef = ref<T | null>(null)
  const resolveRef = ref<(value: boolean) => void>()

  function open(params?: T): Promise<boolean> {
    paramsRef.value = (params ?? null) as T | null
    visible.value = true
    return new Promise((resolve) => {
      resolveRef.value = resolve
    })
  }

  function confirm(): void {
    visible.value = false
    resolveRef.value?.(true)
  }

  function cancel(): void {
    visible.value = false
    resolveRef.value?.(false)
  }

  return { visible, paramsRef, open, confirm, cancel }
}
