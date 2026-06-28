<script setup lang="ts">
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'

interface UserItem {
  id: string
  username: string
  email: string
  phone: string
  nickname: string
  avatar: string
  status: number
}

const props = defineProps<{
  user: UserItem
}>()

const emit = defineEmits<{
  confirm: [data: { id: string; email: string; phone: string; nickname: string }]
}>()

const visible = ref(false)
const form = ref({
  email: '',
  phone: '',
  nickname: ''
})

function open() {
  form.value = {
    email: props.user.email || '',
    phone: props.user.phone || '',
    nickname: props.user.nickname || ''
  }
  visible.value = true
}

function handleConfirm() {
  emit('confirm', { id: props.user.id, ...form.value })
  visible.value = false
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="编辑用户" width="450px">
    <el-form @submit.prevent="handleConfirm">
      <el-form-item label="用户名">
        <el-input :model-value="user.username" disabled />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">保存</el-button>
    </template>
  </el-dialog>
</template>
