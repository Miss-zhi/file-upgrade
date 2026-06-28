<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')

function handleSearch() {
  const kw = keyword.value.trim()
  if (kw) {
    router.push({ path: '/search', query: { q: kw } })
  }
}
</script>

<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      placeholder="搜索文件..."
      clearable
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
  </div>
</template>

<style lang="stylus" scoped>
.search-bar
  width: 280px
</style>
