<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminSystemConfig } from '@/composables/useAdminSystemConfig'

const {
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
} = useAdminSystemConfig()

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="admin-system-config">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索配置键或描述"
        clearable
        style="width: 260px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button type="success" @click="openCreateDialog">新增配置</el-button>
    </div>

    <!-- 配置表格 -->
    <el-table
      :data="configs"
      v-loading="loading"
      border
      stripe
      class="config-table"
    >
      <el-table-column prop="configKey" label="配置键" min-width="180" show-overflow-tooltip />
      <el-table-column prop="configValue" label="配置值" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column prop="updateTime" label="更新时间" width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            编辑
          </el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row)">
            删除
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

    <!-- 新增/编辑配置对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="isEditing ? '编辑配置' : '新增配置'"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="配置键" required>
          <el-input
            v-model="form.configKey"
            placeholder="如 upload.max_size"
            :disabled="isEditing"
            maxlength="100"
          />
          <template v-if="isEditing">
            <span class="form-hint">配置键不可修改</span>
          </template>
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-model="form.configValue"
            placeholder="配置值"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="配置项描述（可选）"
            maxlength="200"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-system-config {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.config-table {
  flex: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.form-hint {
  font-size: 12px;
  color: $secondary-text;
}
</style>
