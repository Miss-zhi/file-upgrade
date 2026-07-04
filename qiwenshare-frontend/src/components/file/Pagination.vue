<script setup lang="ts">
import { useFileListStore } from '@/stores/fileList'

const fileListStore = useFileListStore()

const props = defineProps<{
  currentPage: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'pageChange', page: number): void
  (e: 'sizeChange', size: number): void
}>()

function handleCurrentChange(page: number): void {
  emit('pageChange', page - 1) // 后端从 0 开始
}

function handleSizeChange(size: number): void {
  emit('sizeChange', size)
}
</script>

<template>
  <div class="file-pagination">
    <el-pagination
      :current-page="props.currentPage + 1"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="fileListStore.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="props.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<style lang="scss" scoped>
.file-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 44px;
  padding: 0 8px;
  border-top: 1px solid $border-lighter;
}
</style>
