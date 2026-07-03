<script setup lang="ts">
import { computed } from 'vue'
import { useUploadFileStore } from '@/stores/uploadFile'
import { formatFileSize } from '@/utils/file'

const uploadFileStore = useUploadFileStore()

const isCollapsed = computed({
  get: () => uploadFileStore.isPanelCollapsed,
  set: (val: boolean) => { uploadFileStore.isPanelCollapsed = val },
})

const isVisible = computed({
  get: () => uploadFileStore.isPanelVisible,
  set: (val: boolean) => { uploadFileStore.isPanelVisible = val },
})

function toggleCollapse(): void {
  uploadFileStore.isPanelCollapsed = !uploadFileStore.isPanelCollapsed
}

function closePanel(): void {
  uploadFileStore.isPanelVisible = false
}

function statusText(status: string): string {
  switch (status) {
    case 'pending': return '等待中'
    case 'hashing': return '计算MD5'
    case 'uploading': return '上传中'
    case 'success': return '上传成功'
    case 'error': return '上传失败'
    default: return status
  }
}

function statusColor(status: string): string {
  switch (status) {
    case 'success': return '#67C23A' // $success
    case 'error': return '#F56C6C' // $danger
    default: return '#409EFF' // $primary
  }
}
</script>

<template>
  <div v-if="isVisible && uploadFileStore.uploadQueue.length > 0" class="upload-panel">
    <!-- 标题栏 -->
    <div class="upload-panel-header">
      <span class="upload-panel-title">
        上传列表 ({{ uploadFileStore.activeTaskCount }}/{{ uploadFileStore.uploadQueue.length }})
      </span>
      <div class="upload-panel-actions">
        <el-icon @click="toggleCollapse">
          <ArrowDown v-if="!isCollapsed" />
          <ArrowUp v-else />
        </el-icon>
        <el-icon @click="closePanel"><Close /></el-icon>
      </div>
    </div>

    <!-- 文件列表 -->
    <div v-show="!isCollapsed" class="upload-panel-body">
      <div
        v-for="task in uploadFileStore.uploadQueue"
        :key="task.id"
        class="upload-task-item"
      >
        <div class="task-info">
          <span class="task-name" :title="task.fileName">{{ task.fileName }}</span>
          <span class="task-size">{{ formatFileSize(task.fileSize) }}</span>
        </div>
        <el-progress
          :percentage="task.progress"
          :status="task.status === 'success' ? 'success' : task.status === 'error' ? 'exception' : ''"
          :stroke-width="4"
          :show-text="false"
        />
        <div class="task-status" :style="{ color: statusColor(task.status) }">
          {{ statusText(task.status) }}
          <span v-if="task.errorMsg" class="task-error">{{ task.errorMsg }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.upload-panel {
  position: fixed;
  right: 16px;
  bottom: 16px;
  width: 560px;
  z-index: 20;
  background: #fff;
  border: 1px solid $border-light;
  border-radius: 7px 7px 0 0;
  box-shadow: $tab-box-shadow;
}

.upload-panel-header {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: $tab-back-color;
  border-bottom: 1px solid $border-light;
  cursor: pointer;
}

.upload-panel-title {
  font-size: 13px;
  color: $primary-text;
}

.upload-panel-actions {
  display: flex;
  gap: 8px;

  .el-icon {
    cursor: pointer;
    font-size: 16px;
    color: $secondary-text;

    &:hover {
      color: $primary;
    }
  }
}

.upload-panel-body {
  max-height: 240px;
  overflow-y: auto;
  padding: 8px 12px;
}

.upload-task-item {
  padding: 8px 0;
  border-bottom: 1px solid $border-extralight;

  &:last-child {
    border-bottom: none;
  }
}

.task-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.task-name {
  font-size: 13px;
  color: $primary-text;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 400px;
}

.task-size {
  font-size: 12px;
  color: $secondary-text;
  flex-shrink: 0;
}

.task-status {
  font-size: 12px;
  margin-top: 2px;
}

.task-error {
  color: $danger;
  margin-left: 8px;
}
</style>
