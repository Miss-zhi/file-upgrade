<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSearchHealth, rebuildSearchIndex } from '@/api/search'
import type { SearchHealthVO } from '@/types/search'

const health = ref<SearchHealthVO | null>(null)
const healthLoading = ref(false)
const rebuilding = ref(false)

async function checkHealth() {
  healthLoading.value = true
  try {
    health.value = await getSearchHealth()
  } catch {
    ElMessage.error('获取搜索服务状态失败')
  } finally {
    healthLoading.value = false
  }
}

async function handleRebuild() {
  try {
    await ElMessageBox.confirm(
      '重建索引将清空现有索引并重新导入所有文件数据，耗时较长，期间搜索功能不可用。确认继续？',
      '重建搜索索引',
      { confirmButtonText: '确认重建', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  rebuilding.value = true
  try {
    await rebuildSearchIndex()
    ElMessage.success('索引重建完成')
    await checkHealth()
  } catch (err: any) {
    ElMessage.error(err.message || '索引重建失败')
  } finally {
    rebuilding.value = false
  }
}

onMounted(() => {
  checkHealth()
})
</script>

<template>
  <div class="admin-search-index">
    <div class="section-card">
      <div class="card-header">
        <span class="card-title">搜索服务状态</span>
        <el-button :loading="healthLoading" @click="checkHealth">刷新</el-button>
      </div>
      <div class="status-row">
        <el-tag v-if="health" :type="health.available ? 'success' : 'danger'" size="large">
          {{ health.available ? '正常' : '异常' }}
        </el-tag>
        <span v-if="health" class="status-text">{{ health.status }}</span>
        <span v-else class="status-text">加载中...</span>
      </div>
    </div>

    <div class="section-card">
      <div class="card-header">
        <span class="card-title">重建索引</span>
      </div>
      <p class="section-desc">
        当 Elasticsearch 索引数据与数据库不一致时（如开发调试、ES 容器重建后），
        可通过此功能将所有用户文件重新导入搜索索引。
      </p>
      <el-button
        type="warning"
        :loading="rebuilding"
        @click="handleRebuild"
      >
        {{ rebuilding ? '重建中...' : '重建索引' }}
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-search-index {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  border: 1px solid $border-lighter;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: $primary-text;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-text {
  font-size: 14px;
  color: $regular-text;
}

.section-desc {
  font-size: 13px;
  color: $secondary-text;
  line-height: 1.6;
  margin-bottom: 16px;
}
</style>
