<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminAuditLog } from '@/composables/useAdminAuditLog'

const {
  logs,
  loading,
  filterModule,
  filterAction,
  filterUsername,
  filterTimeRange,
  page,
  pageSize,
  total,
  moduleOptions,
  actionOptions,
  loadLogs,
  onSearch,
  onReset,
  onPageChange,
  onSizeChange,
  detailVisible,
  detailLog,
  openDetail,
  formatDuration,
} = useAdminAuditLog()

onMounted(() => {
  loadLogs()
})
</script>

<template>
  <div class="admin-audit-log">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select
        v-model="filterModule"
        placeholder="模块"
        clearable
        style="width: 130px"
        @change="onSearch"
      >
        <el-option
          v-for="opt in moduleOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select
        v-model="filterAction"
        placeholder="操作类型"
        clearable
        style="width: 130px"
        @change="onSearch"
      >
        <el-option
          v-for="opt in actionOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-input
        v-model="filterUsername"
        placeholder="用户名"
        clearable
        style="width: 160px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-date-picker
        v-model="filterTimeRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="onSearch"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <!-- 日志表格 -->
    <el-table
      :data="logs"
      v-loading="loading"
      border
      stripe
      class="log-table"
    >
      <el-table-column prop="username" label="操作人" min-width="100" />
      <el-table-column prop="module" label="模块" width="80" />
      <el-table-column prop="action" label="操作" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.action === 'DELETE' ? 'danger' : row.action === 'CREATE' ? 'success' : 'warning'">
            {{ row.action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
      <el-table-column prop="requestMethod" label="方法" width="70" />
      <el-table-column prop="requestUri" label="路径" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态码" width="80">
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="row.responseCode >= 400 ? 'danger' : 'success'"
          >
            {{ row.responseCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP" width="130" />
      <el-table-column label="耗时" width="80" align="right">
        <template #default="{ row }">
          {{ formatDuration(row.executionTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" min-width="160" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDetail(row)">
            详情
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

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="600px"
      destroy-on-close
    >
      <template v-if="detailLog">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作人">{{ detailLog.username }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailLog.userId }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detailLog.module }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag size="small">{{ detailLog.action }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailLog.description }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ detailLog.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="请求路径">{{ detailLog.requestUri }}</el-descriptions-item>
          <el-descriptions-item label="响应码">{{ detailLog.responseCode }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatDuration(detailLog.executionTime) }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ detailLog.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detailLog.createTime }}</el-descriptions-item>
          <el-descriptions-item label="请求参数" :span="2">
            <div class="params-pre">{{ detailLog.requestParams || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">
            <div class="params-pre">{{ detailLog.userAgent || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailLog.errorMessage" label="错误信息" :span="2">
            <div class="error-msg">{{ detailLog.errorMessage }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-audit-log {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.log-table {
  flex: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.params-pre {
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: monospace;
  font-size: 12px;
  color: $regular-text;
}

.error-msg {
  color: #f56c6c;
  font-size: 12px;
}
</style>
