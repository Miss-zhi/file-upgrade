<script setup lang="ts">
defineProps<{ count: number }>()
defineEmits<{
  delete: []
  move: []
  clear: []
}>()
</script>

<template>
  <div class="batch-toolbar">
    <span class="batch-info">已选 {{ count }} 项</span>
    <el-button type="danger" size="small" @click="$emit('delete')">批量删除</el-button>
    <el-button type="primary" size="small" @click="$emit('move')">批量移动</el-button>
    <el-button size="small" @click="$emit('clear')">取消选择</el-button>
  </div>
</template>

<style lang="stylus" scoped>
.batch-toolbar
  display: flex
  align-items: center
  gap: 12px
  padding: 10px 16px
  background: #f0f9ff
  border-radius: 4px
  margin-bottom: 12px

  .batch-info
    font-weight: bold
    color: #409eff
    margin-right: auto
</style>
