<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { searchFiles } from '_api/search'

const route = useRoute()
const results = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')

async function doSearch(kw: string) {
  if (!kw) return
  keyword.value = kw
  loading.value = true
  const res: any = await searchFiles(kw)
  if (res.success) {
    results.value = res.data || []
  }
  loading.value = false
}

onMounted(() => {
  const q = route.query.q as string
  if (q) doSearch(q)
})

watch(() => route.query.q, (q) => {
  if (q) doSearch(q as string)
})
</script>

<template>
  <div class="search-result-page">
    <h2>搜索结果：{{ keyword }}</h2>
    <p class="result-count" v-if="!loading">共 {{ results.length }} 条结果</p>

    <div v-loading="loading" class="result-list">
      <div
        v-for="item in results"
        :key="item.fileId"
        class="result-item"
      >
        <div class="file-name">
          <el-icon><Document /></el-icon>
          <span v-html="item.fileNameHighlight || item.fileName" />
        </div>
        <div class="file-info">
          <span class="file-path">{{ item.filePath }}</span>
          <span class="file-size">{{ item.fileSize }} B</span>
        </div>
      </div>

      <el-empty v-if="!loading && !results.length" description="未找到相关文件" />
    </div>
  </div>
</template>

<style lang="stylus" scoped>
.search-result-page
  padding: 24px 40px

  h2
    margin-bottom: 8px

  .result-count
    color: #909399
    font-size: 14px
    margin-bottom: 16px

  .result-item
    padding: 12px 0
    border-bottom: 1px solid #ebeef5

    .file-name
      display: flex
      align-items: center
      gap: 8px
      font-size: 15px
      margin-bottom: 4px

      :deep(em)
        color: #f56c6c
        font-style: normal

    .file-info
      font-size: 13px
      color: #909399
      display: flex
      gap: 16px
</style>
