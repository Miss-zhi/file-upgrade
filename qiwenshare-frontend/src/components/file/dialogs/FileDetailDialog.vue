<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FileInfo, FileDetail } from '@/types/file'
import { getFileDetail } from '@/api/file'
import { formatFileSize, getFileIconSrc } from '@/utils/file'

const props = defineProps<{
  visible: boolean
  file: FileInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const detail = ref<FileDetail | null>(null)
const loading = ref(false)

watch(() => props.visible, async (val) => {
  if (val && props.file) {
    loading.value = true
    try {
      detail.value = await getFileDetail(props.file.userFileId)
    } finally {
      loading.value = false
    }
  } else {
    detail.value = null
  }
})

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="文件详情"
    width="550px"
    @close="handleClose"
  >
    <div v-loading="loading" class="file-detail">
      <template v-if="detail">
        <div class="detail-row">
          <img :src="getFileIconSrc(detail.extendName, detail.fileType, detail.userFileId)" class="detail-icon" />
          <span class="detail-name">{{ detail.fileName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">路径：</span>
          <span>{{ detail.filePath }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">类型：</span>
          <span>{{ detail.extendName || '文件夹' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">大小：</span>
          <span>{{ formatFileSize(detail.fileSize) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">上传时间：</span>
          <span>{{ detail.uploadTime }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">修改时间：</span>
          <span>{{ detail.modifyTime }}</span>
        </div>
        <div v-if="detail.fileHash" class="detail-item">
          <span class="detail-label">Hash：</span>
          <span class="detail-hash">{{ detail.fileHash }}</span>
        </div>
        <div v-if="detail.storageType" class="detail-item">
          <span class="detail-label">存储位置：</span>
          <span>{{ detail.storageType }}</span>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.file-detail {
  min-height: 200px;
}
.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.detail-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
}
.detail-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.detail-item {
  display: flex;
  padding: 6px 0;
  font-size: 13px;
}
.detail-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}
.detail-hash {
  word-break: break-all;
  font-family: monospace;
  font-size: 12px;
}
</style>
