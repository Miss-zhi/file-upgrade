<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './AppHeader.vue'
import AppAside from './AppAside.vue'
import AppFooter from './AppFooter.vue'

const route = useRoute()

/** 是否显示 Header */
const showHeader = computed(() => route.meta.hideHeader !== true)

/** 是否显示 Footer */
const showFooter = computed(() => route.meta.hideFooter !== true)

/** 是否为文件管理页面（需要侧边栏） */
const showAside = computed(() => route.path.startsWith('/file'))

/** 是否为非文件页面（主内容区需要 90% 宽度约束） */
const isNonFilePage = computed(() => !route.path.startsWith('/file') && !route.path.startsWith('/admin'))
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <el-container class="app-main-container">
      <el-aside v-if="showAside" width="auto" class="app-aside-wrapper">
        <AppAside />
      </el-aside>
      <el-main class="app-content" :class="{ 'content-centered': isNonFilePage }">
        <RouterView />
      </el-main>
    </el-container>
    <AppFooter />
  </div>
</template>

<style lang="scss" scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-main-container {
  flex: 1;
}

.app-aside-wrapper {
  overflow: visible;
}

.app-content {
  padding: 0;
  overflow: auto;

  &.content-centered {
    width: 90%;
    min-height: calc(100vh - 70px);
    margin: 0 auto;
  }
}
</style>
