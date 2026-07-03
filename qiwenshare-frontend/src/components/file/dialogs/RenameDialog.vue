<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FileInfo } from '@/types/file'
import { isValidFileName } from '@/utils/file'
import { useFileOperations } from '@/composables/useFileOperations'

const props = defineProps<{
  visible: boolean
  file: FileInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const newName = ref('')
const fileOps = useFileOperations()

watch(() => props.visible, (val) => {
  if (val && props.file) {
    newName.value = props.file.fileName
  }
})

const isValid = ref(true)

watch(newName, (val) => {
  if (!val) {
    isValid.value = false
  } else if (!isValidFileName(val)) {
    isValid.value = false
  } else if (props.file && val === props.file.fileName) {
    isValid.value = false
  } else {
    isValid.value = true
  }
})

async function handleConfirm(): Promise<void> {
  if (!isValid.value || !props.file) return
  const result = await fileOps.rename(props.file.userFileId, newName.value)
  if (result) {
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
    title="重命名文件"
    width="550px"
    @close="handleClose"
  >
    <el-input
      v-model="newName"
      type="textarea"
      :rows="2"
      placeholder="请输入新文件名"
      autofocus
    />

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :disabled="!isValid" :loading="fileOps.operationLoading.value" @click="handleConfirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>
