<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminRoleList } from '@/composables/useAdminRoleList'

const {
  roles,
  loading,
  loadRoles,
  getPermissionCount,
  editVisible,
  editingRole,
  checkedPermIds,
  editLoading,
  permissionTree,
  openEditPermissions,
  submitPermissions,
} = useAdminRoleList()

onMounted(() => {
  loadRoles()
})

/** el-tree 节点 key */
function getNodeKey(data: { permissionId: number }): number {
  return data.permissionId
}
</script>

<template>
  <div class="admin-role-list">
    <el-table
      :data="roles"
      v-loading="loading"
      border
      stripe
      class="role-table"
    >
      <el-table-column prop="roleName" label="角色名称" min-width="150" />
      <el-table-column prop="roleDesc" label="角色描述" min-width="200" />
      <el-table-column label="权限数量" width="100" align="center">
        <template #default="{ row }">
          {{ getPermissionCount(row) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.available === 1 ? 'success' : 'danger'" size="small">
            {{ row.available === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditPermissions(row)">
            编辑权限
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 权限编辑对话框 -->
    <el-dialog
      v-model="editVisible"
      title="编辑角色权限"
      width="480px"
      destroy-on-close
    >
      <template v-if="editingRole">
        <p class="role-hint">
          角色：<strong>{{ editingRole.roleName }}</strong>
        </p>
        <el-tree
          :data="permissionTree"
          show-checkbox
          node-key="permissionId"
          :default-checked-keys="checkedPermIds"
          :props="{ label: 'permName', children: 'children' }"
          default-expand-all
          check-strictly
        />
      </template>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitPermissions">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-role-list {
  height: 100%;
}

.role-hint {
  margin-bottom: 16px;
  color: $regular-text;
}
</style>
