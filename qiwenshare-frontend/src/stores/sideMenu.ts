import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { getStorage, getQuotaInfo } from '@/api/file'

const LS_IS_COLLAPSE = 'qiwen_is_collapse'

/**
 * 侧边栏状态管理。
 */
export const useSideMenuStore = defineStore('sideMenu', () => {
  const storageValue = ref(0)
  const totalStorageValue = ref(0)

  const isCollapsed = ref<boolean>(
    localStorage.getItem(LS_IS_COLLAPSE) === 'true',
  )

  /** 存储使用百分比 */
  const storagePercentage = computed(() => {
    if (totalStorageValue.value === 0) return 0
    return Math.round((storageValue.value / totalStorageValue.value) * 100)
  })

  /** 切换折叠状态并持久化 */
  function toggleCollapse(): void {
    isCollapsed.value = !isCollapsed.value
    localStorage.setItem(LS_IS_COLLAPSE, String(isCollapsed.value))
  }

  /** 从服务端获取存储容量信息（优先 quota/info，降级 getstorage） */
  async function fetchStorage(): Promise<void> {
    try {
      const quota = await getQuotaInfo()
      storageValue.value = Number(quota.usedSize)
      totalStorageValue.value = Number(quota.totalQuota)
    } catch {
      try {
        const info = await getStorage()
        storageValue.value = Number(info.storageSize)
        totalStorageValue.value = Number(info.totalStorageSize)
      } catch {
        ElMessage.error('获取存储容量失败')
      }
    }
  }

  return {
    storageValue,
    totalStorageValue,
    isCollapsed,
    storagePercentage,
    toggleCollapse,
    fetchStorage,
  }
})
