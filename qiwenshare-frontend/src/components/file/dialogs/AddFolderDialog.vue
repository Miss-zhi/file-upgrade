<script setup lang="ts">
import { ref, watch } from 'vue'
import { isValidFileName } from '@/utils/file'
import { useFileOperations } from '@/composables/useFileOperations'

const props = defineProps<{
  visible: boolean
  filePath: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const folderName = ref('')
const errorMsg = ref('')
const fileOps = useFileOperations()

watch(() => props.visible, (val) => {
  if (val) {
    folderName.value = ''
    errorMsg.value = ''
  }
})

const isValid = ref(true)

watch(folderName, (val) => {
  if (!val) {
    errorMsg.value = ''
    isValid.value = true
  } else if (!isValidFileName(val)) {
    errorMsg.value = '文件夹名称不能包含 \\ / : * ? " < > |'
    isValid.value = false
  } else {
    errorMsg.value = ''
    isValid.value = true
  }
})

async function handleConfirm(): Promise<void> {
  if (!isValid.value || !folderName.value) return
  const result = await fileOps.addFolder(folderName.value, props.filePath)
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
    title="新建文件夹"
    width="580px"
    @close="handleClose"
  >
    <el-input
      v-model="folderName"
      type="textarea"
      :rows="2"
      placeholder="请输入文件夹名称"
      autofocus
    />
    <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :disabled="!isValid || !folderName" :loading="fileOps.operationLoading.value" @click="handleConfirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.error-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
