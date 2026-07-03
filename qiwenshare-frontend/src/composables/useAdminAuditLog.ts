import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listLogs } from '@/api/admin'
import type { OperationLogVO, LogQueryParams } from '@/types/admin'

/** 模块选项 */
const MODULE_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'auth', value: 'auth' },
  { label: 'user', value: 'user' },
  { label: 'file', value: 'file' },
  { label: 'share', value: 'share' },
  { label: 'quota', value: 'quota' },
  { label: 'config', value: 'config' },
  { label: 'admin', value: 'admin' },
]

/** 操作类型选项 */
const ACTION_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'CREATE', value: 'CREATE' },
  { label: 'UPDATE', value: 'UPDATE' },
  { label: 'DELETE', value: 'DELETE' },
]

/**
 * 审计日志页面 composable。
 * 封装日志列表获取、多条件筛选（模块、操作类型、用户名、时间范围）、分页逻辑。
 */
export function useAdminAuditLog() {
  const logs = ref<OperationLogVO[]>([])
  const loading = ref(false)

  // 筛选条件
  const filterModule = ref('')
  const filterAction = ref('')
  const filterUsername = ref('')
  const filterTimeRange = ref<[string, string] | null>(null)

  const page = ref(0)
  const pageSize = ref(20)
  const total = ref(0)

  // 详情对话框
  const detailVisible = ref(false)
  const detailLog = ref<OperationLogVO | null>(null)

  /** 构建查询参数 */
  function buildParams(): LogQueryParams {
    const params: LogQueryParams = {
      page: page.value,
      pageSize: pageSize.value,
    }
    if (filterModule.value) params.module = filterModule.value
    if (filterAction.value) params.action = filterAction.value
    if (filterUsername.value) params.username = filterUsername.value
    if (filterTimeRange.value) {
      const [start, end] = filterTimeRange.value
      if (start) params.startTime = `${start}T00:00:00`
      if (end) params.endTime = `${end}T23:59:59`
    }
    return params
  }

  /** 加载日志列表 */
  async function loadLogs(): Promise<void> {
    loading.value = true
    try {
      const result = await listLogs(buildParams())
      logs.value = result.content
      total.value = result.totalElements
    } catch {
      ElMessage.error('加载日志列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 搜索 */
  function onSearch(): void {
    page.value = 0
    loadLogs()
  }

  /** 重置筛选条件 */
  function onReset(): void {
    filterModule.value = ''
    filterAction.value = ''
    filterUsername.value = ''
    filterTimeRange.value = null
    page.value = 0
    loadLogs()
  }

  function onPageChange(newPage: number): void {
    page.value = newPage
    loadLogs()
  }

  function onSizeChange(newSize: number): void {
    pageSize.value = newSize
    page.value = 0
    loadLogs()
  }

  /** 打开详情 */
  function openDetail(log: OperationLogVO): void {
    detailLog.value = log
    detailVisible.value = true
  }

  /** 格式化耗时 */
  function formatDuration(ms: number): string {
    if (ms < 1000) return `${ms}ms`
    return `${(ms / 1000).toFixed(2)}s`
  }

  return {
    logs,
    loading,
    filterModule,
    filterAction,
    filterUsername,
    filterTimeRange,
    page,
    pageSize,
    total,
    moduleOptions: MODULE_OPTIONS,
    actionOptions: ACTION_OPTIONS,
    loadLogs,
    onSearch,
    onReset,
    onPageChange,
    onSizeChange,
    detailVisible,
    detailLog,
    openDetail,
    formatDuration,
  }
}
