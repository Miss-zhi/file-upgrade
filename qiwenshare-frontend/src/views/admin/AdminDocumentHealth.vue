<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDocumentHealth } from '@/api/search'
import type { DocumentHealthVO } from '@/types/search'

const health = ref<DocumentHealthVO | null>(null)
const loading = ref(false)

async function checkHealth() {
  loading.value = true
  try {
    health.value = await getDocumentHealth()
  } catch {
    ElMessage.error('健康检查请求失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  checkHealth()
})
</script>

<template>
  <div class="admin-document-health">
    <div class="section-card">
      <div class="card-header">
        <span class="card-title">OnlyOffice 文档服务</span>
        <el-button :loading="loading" @click="checkHealth">重新检查</el-button>
      </div>

      <template v-if="health">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="服务状态">
            <el-tag :type="health.status === 'UP' ? 'success' : 'danger'" size="large">
              {{ health.status === 'UP' ? '正常运行' : '不可用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务地址">
            <code class="server-url">{{ health.serverUrl }}</code>
          </el-descriptions-item>
          <el-descriptions-item v-if="health.error" label="错误信息">
            <span class="error-text">{{ health.error }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="health.status === 'DOWN'" class="tip-box">
          <el-icon class="tip-icon"><WarningFilled /></el-icon>
          <span>文档预览和在线编辑功能当前不可用，请检查 OnlyOffice 容器是否正常运行。</span>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
        <span>正在检查服务状态...</span>
      </div>
    </div>

    <div class="section-card">
      <div class="card-header">
        <span class="card-title">功能说明</span>
      </div>
      <p class="section-desc">
        此页面用于检测 OnlyOffice Document Server 的运行状态。
        OnlyOffice 提供文档在线预览和协同编辑功能，支持 Word、Excel、PPT 等格式。
        当状态为"不可用"时，用户无法预览或编辑 Office 文档，但文件上传下载等基础功能不受影响。
      </p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-document-health {
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

.server-url {
  font-family: monospace;
  font-size: 13px;
  color: $primary;
  background: $primary-hover;
  padding: 2px 8px;
  border-radius: 3px;
}

.error-text {
  color: $danger;
  font-size: 13px;
}

.tip-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: $warning-hover;
  border-radius: 4px;
  font-size: 13px;
  color: $warning;
}

.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: $secondary-text;
  gap: 12px;
}

.loading-icon {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.section-desc {
  font-size: 13px;
  color: $secondary-text;
  line-height: 1.6;
  margin: 0;
}
</style>
