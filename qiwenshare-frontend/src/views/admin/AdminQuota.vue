<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminQuota } from '@/composables/useAdminQuota'
import { usageColor, calcUsagePercent, formatBytes } from '@/utils/admin'

const {
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
  newQuotaMB,
  editLoading,
  openEditDialog,
  submitSingleQuota,
  batchVisible,
  batchQuotaMB,
  batchLoading,
  openBatchDialog,
  submitBatchQuota,
} = useAdminQuota()

onMounted(() => {
  loadQuotaList()
})
</script>

<template>
  <div class="admin-quota">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="用户名"
        clearable
        style="width: 260px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button
        type="warning"
        :disabled="selectedUsers.length === 0"
        @click="openBatchDialog"
      >
        批量设置配额
      </el-button>
    </div>

    <!-- 配额表格 -->
    <el-table
      :data="quotaUsers"
      v-loading="loading"
      border
      stripe
      class="quota-table"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column label="存储用量" min-width="220">
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
      <el-table-column label="可用空间" min-width="120">
        <template #default="{ row }">
          {{ formatBytes(row.availableQuota) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            修改配额
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

    <!-- 单用户修改配额对话框 -->
    <el-dialog
      v-model="editVisible"
      title="修改配额"
      width="400px"
      destroy-on-close
    >
      <el-form label-width="100px">
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
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitSingleQuota">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量设置配额对话框 -->
    <el-dialog
      v-model="batchVisible"
      title="批量设置配额"
      width="400px"
      destroy-on-close
    >
      <p class="batch-hint">已选择 {{ selectedUsers.length }} 个用户</p>
      <el-form label-width="100px">
        <el-form-item label="统一配额(MB)">
          <el-input-number
            v-model="batchQuotaMB"
            :min="1"
            :step="100"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="submitBatchQuota">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-quota {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.quota-table {
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
}

.batch-hint {
  margin-bottom: 16px;
  color: $regular-text;
}
</style>
