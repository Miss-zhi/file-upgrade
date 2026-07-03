<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FileType } from '@/types/file'

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  fileType: number
  filePath: string
}>()

/** 是否可编辑路径（仅全部文件时可编辑） */
const isEditable = computed(() => props.fileType === FileType.ALL)

/** 是否编辑模式 */
const isEditing = ref(false)
const editPath = ref('')

/** 分类文本映射 */
const categoryLabels: Record<number, string> = {
  [FileType.IMAGE]: '图片',
  [FileType.DOCUMENT]: '文档',
  [FileType.VIDEO]: '视频',
  [FileType.MUSIC]: '音乐',
  [FileType.OTHER]: '其他',
  [FileType.RECYCLE]: '回收站',
  [FileType.SHARE]: '我的分享',
}

/** 面包屑路径段 */
const pathSegments = computed(() => {
  if (props.fileType !== FileType.ALL && props.fileType !== FileType.SHARE) {
    return []
  }
  const path = props.filePath || '/'
  if (path === '/') return [{ name: '全部文件', path: '/' }]
  const parts = path.split('/').filter(Boolean)
  const segments = [{ name: '全部文件', path: '/' }]
  let current = ''
  for (const part of parts) {
    current += '/' + part
    segments.push({ name: part, path: current })
  }
  return segments
})

/** 分类标签文本 */
const categoryLabel = computed(() => categoryLabels[props.fileType] || '')

/** 导航到路径 */
function navigateTo(path: string): void {
  router.push({
    path: '/file',
    query: { fileType: String(props.fileType), filePath: path },
  })
}

/** 进入编辑模式 */
function startEditing(): void {
  if (!isEditable.value) return
  isEditing.value = true
  editPath.value = props.filePath || '/'
}

/** 确认编辑 */
function confirmEdit(): void {
  isEditing.value = false
  if (editPath.value && editPath.value !== props.filePath) {
    navigateTo(editPath.value)
  }
}

/** 取消编辑 */
function cancelEdit(): void {
  isEditing.value = false
}
</script>

<template>
  <div class="breadcrumb-container" @click="startEditing">
    <!-- 可编辑路径输入框 -->
    <el-input
      v-if="isEditing"
      v-model="editPath"
      size="small"
      class="breadcrumb-input"
      @blur="confirmEdit"
      @keyup.enter="confirmEdit"
      @keyup.escape="cancelEdit"
    />

    <!-- 分类标签（非全部文件） -->
    <template v-else-if="fileType !== FileType.ALL && fileType !== FileType.SHARE">
      <span class="breadcrumb-label">{{ categoryLabel }}</span>
    </template>

    <!-- 面包屑导航 -->
    <template v-else>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(seg, idx) in pathSegments"
          :key="seg.path"
          class="breadcrumb-item"
          :class="{ 'is-last': idx === pathSegments.length - 1 }"
          @click.stop="navigateTo(seg.path)"
        >
          {{ seg.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.breadcrumb-container {
  display: flex;
  align-items: center;
  height: 30px;
  line-height: 30px;
  padding: 0 4px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: $tab-back-color;
  }
}

.breadcrumb-input {
  width: 100%;
  max-width: 500px;
}

.breadcrumb-label {
  font-size: 14px;
  color: $primary-text;
  font-weight: 500;
}

.breadcrumb-item {
  cursor: pointer;

  &.is-last {
    font-weight: 500;
  }

  &:hover:not(.is-last) {
    color: $primary;
  }
}
</style>
