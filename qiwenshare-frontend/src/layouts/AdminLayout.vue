<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

/** 菜单项定义：label + route name + permission */
const menuItems = [
  { label: '用户管理', routeName: 'adminUsers', permission: 'admin:user-manage' },
  { label: '角色管理', routeName: 'adminRoles', permission: 'admin:role-manage' },
  { label: '配额管理', routeName: 'adminQuota', permission: 'admin:quota-manage' },
  { label: '审计日志', routeName: 'adminLogs', permission: 'admin:log-view' },
  { label: '系统配置', routeName: 'adminConfig', permission: 'admin:config-manage' },
]

/** 当前激活的菜单项（基于路由名） */
const activeMenu = computed(() => {
  const name = route.name as string
  return name ?? 'adminUsers'
})

/** 根据权限码过滤可见菜单 */
const visibleMenuItems = computed(() =>
  menuItems.filter((item) => authStore.hasPermission(item.permission)),
)

/** 菜单点击导航 */
function handleMenuSelect(index: string): void {
  router.push({ name: index })
}
</script>

<template>
  <div class="admin-layout">
    <!-- 左侧导航 -->
    <aside class="admin-aside">
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item
          v-for="item in visibleMenuItems"
          :key="item.routeName"
          :index="item.routeName"
        >
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧内容区 -->
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  height: 100%;
  background: $tab-back-color;
}

.admin-aside {
  width: 200px;
  min-width: 200px;
  background: #fff;
  border-right: 1px solid $border-lighter;
  overflow-y: auto;
}

.admin-menu {
  border-right: none;
  height: 100%;
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>
