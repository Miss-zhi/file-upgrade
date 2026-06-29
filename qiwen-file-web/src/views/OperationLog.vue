<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOperationLogs } from '_api/admin'

const logs = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const operation = ref('')
const dateRange = ref<string[]>([])

const opTypes = ['登录', '注册', '上传', '删除', '下载', '创建文件夹', '分享', '更新', '恢复', '彻底删除']

async function fetchData() {
  const res: any = await getOperationLogs({
    page: page.value,
    size: size.value,
    operation: operation.value || undefined,
    startTime: dateRange.value[0] || undefined,
    endTime: dateRange.value[1] ? dateRange.value[1] + ' 23:59:59' : undefined
  })
  if (res.success) {
    logs.value = res.data.records || []
    total.value = res.data.total || 0
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="log-page">
    <h2>操作日志</h2>

    <el-form inline style="margin-bottom:16px">
      <el-form-item label="操作类型">
        <el-select v-model="operation" placeholder="全部" clearable style="width:160px" @change="fetchData">
          <el-option v-for="o in opTypes" :key="o" :label="o" :value="o" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" @change="fetchData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logs" style="width:100%">
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="operation" label="操作" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.operation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="method" label="接口" min-width="200" />
      <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
      <el-table-column label="耗时" width="80">
        <template #default="{ row }">{{ row.costTime }}ms</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="createTime" label="时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:16px;justify-content:flex-end"
      @current-change="fetchData"
    />
  </div>
</template>

<style lang="stylus" scoped>
.log-page
  padding: 24px
  h2
    margin-bottom: 16px
</style>
