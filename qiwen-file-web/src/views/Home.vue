<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHomeStats } from '_api/home'

const router = useRouter()
const userInfo = ref<any>(null)
const recentFiles = ref<any[]>([])
const storage = ref<any>({})

const quickActions = [
  { label: '上传文件', icon: 'Upload', color: '#409eff', path: '/file' },
  { label: '新建文件夹', icon: 'FolderAdd', color: '#67c23a', path: '/file' },
  { label: '回收站', icon: 'Delete', color: '#e6a23c', path: '/recycle' },
  { label: '管理面板', icon: 'Setting', color: '#909399', path: '/dashboard' }
]

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const u = ['B', 'KB', 'MB', 'GB', 'TB']
  let s = bytes, i = 0
  while (s >= 1024 && i < u.length - 1) { s /= 1024; i++ }
  return s.toFixed(1) + ' ' + u[i]
}

onMounted(async () => {
  const res: any = await getHomeStats()
  if (res.success && res.data) {
    userInfo.value = res.data.user
    recentFiles.value = res.data.recentFiles || []
    storage.value = res.data.storage || {}
  }
})
</script>

<template>
  <div class="home-page">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <div class="welcome-content">
        <el-icon :size="48" color="#409eff"><UserFilled /></el-icon>
        <div>
          <h1>欢迎回来，{{ userInfo?.nickname || userInfo?.username || '用户' }}</h1>
          <p class="welcome-sub">奇文网盘 — 安全、高效的文件管理</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top:20px">
      <!-- 快捷操作 -->
      <el-col :span="8">
        <el-card header="快捷操作">
          <div class="quick-actions">
            <div
              v-for="action in quickActions"
              :key="action.label"
              class="action-item"
              @click="router.push(action.path)"
            >
              <el-icon :size="28" :color="action.color">
                <component :is="action.icon" />
              </el-icon>
              <span>{{ action.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 存储统计 -->
      <el-col :span="8">
        <el-card header="存储概览">
          <div class="storage-stats">
            <div class="stat-item">
              <span class="stat-value">{{ storage.fileCount || 0 }}</span>
              <span class="stat-label">文件总数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ formatSize(storage.totalSize || 0) }}</span>
              <span class="stat-label">已用空间</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近文件 -->
      <el-col :span="8">
        <el-card header="最近文件">
          <div class="recent-files">
            <div
              v-for="file in recentFiles.slice(0, 5)"
              :key="file.id"
              class="recent-item"
            >
              <el-icon><Document /></el-icon>
              <span class="recent-name">{{ file.fileName }}</span>
              <span class="recent-size">{{ formatSize(file.fileSize) }}</span>
            </div>
            <el-empty v-if="!recentFiles.length" description="暂无文件" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="stylus" scoped>
.home-page
  padding: 24px

  .welcome-card
    .welcome-content
      display: flex
      align-items: center
      gap: 20px

      h1
        font-size: 24px
        margin: 0 0 4px

      .welcome-sub
        color: #909399
        margin: 0

  .quick-actions
    display: grid
    grid-template-columns: 1fr 1fr
    gap: 16px

    .action-item
      display: flex
      flex-direction: column
      align-items: center
      gap: 8px
      padding: 16px
      border-radius: 8px
      background: #f5f7fa
      cursor: pointer
      transition: background 0.2s

      &:hover
        background: #e6f0ff

      span
        font-size: 13px
        color: #606266

  .storage-stats
    display: flex
    flex-direction: column
    gap: 24px

    .stat-item
      text-align: center

      .stat-value
        display: block
        font-size: 32px
        font-weight: bold
        color: #303133

      .stat-label
        font-size: 14px
        color: #909399

  .recent-files
    .recent-item
      display: flex
      align-items: center
      gap: 8px
      padding: 8px 0
      border-bottom: 1px solid #f0f0f0

      .recent-name
        flex: 1
        font-size: 14px
        overflow: hidden
        text-overflow: ellipsis
        white-space: nowrap

      .recent-size
        font-size: 12px
        color: #909399
</style>
