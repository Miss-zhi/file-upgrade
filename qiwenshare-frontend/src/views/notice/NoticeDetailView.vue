<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNoticeDetail } from '@/api/notice'
import type { NoticeItem } from '@/api/notice'

const route = useRoute()

const notice = ref<NoticeItem | null>(null)
const loading = ref(false)

onMounted(async () => {
  const noticeId = route.params.noticeId as string
  loading.value = true
  try {
    notice.value = await getNoticeDetail({ noticeId })
  } catch {
    notice.value = null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="notice-detail-view">
    <template v-if="notice">
      <h1 class="notice-title">{{ notice.title }}</h1>
      <div class="notice-meta">
        <span>发布时间：{{ notice.publishTime }}</span>
      </div>
      <div class="notice-body" v-html="notice.content" />
    </template>
    <el-empty v-if="!loading && !notice" description="公告不存在或已删除" />
  </div>
</template>

<style lang="scss" scoped>
.notice-detail-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.notice-title {
  font-size: 24px;
  color: $primary-text;
  text-align: center;
  margin-bottom: 16px;
}

.notice-meta {
  text-align: center;
  color: $secondary-text;
  font-size: 13px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid $border-lighter;
}

.notice-body {
  font-size: 15px;
  line-height: 1.8;
  color: $primary-text;
}
</style>
