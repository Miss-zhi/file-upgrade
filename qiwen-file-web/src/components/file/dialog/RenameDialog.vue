<script setup lang="ts">
import { ref } from 'vue'

interface FileItem {
  id: string
  fileName: string
}

const visible = ref(false)
const newName = ref('')
const file = ref<FileItem | null>(null)

const emit = defineEmits<{
  confirm: [id: string, newName: string]
}>()

function open(item: FileItem) {
  file.value = item
  newName.value = item.fileName
  visible.value = true
}

function handleConfirm() {
  if (file.value && newName.value.trim()) {
    emit('confirm', file.value.id, newName.value.trim())
    visible.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="重命名" width="400px">
    <el-form @submit.prevent="handleConfirm">
      <el-form-item label="当前名称">
        <el-input :model-value="file?.fileName" disabled />
      </el-form-item>
      <el-form-item label="新名称">
        <el-input
          v-model="newName"
          placeholder="请输入新名称"
          @keyup.enter="handleConfirm"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
