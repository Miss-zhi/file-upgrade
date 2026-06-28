<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStats, getConfig, saveConfig } from '_api/admin'
import { ElMessage } from 'element-plus'

const stats = ref({ fileCount: 0, totalSize: 0, userCount: 0 })
const config = ref<Record<string, string>>({})

async function fetchStats() {
  const res: any = await getStats()
  if (res.success) stats.value = res.data
}

async function fetchConfig() {
  const res: any = await getConfig()
  if (res.success) config.value = res.data || {}
}

async function handleSaveConfig() {
  const res: any = await saveConfig(config.value)
  if (res.success) ElMessage.success('配置已保存')
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes, u = 0
  while (size >= 1024 && u < units.length - 1) { size /= 1024; u++ }
  return size.toFixed(1) + ' ' + units[u]
}

onMounted(() => { fetchStats(); fetchConfig() })
</script>

<template>
  <div class="dashboard">
    <h2>管理面板</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#409eff"><Document /></el-icon>
            <div>
              <div class="stat-value">{{ stats.fileCount }}</div>
              <div class="stat-label">文件总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#67c23a"><Coin /></el-icon>
            <div>
              <div class="stat-value">{{ formatSize(stats.totalSize) }}</div>
              <div class="stat-label">存储用量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="36" color="#e6a23c"><User /></el-icon>
            <div>
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">注册用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统配置 -->
    <el-card class="config-card">
      <template #header>系统配置</template>
      <el-form label-width="100px">
        <el-form-item label="站点名称">
          <el-input v-model="config.siteName" placeholder="奇文网盘" />
        </el-form-item>
        <el-form-item label="存储类型">
          <el-select v-model="config.storageType" style="width:200px">
            <el-option value="local" label="本地存储" />
            <el-option value="aliyun" label="阿里云 OSS" />
            <el-option value="minio" label="MinIO" />
            <el-option value="qiniu" label="七牛云" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style lang="stylus" scoped>
.dashboard
  padding: 24px

  h2
    margin-bottom: 20px

  .stat-cards
    margin-bottom: 24px

    .stat-card
      display: flex
      align-items: center
      gap: 16px

      .stat-value
        font-size: 28px
        font-weight: bold
        color: #303133

      .stat-label
        font-size: 14px
        color: #909399
        margin-top: 4px

  .config-card
    max-width: 500px
</style>
