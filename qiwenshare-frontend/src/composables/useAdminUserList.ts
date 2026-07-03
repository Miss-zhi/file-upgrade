import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listUsers,
  getUserDetail,
  enableUser,
  disableUser,
  resetUserPassword,
  getUserQuota,
  setUserQuota,
} from '@/api/admin'
import type { UserListVO, UserDetailVO, AdminQuotaVO } from '@/types/admin'
import { mbToBytes, formatBytes, calcUsagePercent, usageColor } from '@/utils/admin'

/** 带配额信息的用户行（用于表格展示） */
export interface UserRowWithQuota extends UserListVO {
  usedQuota: number
  totalQuota: number
}

/**
 * 用户管理页面 composable。
 * 封装用户列表数据获取、分页、搜索、启用/禁用、重置密码、修改配额逻辑。
 */
export function useAdminUserList() {
  const users = ref<UserRowWithQuota[]>([])
  const loading = ref(false)
  const keyword = ref('')
  const page = ref(0)
  const pageSize = ref(20)
  const total = ref(0)

  // ---- 对话框状态 ----
  const detailVisible = ref(false)
  const detailUser = ref<UserDetailVO | null>(null)
  const detailLoading = ref(false)

  const quotaVisible = ref(false)
  const quotaUser = ref<UserListVO | null>(null)
  const currentQuota = ref<AdminQuotaVO | null>(null)
  const quotaLoading = ref(false)
  const newQuotaMB = ref(0)

  const passwordVisible = ref(false)
  const passwordUser = ref<UserListVO | null>(null)
  const newPassword = ref('Admin@123')

  /** 加载用户列表（含配额信息，用于存储用量展示） */
  async function loadUsers(): Promise<void> {
    loading.value = true
    try {
      const result = await listUsers({
        keyword: keyword.value || undefined,
        page: page.value,
        pageSize: pageSize.value,
      })
      total.value = result.totalElements

      // 并行获取配额信息，合并到用户行数据
      const rows: UserRowWithQuota[] = []
      const quotaPromises = result.content.map(async (user: UserListVO) => {
        try {
          const quota = await getUserQuota(user.userId)
          rows.push({
            ...user,
            usedQuota: quota.usedQuota,
            totalQuota: quota.totalQuota,
          })
        } catch {
          rows.push({
            ...user,
            usedQuota: 0,
            totalQuota: 0,
          })
        }
      })
      await Promise.all(quotaPromises)
      users.value = rows
    } catch {
      ElMessage.error('加载用户列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 搜索 */
  function onSearch(): void {
    page.value = 0
    loadUsers()
  }

  /** 翻页 */
  function onPageChange(newPage: number): void {
    page.value = newPage
    loadUsers()
  }

  /** 每页条数变化 */
  function onSizeChange(newSize: number): void {
    pageSize.value = newSize
    page.value = 0
    loadUsers()
  }

  // ---- 用户详情 ----

  async function openDetail(user: UserListVO): Promise<void> {
    detailLoading.value = true
    detailVisible.value = true
    detailUser.value = null
    try {
      detailUser.value = await getUserDetail(user.userId)
    } catch {
      ElMessage.error('加载用户详情失败')
      detailVisible.value = false
    } finally {
      detailLoading.value = false
    }
  }

  // ---- 启用/禁用 ----

  async function toggleUserStatus(user: UserListVO): Promise<void> {
    const action = user.available === 1 ? '禁用' : '启用'
    try {
      await ElMessageBox.confirm(
        `确定要${action}用户「${user.username}」吗？`,
        `${action}确认`,
        { type: 'warning' },
      )
      if (user.available === 1) {
        await disableUser(user.userId)
      } else {
        await enableUser(user.userId)
      }
      ElMessage.success(`${action}成功`)
      await loadUsers()
    } catch {
      // 用户取消
    }
  }

  // ---- 修改配额 ----

  async function openQuotaDialog(user: UserListVO): Promise<void> {
    quotaUser.value = user
    quotaVisible.value = true
    currentQuota.value = null
    newQuotaMB.value = 0

    quotaLoading.value = true
    try {
      currentQuota.value = await getUserQuota(user.userId)
      // 转成 MB 预填
      newQuotaMB.value = Math.round(currentQuota.value.totalQuota / (1024 * 1024))
    } catch {
      ElMessage.error('获取配额信息失败')
      quotaVisible.value = false
    } finally {
      quotaLoading.value = false
    }
  }

  /** 提交配额修改 */
  async function submitQuotaChange(): Promise<void> {
    if (!quotaUser.value || newQuotaMB.value <= 0) {
      ElMessage.warning('配额必须大于 0')
      return
    }
    const bytes = mbToBytes(newQuotaMB.value)
    try {
      await setUserQuota(quotaUser.value.userId, { totalQuota: bytes })
      ElMessage.success('配额修改成功')
      quotaVisible.value = false
      await loadUsers()
    } catch {
      ElMessage.error('配额修改失败')
    }
  }

  // ---- 重置密码 ----

  function openPasswordDialog(user: UserListVO): void {
    passwordUser.value = user
    newPassword.value = 'Admin@123'
    passwordVisible.value = true
  }

  async function submitResetPassword(): Promise<void> {
    if (!passwordUser.value) return
    const pwd = newPassword.value
    if (pwd.length < 8 || pwd.length > 30) {
      ElMessage.warning('密码长度应在 8-30 位之间')
      return
    }
    if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(pwd)) {
      ElMessage.warning('密码须包含大写字母、小写字母和数字')
      return
    }
    try {
      await resetUserPassword(passwordUser.value.userId, {
        newPassword: newPassword.value,
      })
      ElMessage.success('密码重置成功')
      passwordVisible.value = false
    } catch {
      ElMessage.error('密码重置失败')
    }
  }

  return {
    // 列表
    users,
    loading,
    keyword,
    page,
    pageSize,
    total,
    loadUsers,
    onSearch,
    onPageChange,
    onSizeChange,
    // 详情
    detailVisible,
    detailUser,
    detailLoading,
    openDetail,
    // 启用/禁用
    toggleUserStatus,
    // 配额
    quotaVisible,
    quotaLoading,
    currentQuota,
    newQuotaMB,
    openQuotaDialog,
    submitQuotaChange,
    // 密码
    passwordVisible,
    newPassword,
    openPasswordDialog,
    submitResetPassword,
    // 工具
    formatBytes,
    calcUsagePercent,
    usageColor,
  }
}
