import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { listRoles, updateRolePermissions } from '@/api/admin'
import { ADMIN_PERMISSION_TREE } from '@/types/admin'
import type { RoleVO } from '@/types/admin'

/**
 * 角色权限管理页面 composable。
 * 封装角色列表获取、权限树数据构建、权限更新逻辑。
 */
export function useAdminRoleList() {
  const roles = ref<RoleVO[]>([])
  const loading = ref(false)

  // 权限编辑对话框
  const editVisible = ref(false)
  const editingRole = ref<RoleVO | null>(null)
  const checkedPermIds = ref<number[]>([])
  const editLoading = ref(false)

  /** 权限树（所有可用权限） */
  const permissionTree = ADMIN_PERMISSION_TREE

  /** 所有叶子节点的 ID（用于父节点半选判断） */
  const allLeafIds = computed(() => {
    const ids: number[] = []
    for (const node of permissionTree) {
      if (node.children) {
        ids.push(...node.children.map((c) => c.permissionId))
      }
    }
    return ids
  })

  /** 加载角色列表 */
  async function loadRoles(): Promise<void> {
    loading.value = true
    try {
      roles.value = await listRoles()
    } catch {
      ElMessage.error('加载角色列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 获取角色的权限数量 */
  function getPermissionCount(role: RoleVO): number {
    return role.permissions?.length ?? 0
  }

  /** 打开权限编辑对话框 */
  function openEditPermissions(role: RoleVO): void {
    editingRole.value = role
    checkedPermIds.value = [...role.permissions]
    editVisible.value = true
  }

  /** 提交权限更新 */
  async function submitPermissions(): Promise<void> {
    if (!editingRole.value) return
    editLoading.value = true
    try {
      await updateRolePermissions(editingRole.value.roleId, {
        permissionIds: checkedPermIds.value,
      })
      ElMessage.success('权限更新成功')
      editVisible.value = false
      await loadRoles()
    } catch {
      ElMessage.error('权限更新失败')
    } finally {
      editLoading.value = false
    }
  }

  return {
    roles,
    loading,
    loadRoles,
    getPermissionCount,
    // 权限编辑
    editVisible,
    editingRole,
    checkedPermIds,
    editLoading,
    permissionTree,
    allLeafIds,
    openEditPermissions,
    submitPermissions,
  }
}
