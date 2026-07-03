import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, getUserQuota, setUserQuota, batchSetQuota } from '@/api/admin'
import type { UserListVO, AdminQuotaVO } from '@/types/admin'
import { mbToBytes, formatBytes } from '@/utils/admin'

/** 带配额信息的用户行 */
export interface QuotaUserRow {
  userId: string
  username: string
  totalQuota: number
  usedQuota: number
  availableQuota: number
}

/**
 * 配额管理页面 composable。
 * 封装配额列表获取、单用户配额修改、批量配额设置逻辑。
 */
export function useAdminQuota() {
  const quotaUsers = ref<QuotaUserRow[]>([])
  const loading = ref(false)
  const keyword = ref('')
  const page = ref(0)
  const pageSize = ref(20)
  const total = ref(0)

  // 多选
  const selectedUsers = ref<QuotaUserRow[]>([])

  // 单用户修改对话框
  const editVisible = ref(false)
  const editingUser = ref<QuotaUserRow | null>(null)
  const newQuotaMB = ref(0)
  const editLoading = ref(false)

  // 批量修改对话框
  const batchVisible = ref(false)
  const batchQuotaMB = ref(0)
  const batchLoading = ref(false)

  /**
   * 加载配额列表（先查用户列表，再并行取配额）。
   * 注意：此处存在 N+1 查询模式（1 次 listUsers + N 次 getUserQuota）。
   * 后端暂无批量获取配额列表接口，当前方案在默认 pageSize=20 下可接受。
   */
  async function loadQuotaList(): Promise<void> {
    loading.value = true
    try {
      const result = await listUsers({
        keyword: keyword.value || undefined,
        page: page.value,
        pageSize: pageSize.value,
      })
      total.value = result.totalElements

      // 并行获取配额信息
      const rows: QuotaUserRow[] = []
      const quotaPromises = result.content.map(async (user: UserListVO) => {
        try {
          const quota = await getUserQuota(user.userId)
          rows.push({
            userId: user.userId,
            username: user.username,
            totalQuota: quota.totalQuota,
            usedQuota: quota.usedQuota,
            availableQuota: quota.availableQuota,
          })
        } catch {
          rows.push({
            userId: user.userId,
            username: user.username,
            totalQuota: 0,
            usedQuota: 0,
            availableQuota: 0,
          })
        }
      })
      await Promise.all(quotaPromises)
      quotaUsers.value = rows
    } catch {
      ElMessage.error('加载配额列表失败')
    } finally {
      loading.value = false
    }
  }

  function onSearch(): void {
    page.value = 0
    loadQuotaList()
  }

  function onPageChange(newPage: number): void {
    page.value = newPage
    loadQuotaList()
  }

  function onSizeChange(newSize: number): void {
    pageSize.value = newSize
    page.value = 0
    loadQuotaList()
  }

  /** 多选变化 */
  function onSelectionChange(rows: QuotaUserRow[]): void {
    selectedUsers.value = rows
  }

  // ---- 单用户修改 ----

  function openEditDialog(user: QuotaUserRow): void {
    editingUser.value = user
    newQuotaMB.value = Math.round(user.totalQuota / (1024 * 1024))
    editVisible.value = true
  }

  async function submitSingleQuota(): Promise<void> {
    if (!editingUser.value || newQuotaMB.value <= 0) {
      ElMessage.warning('配额必须大于 0')
      return
    }
    editLoading.value = true
    try {
      await setUserQuota(editingUser.value.userId, {
        totalQuota: mbToBytes(newQuotaMB.value),
      })
      ElMessage.success('配额修改成功')
      editVisible.value = false
      await loadQuotaList()
    } catch {
      ElMessage.error('配额修改失败')
    } finally {
      editLoading.value = false
    }
  }

  // ---- 批量修改 ----

  function openBatchDialog(): void {
    if (selectedUsers.value.length === 0) {
      ElMessage.warning('请至少选择一个用户')
      return
    }
    batchQuotaMB.value = 0
    batchVisible.value = true
  }

  async function submitBatchQuota(): Promise<void> {
    if (batchQuotaMB.value <= 0) {
      ElMessage.warning('配额必须大于 0')
      return
    }
    batchLoading.value = true
    try {
      const bytes = mbToBytes(batchQuotaMB.value)
      const items = selectedUsers.value.map((u) => ({
        userId: u.userId,
        totalQuota: bytes,
      }))
      const skipped = await batchSetQuota({ items })
      if (skipped.length > 0) {
        ElMessage.warning(`部分用户设置失败：${skipped.join(', ')}`)
      } else {
        ElMessage.success('批量配额设置成功')
      }
      batchVisible.value = false
      await loadQuotaList()
    } catch {
      ElMessage.error('批量配额设置失败')
    } finally {
      batchLoading.value = false
    }
  }

  return {
    quotaUsers,
    loading,
    keyword,
    page,
    pageSize,
    total,
    selectedUsers,
    loadQuotaList,
    onSearch,
    onPageChange,
    onSizeChange,
    onSelectionChange,
    editVisible,
    editingUser,
    newQuotaMB,
    editLoading,
    openEditDialog,
    submitSingleQuota,
    batchVisible,
    batchQuotaMB,
    batchLoading,
    openBatchDialog,
    submitBatchQuota,
    formatBytes,
  }
}
