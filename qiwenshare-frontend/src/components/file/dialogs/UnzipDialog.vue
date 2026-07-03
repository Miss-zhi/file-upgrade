<script setup lang="ts">
import type { FileInfo } from '@/types/file'

const props = defineProps<{
  visible: boolean
  file: FileInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="解压缩文件"
    width="550px"
    @close="handleClose"
  >
    <div class="unzip-content">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <p>解压功能正在开发中，敬请期待...</p>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.unzip-content {
  text-align: center;
  padding: 20px;
}
</style>
