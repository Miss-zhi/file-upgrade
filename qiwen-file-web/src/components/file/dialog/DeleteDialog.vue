<script setup lang="ts">
import { ref } from 'vue'

interface FileItem {
  id: string
  fileName: string
  fileSize: number | null
  isFolder: boolean
}

const visible = ref(false)
const file = ref<FileItem | null>(null)

const emit = defineEmits<{
  confirm: [id: string]
}>()

function open(item: FileItem) {
  file.value = item
  visible.value = true
}

function handleConfirm() {
  if (file.value) {
    emit('confirm', file.value.id)
    visible.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    title="确认删除"
    width="400px"
    :close-on-click-modal="false"
  >
    <div class="delete-warning">
      <el-icon :size="40" color="#f56c6c"><WarningFilled /></el-icon>
      <p>确定要删除 {{ file?.isFolder ? '文件夹' : '文件' }}"{{ file?.fileName }}" 吗？</p>
      <p class="hint">删除后不可恢复</p>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" @click="handleConfirm">确认删除</el-button>
    </template>
  </el-dialog>
</template>

<style lang="stylus" scoped>
.delete-warning
  text-align: center
  padding: 20px 0

  p
    margin-top: 12px
    font-size: 15px
    color: #303133

  .hint
    font-size: 13px
    color: #909399
</style>
