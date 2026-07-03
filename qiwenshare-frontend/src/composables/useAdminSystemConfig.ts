import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigs, createConfig, updateConfig, deleteConfig } from '@/api/admin'
import type { ConfigVO, CreateConfigRequest, UpdateConfigRequest } from '@/types/admin'

/**
 * 系统配置管理页面 composable。
 * 封装配置列表获取、搜索、新增、编辑、删除逻辑。
 */
export function useAdminSystemConfig() {
  const configs = ref<ConfigVO[]>([])
  const loading = ref(false)
  const keyword = ref('')
  const page = ref(0)
  const pageSize = ref(20)
  const total = ref(0)

  // 新增/编辑对话框
  const formVisible = ref(false)
  const isEditing = ref(false)
  const editingId = ref<number | null>(null)
  const form = ref<CreateConfigRequest>({
    configKey: '',
    configValue: '',
    description: '',
  })
  const formLoading = ref(false)

  /** 加载配置列表 */
  async function loadConfigs(): Promise<void> {
    loading.value = true
    try {
      const result = await listConfigs({
        keyword: keyword.value || undefined,
        page: page.value,
        pageSize: pageSize.value,
      })
      configs.value = result.content
      total.value = result.totalElements
    } catch {
      ElMessage.error('加载配置列表失败')
    } finally {
      loading.value = false
    }
  }

  function onSearch(): void {
    page.value = 0
    loadConfigs()
  }

  function onPageChange(newPage: number): void {
    page.value = newPage
    loadConfigs()
  }

  function onSizeChange(newSize: number): void {
    pageSize.value = newSize
    page.value = 0
    loadConfigs()
  }

  // ---- 新增 ----

  function openCreateDialog(): void {
    isEditing.value = false
    editingId.value = null
    form.value = { configKey: '', configValue: '', description: '' }
    formVisible.value = true
  }

  // ---- 编辑 ----

  function openEditDialog(config: ConfigVO): void {
    isEditing.value = true
    editingId.value = config.id
    form.value = {
      configKey: config.configKey,
      configValue: config.configValue,
      description: config.description,
    }
    formVisible.value = true
  }

  /** 提交表单（新增或编辑） */
  async function submitForm(): Promise<void> {
    if (!form.value.configKey || !form.value.configValue) {
      ElMessage.warning('配置键和配置值不能为空')
      return
    }
    formLoading.value = true
    try {
      if (isEditing.value && editingId.value !== null) {
        await updateConfig(editingId.value, {
          configValue: form.value.configValue,
          description: form.value.description,
        })
        ElMessage.success('配置更新成功')
      } else {
        await createConfig({
          configKey: form.value.configKey,
          configValue: form.value.configValue,
          description: form.value.description,
        })
        ElMessage.success('配置添加成功')
      }
      formVisible.value = false
      await loadConfigs()
    } catch {
      ElMessage.error(isEditing.value ? '配置更新失败' : '配置添加失败')
    } finally {
      formLoading.value = false
    }
  }

  // ---- 删除 ----

  async function handleDelete(config: ConfigVO): Promise<void> {
    try {
      await ElMessageBox.confirm(
        `确定要删除配置「${config.configKey}」吗？此操作不可恢复。`,
        '删除确认',
        { type: 'warning' },
      )
      await deleteConfig(config.id)
      ElMessage.success('配置已删除')
      await loadConfigs()
    } catch {
      // 用户取消
    }
  }

  return {
    configs,
    loading,
    keyword,
    page,
    pageSize,
    total,
    loadConfigs,
    onSearch,
    onPageChange,
    onSizeChange,
    formVisible,
    isEditing,
    form,
    formLoading,
    openCreateDialog,
    openEditDialog,
    submitForm,
    handleDelete,
  }
}
