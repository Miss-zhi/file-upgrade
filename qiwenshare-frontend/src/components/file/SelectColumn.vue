<script setup lang="ts">
import { ref, watch } from 'vue'
import { useFileListStore } from '@/stores/fileList'
import { allColumnList } from '@/types/file'

const fileListStore = useFileListStore()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const localColumns = ref<string[]>([...fileListStore.selectedColumnList])

watch(() => props.modelValue, (val) => {
  if (val) {
    localColumns.value = [...fileListStore.selectedColumnList]
  }
})

function handleConfirm(): void {
  fileListStore.setSelectedColumnList(localColumns.value)
  emit('update:modelValue', false)
}

function handleCancel(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="props.modelValue"
    title="设置表格列显隐"
    width="700px"
    @close="handleCancel"
  >
    <el-checkbox-group v-model="localColumns">
      <el-checkbox value="extendName">类型</el-checkbox>
      <el-checkbox value="fileSize">大小</el-checkbox>
      <el-checkbox value="uploadTime">修改日期</el-checkbox>
      <el-checkbox value="deleteTime">删除日期</el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
