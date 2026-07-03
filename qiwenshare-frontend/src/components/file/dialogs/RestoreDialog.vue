<script setup lang="ts">
import { ref, watch } from 'vue'
import { useFileOperations } from '@/composables/useFileOperations'

const props = defineProps<{
  visible: boolean
  userFileIds: number[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const fileOps = useFileOperations()
const statusText = ref('正在还原文件...')

watch(() => props.visible, async (val) => {
  if (val && props.userFileIds.length > 0) {
    statusText.value = '正在还原文件...'
    const success = await fileOps.restore(props.userFileIds)
    if (success) {
      statusText.value = '还原成功！'
      emit('success')
      setTimeout(() => emit('update:visible', false), 1000)
    } else {
      statusText.value = '还原失败'
    }
  }
})

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="还原文件"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="restore-content">
      <el-icon v-if="fileOps.operationLoading.value" class="is-loading" :size="24"><Loading /></el-icon>
      <p>{{ statusText }}</p>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.restore-content {
  text-align: center;
  padding: 20px;
}
</style>
