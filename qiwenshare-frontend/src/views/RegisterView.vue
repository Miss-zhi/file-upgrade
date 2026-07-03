<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useCanvasNest } from '@/composables/useCanvasNest'
import DragVerify from '@/components/common/DragVerify.vue'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const nestRef = ref<HTMLElement | null>(null)
const dragVerified = ref(false)

useCanvasNest(nestRef, '230,162,60', 99)

const registerForm = reactive({
  username: '',
  telephone: '',
  password: '',
  confirmPassword: '',
})

/** 自定义校验：确认密码必须与密码一致 */
const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' },
  ],
  telephone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 30, message: '密码长度为 8-30 个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/, message: '密码需包含大小写字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (!formRef.value) return
  if (!dragVerified.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: registerForm.username,
      telephone: registerForm.telephone,
      password: registerForm.password,
    })
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'login' })
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    const message = err.response?.data?.message || '注册失败，请稍后重试'
    ElMessage.error(message)
    dragVerified.value = false
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div ref="nestRef" class="register-container">
    <div class="register-card">
      <h2 class="register-title">奇文网盘</h2>
      <p class="register-subtitle">创建新账户</p>

      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item label="手机号" prop="telephone">
          <el-input
            v-model="registerForm.telephone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（8-30位，含大小写字母和数字）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <DragVerify v-model:verified="dragVerified" />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!dragVerified"
            style="width: 100%"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        已有账户？
        <router-link :to="{ name: 'login' }">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #fff;
  position: relative;
}

.register-card {
  width: 375px;
  padding-top: 50px;
  position: relative;
  z-index: 1;
}

.register-title {
  text-align: center;
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 300;
  color: #000;
}

.register-subtitle {
  text-align: center;
  margin: 0 0 32px;
  font-size: 14px;
  color: #999;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #999;
}

.form-footer a {
  color: #409eff;
  text-decoration: none;
}

.form-footer a:hover {
  text-decoration: underline;
}
</style>
