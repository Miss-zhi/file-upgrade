<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'

defineProps<{
  fileName: string
  label?: string
  showDownload?: boolean
  downloadUrl?: string
}>()

const emit = defineEmits<{
  close: []
  download: []
}>()
</script>

<template>
  <div class="preview-top-bar">
    <div class="top-bar-left">
      <span class="file-name">{{ fileName }}</span>
      <span v-if="label" class="preview-label">{{ label }}</span>
    </div>
    <div class="top-bar-right">
      <el-link
        v-if="showDownload"
        :underline="false"
        class="action-link"
        @click="emit('download')"
      >
        下载
      </el-link>
      <el-link :underline="false" class="action-link close-btn" @click="emit('close')">
        <el-icon :size="20"><Close /></el-icon>
      </el-link>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.preview-top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.file-name {
  font-size: 16px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-label {
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-link {
  color: #fff !important;
  font-size: 14px;

  &:hover {
    opacity: 0.8;
  }
}

.close-btn {
  font-size: 20px;
}
</style>
