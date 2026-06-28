<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile } from 'element-plus'

const visible = ref(false)
const currentPath = ref('/')
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)

const emit = defineEmits<{
  confirm: [file: File, path: string]
}>()

function open(path: string) {
  currentPath.value = path || '/'
  fileList.value = []
  visible.value = true
}

function handleFileChange(file: UploadFile) {
  fileList.value = [file]
}

function handleConfirm() {
  if (!fileList.value.length) return
  const uploadFile = fileList.value[0]
  if (uploadFile.raw) {
    emit('confirm', uploadFile.raw, currentPath.value)
    visible.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    title="上传文件"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="upload-body">
      <div class="upload-path">
        <span class="label">上传到：</span>
        <el-tag>{{ currentPath }}</el-tag>
      </div>
      <el-upload
        class="upload-area"
        drag
        :auto-upload="false"
        :file-list="fileList"
        :on-change="handleFileChange"
        :limit="1"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">将文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="!fileList.length"
        @click="handleConfirm"
      >确认上传</el-button>
    </template>
  </el-dialog>
</template>

<style lang="stylus" scoped>
.upload-body
  .upload-path
    margin-bottom: 16px
    display: flex
    align-items: center
    gap: 8px

    .label
      color: #606266
      font-size: 14px

  .upload-area
    width: 100%

    .upload-icon
      font-size: 48px
      color: #c0c4cc

    .upload-text
      color: #909399
      font-size: 14px

      em
        color: #409eff
        font-style: normal
</style>
