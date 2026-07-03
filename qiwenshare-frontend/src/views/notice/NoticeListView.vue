<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNoticeList } from '@/api/notice'
import type { NoticeItem } from '@/api/notice'

const router = useRouter()

const noticeList = ref<NoticeItem[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageCount = ref(10)
const total = ref(0)

async function loadData(): Promise<void> {
  loading.value = true
  try {
    noticeList.value = await getNoticeList({
      currentPage: currentPage.value,
      pageCount: pageCount.value,
    })
  } catch {
    noticeList.value = []
  } finally {
    loading.value = false
  }
}

function viewDetail(notice: NoticeItem): void {
  router.push(`/notice/${notice.noticeId}`)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="notice-list-view">
    <h2 class="page-title">系统公告</h2>

    <div v-loading="loading" class="notice-list">
      <div
        v-for="notice in noticeList"
        :key="notice.noticeId"
        class="notice-card"
        @click="viewDetail(notice)"
      >
        <h3 class="notice-card-title">{{ notice.title }}</h3>
        <span class="notice-card-time">{{ notice.publishTime }}</span>
      </div>

      <el-empty v-if="!loading && noticeList.length === 0" description="暂无公告" />
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      :page-size="pageCount"
      :total="total"
      layout="total, prev, pager, next"
      class="notice-pagination"
      @current-change="loadData"
    />
  </div>
</template>

<style lang="scss" scoped>
.notice-list-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 22px;
  color: $primary-text;
  margin-bottom: 20px;
}

.notice-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid $border-lighter;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: $primary-hover;
  }
}

.notice-card-title {
  font-size: 15px;
  color: $primary-text;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-card-time {
  font-size: 12px;
  color: $secondary-text;
  margin-left: 16px;
  white-space: nowrap;
}

.notice-pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
