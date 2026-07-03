<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { FileInfo } from '@/types/file'
import { useFileOperations } from '@/composables/useFileOperations'

const props = defineProps<{
  visible: boolean
  files: FileInfo[]
  mode: 1 | 2 // 1=软删除, 2=永久删除
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const fileOps = useFileOperations()

const title = computed(() =>
  props.mode === 1 ? '删除文件' : '永久删除文件',
)

const message = computed(() => {
  const count = props.files.length
  if (props.mode === 1) {
    return `确定将 ${count} 个文件移至回收站？`
  }
  return `确定永久删除 ${count} 个文件？此操作不可恢复！`
})

async function handleConfirm(): Promise<void> {
  const ids = props.files.map((f) => f.userFileId)
  let success = false
  if (props.mode === 1) {
    if (ids.length === 1 && ids[0] != null) {
      success = await fileOps.remove(ids[0])
    } else {
      success = await fileOps.batchRemove(ids)
    }
  } else {
    success = await fileOps.permanentDelete(ids)
  }
  if (success) {
    emit('success')
    emit('update:visible', false)
  }
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="550px"
    @close="handleClose"
  >
    <div :class="{ 'delete-warning': mode === 2 }">
      <el-icon v-if="mode === 2" :size="24" color="#f56c6c"><WarningFilled /></el-icon>
      <p>{{ message }}</p>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button :type="mode === 2 ? 'danger' : 'primary'" :loading="fileOps.operationLoading.value" @click="handleConfirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.delete-warning {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #fef0f0;
  border-radius: 4px;
}
</style>
