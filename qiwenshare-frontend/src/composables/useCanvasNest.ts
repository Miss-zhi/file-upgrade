import { onMounted, onBeforeUnmount, type Ref } from 'vue'
import CanvasNest from 'canvas-nest.js'

/**
 * canvas-nest.js 粒子背景动画 composable。
 * 在 onMounted 后初始化，onBeforeUnmount 前销毁。
 */
export function useCanvasNest(
  elRef: Ref<HTMLElement | null>,
  color: string = '64,158,255',
  count: number = 99,
): void {
  let instance: InstanceType<typeof CanvasNest> | null = null

  onMounted(() => {
    if (!elRef.value) return
    instance = new CanvasNest(elRef.value, {
      color,
      pointColor: color,
      count,
      opacity: 0.7,
      zIndex: 0,
    })
  })

  onBeforeUnmount(() => {
    if (instance) {
      instance.destroy()
      instance = null
    }
  })
}
