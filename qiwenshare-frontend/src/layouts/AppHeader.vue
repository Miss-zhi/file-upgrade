<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCommonStore } from '@/stores/common'
import { changePassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const commonStore = useCommonStore()

/** 是否隐藏 Header（由路由 meta 控制） */
const hideHeader = computed(() => route.meta.hideHeader === true)

/** 是否移动端 */
const isMobile = computed(() => commonStore.screenWidth <= 768)

/** 修改密码对话框 */
const passwordDialogVisible = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

/** 当前菜单激活项 */
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/file')) return '/file'
  if (path.startsWith('/notice')) return '/notice'
  return ''
})

/** 导航到指定路径 */
function navigateTo(path: string): void {
  router.push(path)
}

/** 退出登录 */
async function handleLogout(): Promise<void> {
  await authStore.logout()
  router.push('/login')
}

/** 打开修改密码对话框 */
function openPasswordDialog(): void {
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  passwordDialogVisible.value = true
}

/** 提交修改密码 */
async function handleChangePassword(): Promise<void> {
  if (!oldPassword.value || !newPassword.value) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  try {
    await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
  } catch {
    ElMessage.error('密码修改失败')
  }
}
</script>

<template>
  <header v-if="!hideHeader" class="app-header">
    <div class="header-left">
      <img
        src="@/assets/logo.svg"
        alt="Logo"
        class="header-logo"
        @click="navigateTo('/')"
      />
      <el-menu
        v-if="!isMobile"
        mode="horizontal"
        :default-active="activeMenu"
        :ellipsis="false"
        class="header-menu"
        @select="navigateTo"
      >
        <el-menu-item index="/file">网盘</el-menu-item>
        <el-menu-item index="/notice">公告</el-menu-item>
      </el-menu>
    </div>

    <div class="header-right">
      <template v-if="!authStore.isLoggedIn">
        <el-button type="primary" link @click="navigateTo('/login')">登录</el-button>
        <el-button type="primary" link @click="navigateTo('/register')">注册</el-button>
      </template>
      <template v-else>
        <el-dropdown trigger="click">
          <span class="user-dropdown-link">
            {{ authStore.user?.username }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openPasswordDialog">修改密码</el-dropdown-item>
              <el-dropdown-item @click="handleLogout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="旧密码">
          <el-input v-model="oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<style lang="scss" scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $header-height;
  background: #fff;
  box-shadow: $tab-box-shadow;
  padding: 0 20px;
  position: relative;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-logo {
  height: 40px;
  margin: 14px 24px;
  cursor: pointer;
}

.header-menu {
  border-bottom: none !important;
}

.header-right {
  display: flex;
  align-items: center;
  margin-right: 24px;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: $primary-text;
  font-size: 14px;
}
</style>
