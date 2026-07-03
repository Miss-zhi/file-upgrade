import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 公共应用状态管理。
 */
export const useCommonStore = defineStore('common', () => {
  const screenWidth = ref<number>(document.body.clientWidth)

  /**
   * 更新屏幕宽度为当前 document.body.clientWidth。
   */
  function updateScreenWidth(): void {
    screenWidth.value = document.body.clientWidth
  }

  return {
    screenWidth,
    updateScreenWidth,
  }
})
