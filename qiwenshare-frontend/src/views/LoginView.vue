<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useCanvasNest } from '@/composables/useCanvasNest'
import DragVerify from '@/components/common/DragVerify.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const nestRef = ref<HTMLElement | null>(null)
const dragVerified = ref(false)

useCanvasNest(nestRef, '64,158,255', 99)

const loginForm = reactive({
  telephone: '',
  password: '',
})

const rules: FormRules = {
  telephone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
}

async function handleLogin() {
  if (!formRef.value) return
  if (!dragVerified.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login({
      telephone: loginForm.telephone,
      password: loginForm.password,
    })
    ElMessage.success('登录成功')

    // 跳转到目标页面或首页
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    const message = err.response?.data?.message || '登录失败，请检查手机号和密码'
    ElMessage.error(message)
    dragVerified.value = false
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div ref="nestRef" class="login-container">
    <div class="login-card">
      <h2 class="login-title">奇文网盘</h2>
      <p class="login-subtitle">登录您的账户</p>

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="手机号" prop="telephone">
          <el-input
            v-model="loginForm.telephone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
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
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        还没有账户？
        <router-link :to="{ name: 'register' }">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #fff;
  position: relative;
}

.login-card {
  width: 375px;
  padding-top: 50px;
  position: relative;
  z-index: 1;
}

.login-title {
  text-align: center;
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 300;
  color: #000;
}

.login-subtitle {
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
