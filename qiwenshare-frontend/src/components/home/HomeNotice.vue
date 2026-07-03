<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNoticeList } from '@/api/notice'
import type { NoticeItem } from '@/api/notice'

const router = useRouter()

/** 公告列表 */
const noticeList = ref<NoticeItem[]>([])

/** 当前轮播索引 */
const currentIndex = ref(0)

/** 轮播定时器 */
let timer: ReturnType<typeof setInterval> | null = null

/** 加载公告数据 */
onMounted(async () => {
  try {
    noticeList.value = await getNoticeList({ currentPage: 1, pageCount: 3 })
    startAutoPlay()
  } catch {
    // 静默失败
  }
})

onUnmounted(() => {
  stopAutoPlay()
})

/** 开始自动轮播 */
function startAutoPlay(): void {
  stopAutoPlay()
  timer = setInterval(() => {
    if (noticeList.value.length > 0) {
      currentIndex.value = (currentIndex.value + 1) % noticeList.value.length
    }
  }, 4000)
}

/** 停止自动轮播 */
function stopAutoPlay(): void {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/** 上一条 */
function prev(): void {
  stopAutoPlay()
  currentIndex.value =
    (currentIndex.value - 1 + noticeList.value.length) % noticeList.value.length
}

/** 下一条 */
function next(): void {
  stopAutoPlay()
  currentIndex.value = (currentIndex.value + 1) % noticeList.value.length
}

/** 查看公告详情 */
function viewDetail(notice: NoticeItem): void {
  router.push(`/notice/${notice.noticeId}`)
}

/** 查看全部公告 */
function viewAll(): void {
  router.push('/notice')
}
</script>

<template>
  <section v-if="noticeList.length > 0" class="home-notice">
    <div class="notice-header">
      <h2 class="notice-title">最新公告</h2>
      <el-button type="primary" link @click="viewAll">查看全部</el-button>
    </div>

    <div class="notice-content">
      <div class="notice-list">
        <div
          v-for="(notice, index) in noticeList"
          :key="notice.noticeId"
          v-show="index === currentIndex"
          class="notice-item"
          @click="viewDetail(notice)"
        >
          <span class="notice-item-title">{{ notice.title }}</span>
          <span class="notice-item-time">{{ notice.publishTime }}</span>
        </div>
      </div>

      <div class="notice-controls">
        <el-button :icon="'ArrowUp'" circle size="small" @click="prev" />
        <el-button :icon="'ArrowDown'" circle size="small" @click="next" />
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.home-notice {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.notice-title {
  font-size: 22px;
  color: $primary-text;
}

.notice-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notice-list {
  flex: 1;
  min-height: 60px;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: $primary-hover;
  }
}

.notice-item-title {
  font-size: 14px;
  color: $primary-text;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.notice-item-time {
  font-size: 12px;
  color: $secondary-text;
  margin-left: 16px;
  white-space: nowrap;
}

.notice-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
