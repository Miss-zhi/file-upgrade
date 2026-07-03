<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSideMenuStore } from '@/stores/sideMenu'
import { useAuthStore } from '@/stores/auth'
import { useCommonStore } from '@/stores/common'
import { FileType } from '@/types/file'

const route = useRoute()
const router = useRouter()
const sideMenuStore = useSideMenuStore()
const authStore = useAuthStore()
const commonStore = useCommonStore()

/** 是否移动端 */
const isMobile = computed(() => commonStore.screenWidth <= 768)

/** 移动端 drawer 可见性 */
const drawerVisible = computed({
  get: () => isMobile.value && !sideMenuStore.isCollapsed,
  set: (val: boolean) => {
    if (!val && isMobile.value) {
      sideMenuStore.toggleCollapse()
    }
  },
})

/** 当前激活的文件类型 */
const activeFileType = computed(() => {
  const fileType = route.query.fileType
  return fileType !== undefined ? Number(fileType) : FileType.ALL
})

/** 菜单项定义 */
const menuItems = [
  { label: '全部', fileType: FileType.ALL, icon: 'Menu' },
  { label: '图片', fileType: FileType.IMAGE, icon: 'Picture' },
  { label: '文档', fileType: FileType.DOCUMENT, icon: 'Document' },
  { label: '视频', fileType: FileType.VIDEO, icon: 'VideoCamera' },
  { label: '音乐', fileType: FileType.MUSIC, icon: 'Headset' },
  { label: '其他', fileType: FileType.OTHER, icon: 'Box' },
  { label: '回收站', fileType: FileType.RECYCLE, icon: 'Delete' },
  { label: '我的分享', fileType: FileType.SHARE, icon: 'Share' },
]

/** admin 子菜单项 */
const adminMenuItems = [
  { label: '用户管理', route: '/admin/users', permission: 'admin:user-manage' },
  { label: '角色管理', route: '/admin/roles', permission: 'admin:role-manage' },
  { label: '配额管理', route: '/admin/quota', permission: 'admin:quota-manage' },
  { label: '审计日志', route: '/admin/logs', permission: 'admin:log-view' },
  { label: '系统配置', route: '/admin/config', permission: 'admin:config-manage' },
]

/** 是否有任何一个 admin 权限 */
const showAdminEntry = computed(() => adminMenuItems.some((item) => authStore.hasPermission(item.permission)))

/** 存储容量条颜色 */
const storageColor = computed(() => {
  const pct = sideMenuStore.storagePercentage
  if (pct <= 50) return '#67C23A'
  if (pct <= 80) return '#E6A23C'
  return '#F56C6C'
})

/** 菜单点击（仅处理文件类型菜单，admin 菜单有自己的 @click） */
function handleMenuClick(index: string): void {
  const fileType = Number(index)
  if (isNaN(fileType)) return
  router.push({ path: '/file', query: { fileType: index, filePath: '/' } })
}

/** 导航到 admin 子页面 */
function navigateToAdminPage(route: string): void {
  router.push(route)
}

/** 初始化加载存储信息 */
onMounted(() => {
  if (authStore.isLoggedIn) {
    sideMenuStore.fetchStorage()
  }
})
</script>

<template>
  <!-- 桌面端侧边栏 -->
  <aside v-if="!isMobile" class="app-aside" :class="{ 'is-collapsed': sideMenuStore.isCollapsed }">
    <el-menu
      :default-active="String(activeFileType)"
      :collapse="sideMenuStore.isCollapsed"
      class="aside-menu"
      @select="handleMenuClick($event)"
    >
      <el-menu-item
        v-for="item in menuItems"
        :key="item.fileType"
        :index="String(item.fileType)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.label }}</template>
      </el-menu-item>
      <template v-if="showAdminEntry">
        <template v-for="adminItem in adminMenuItems" :key="adminItem.route">
          <el-menu-item
            v-if="authStore.hasPermission(adminItem.permission)"
            :index="adminItem.route"
            @click="navigateToAdminPage(adminItem.route)"
          >
            <template #title>{{ adminItem.label }}</template>
          </el-menu-item>
        </template>
      </template>
    </el-menu>

    <!-- 折叠/展开切换条 -->
    <div class="collapse-toggle" @click="sideMenuStore.toggleCollapse">
      <el-icon>
        <ArrowLeft v-if="!sideMenuStore.isCollapsed" />
        <ArrowRight v-else />
      </el-icon>
    </div>

    <!-- 存储容量条 -->
    <div class="storage-bar">
      <div class="storage-text">
        已用 {{ (sideMenuStore.storageValue / 1024 / 1024).toFixed(1) }} MB
        / 总共 {{ (sideMenuStore.totalStorageValue / 1024 / 1024).toFixed(0) }} MB
      </div>
      <el-progress
        :percentage="sideMenuStore.storagePercentage"
        :color="storageColor"
        :stroke-width="6"
        :show-text="false"
      />
    </div>
  </aside>

  <!-- 移动端 Drawer -->
  <el-drawer
    v-if="isMobile"
    v-model="drawerVisible"
    direction="ltr"
    :size="210"
    :with-header="false"
  >
    <el-menu
      :default-active="String(activeFileType)"
      class="aside-menu"
      @select="handleMenuClick($event)"
    >
      <el-menu-item
        v-for="item in menuItems"
        :key="item.fileType"
        :index="String(item.fileType)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.label }}</template>
      </el-menu-item>
      <template v-if="showAdminEntry">
        <template v-for="adminItem in adminMenuItems" :key="adminItem.route">
          <el-menu-item
            v-if="authStore.hasPermission(adminItem.permission)"
            :index="adminItem.route"
            @click="navigateToAdminPage(adminItem.route)"
          >
            <template #title>{{ adminItem.label }}</template>
          </el-menu-item>
        </template>
      </template>
    </el-menu>

    <div class="storage-bar">
      <div class="storage-text">
        已用 {{ (sideMenuStore.storageValue / 1024 / 1024).toFixed(1) }} MB
        / 总共 {{ (sideMenuStore.totalStorageValue / 1024 / 1024).toFixed(0) }} MB
      </div>
      <el-progress
        :percentage="sideMenuStore.storagePercentage"
        :color="storageColor"
        :stroke-width="6"
        :show-text="false"
      />
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
.app-aside {
  position: relative;
  width: $sidebar-width;
  min-width: $sidebar-width;
  height: calc(100vh - $header-height);
  background: #fff;
  border-right: 1px solid $border-lighter;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;

  &.is-collapsed {
    width: 64px;
    min-width: 64px;
  }
}

.aside-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.collapse-toggle {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 100px;
  background: $border-lighter;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 0 16px 16px 0;
  z-index: 10;

  &:hover {
    background: $primary;
    color: #fff;
  }
}

.storage-bar {
  height: $sidebar-storage-bar;
  padding: 12px 16px;
  border-top: 1px solid $border-lighter;
  flex-shrink: 0;
}

.storage-text {
  font-size: 12px;
  color: $secondary-text;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

::v-deep(.el-menu-item.is-active) {
  background-color: $primary-hover;
}
</style>
