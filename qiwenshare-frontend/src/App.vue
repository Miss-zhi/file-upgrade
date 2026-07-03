<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import AppLayout from '@/layouts/AppLayout.vue'
import { useCommonStore } from '@/stores/common'

const commonStore = useCommonStore()

/** resize 事件处理函数 */
function handleResize(): void {
  commonStore.updateScreenWidth()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <AppLayout />
</template>
