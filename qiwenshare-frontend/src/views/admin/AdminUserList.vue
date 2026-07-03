<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminUserList } from '@/composables/useAdminUserList'

const {
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
  detailVisible,
  detailUser,
  detailLoading,
  openDetail,
  toggleUserStatus,
  quotaVisible,
  quotaLoading,
  currentQuota,
  newQuotaMB,
  openQuotaDialog,
  submitQuotaChange,
  passwordVisible,
  newPassword,
  openPasswordDialog,
  submitResetPassword,
  formatBytes,
  calcUsagePercent,
  usageColor,
} = useAdminUserList()

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="admin-user-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="用户名 / 手机号"
        clearable
        style="width: 260px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
    </div>

    <!-- 用户表格 -->
    <el-table
      :data="users"
      v-loading="loading"
      border
      stripe
      class="user-table"
    >
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="telephone" label="手机号" min-width="130" />
      <el-table-column label="注册时间" min-width="170">
        <template #default="{ row }">
          {{ row.registerTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.available === 1 ? 'success' : 'danger'" size="small">
            {{ row.available === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="140">
        <template #default="{ row }">
          <template v-if="row.roles && row.roles.length">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="role-tag">
              {{ role }}
            </el-tag>
          </template>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="存储用量" min-width="200">
        <template #default="{ row }">
          <div class="storage-cell">
            <el-progress
              :percentage="calcUsagePercent(row.usedQuota, row.totalQuota)"
              :color="usageColor(calcUsagePercent(row.usedQuota, row.totalQuota))"
              :stroke-width="8"
            />
            <span class="storage-text">
              {{ formatBytes(row.usedQuota) }} / {{ formatBytes(row.totalQuota) }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDetail(row)">
            详情
          </el-button>
          <el-button size="small" type="primary" link @click="toggleUserStatus(row)">
            {{ row.available === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="primary" link @click="openQuotaDialog(row)">
            修改配额
          </el-button>
          <el-button size="small" type="primary" link @click="openPasswordDialog(row)">
            重置密码
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="page + 1"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(p: number) => onPageChange(p - 1)"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="用户详情"
      width="560px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <template v-if="detailUser">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">{{ detailUser.userId }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detailUser.telephone }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detailUser.available === 1 ? 'success' : 'danger'" size="small">
                {{ detailUser.available === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ detailUser.registerTime }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag
                v-for="role in detailUser.roles"
                :key="role.roleId"
                size="small"
                class="role-tag"
              >
                {{ role.roleName }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="权限" :span="2">
              <template v-if="detailUser.permissions && detailUser.permissions.length">
                <el-tag
                  v-for="perm in detailUser.permissions"
                  :key="perm"
                  size="small"
                  type="info"
                  class="role-tag"
                >
                  {{ perm }}
                </el-tag>
              </template>
              <span v-else class="text-muted">无权限</span>
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>

    <!-- 修改配额对话框 -->
    <el-dialog
      v-model="quotaVisible"
      title="修改配额"
      width="420px"
      destroy-on-close
    >
      <div v-loading="quotaLoading">
        <template v-if="currentQuota">
          <el-descriptions :column="1" border style="margin-bottom: 16px">
            <el-descriptions-item label="已用空间">
              {{ formatBytes(currentQuota.usedQuota) }}
            </el-descriptions-item>
            <el-descriptions-item label="当前配额">
              {{ formatBytes(currentQuota.totalQuota) }}
            </el-descriptions-item>
          </el-descriptions>
          <el-form label-width="80px">
            <el-form-item label="新配额(MB)">
              <el-input-number
                v-model="newQuotaMB"
                :min="1"
                :step="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>
        </template>
      </div>
      <template #footer>
        <el-button @click="quotaVisible = false">取消</el-button>
        <el-button type="primary" :disabled="newQuotaMB <= 0" @click="submitQuotaChange">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="passwordVisible"
      title="重置密码"
      width="400px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input
            v-model="newPassword"
            show-password
            maxlength="30"
            placeholder="8-30位，含大小写字母和数字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-user-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 900px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.user-table {
  flex: 1;
}

.storage-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.storage-text {
  font-size: 12px;
  color: $secondary-text;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.role-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}

.text-muted {
  color: $secondary-text;
}
</style>
