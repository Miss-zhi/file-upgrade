<script setup lang="ts">
import { computed } from 'vue'

interface Crumb {
  name: string
  path: string
}

const props = defineProps<{
  path: string
}>()

const emit = defineEmits<{
  navigate: [path: string]
}>()

const crumbs = computed<Crumb[]>(() => {
  const parts = props.path.split('/').filter(Boolean)
  const result: Crumb[] = [{ name: '根目录', path: '/' }]
  let accumulated = '/'
  for (const part of parts) {
    accumulated += part + '/'
    result.push({ name: part, path: accumulated })
  }
  return result
})
</script>

<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item
      v-for="crumb in crumbs"
      :key="crumb.path"
    >
      <el-link
        :underline="false"
        :type="crumb.path === path ? 'default' : 'primary'"
        :disabled="crumb.path === path"
        @click="emit('navigate', crumb.path)"
      >
        {{ crumb.name }}
      </el-link>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style lang="stylus" scoped>
// 面包屑无需额外样式
</style>
