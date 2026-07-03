<script setup lang="ts">
import { ref, computed } from 'vue'
import { useFileListStore } from '@/stores/fileList'
import type { FileInfo } from '@/types/file'
import { getFileIconSrc, isVideoFile } from '@/utils/file'

const fileListStore = useFileListStore()

const emit = defineEmits<{
  (e: 'openFile', file: FileInfo): void
}>()

/** 排序方向 */
const sortOrder = ref<'asc' | 'desc'>('desc')

/** 按日期分组的文件列表 */
const groupedFiles = computed(() => {
  const files = [...fileListStore.fileList]
  // 排序
  files.sort((a, b) => {
    const dateA = new Date(a.uploadTime).getTime()
    const dateB = new Date(b.uploadTime).getTime()
    return sortOrder.value === 'desc' ? dateB - dateA : dateA - dateB
  })

  // 按日期分组
  const groups: Map<string, FileInfo[]> = new Map()
  for (const file of files) {
    const date = file.uploadTime?.substring(0, 10) || '未知日期'
    if (!groups.has(date)) {
      groups.set(date, [])
    }
    groups.get(date)!.push(file)
  }

  return Array.from(groups.entries()).map(([date, items]) => ({
    date,
    items,
  }))
})

const imgSize = computed(() => fileListStore.gridSize)
</script>

<template>
  <div v-loading="fileListStore.loading" class="file-timeline-container">
    <div class="timeline-sort">
      <el-radio-group v-model="sortOrder" size="small">
        <el-radio-button value="desc">倒序</el-radio-button>
        <el-radio-button value="asc">正序</el-radio-button>
      </el-radio-group>
    </div>

    <el-timeline v-if="groupedFiles.length > 0">
      <el-timeline-item
        v-for="group in groupedFiles"
        :key="group.date"
        :timestamp="group.date"
        placement="top"
      >
        <div class="timeline-group">
          <div
            v-for="file in group.items"
            :key="file.userFileId"
            class="timeline-item"
            @dblclick="emit('openFile', file)"
          >
            <video
              v-if="isVideoFile(file.extendName)"
              :src="getFileIconSrc(file.extendName, file.fileType, file.userFileId)"
              :style="{ width: imgSize + 'px', height: imgSize + 'px' }"
              :alt="file.fileName"
              muted
              preload="metadata"
            />
            <img
              v-else
              :src="getFileIconSrc(file.extendName, file.fileType, file.userFileId)"
              :style="{ width: imgSize + 'px', height: imgSize + 'px' }"
              :alt="file.fileName"
            />
            <div class="timeline-item-name" :title="file.fileName">
              {{ file.fileName }}
            </div>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>

    <el-empty
      v-if="!fileListStore.loading && fileListStore.fileList.length === 0"
      description="暂无文件"
    />
  </div>
</template>

<style lang="scss" scoped>
.file-timeline-container {
  height: calc(100vh - 215px);
  overflow-y: auto;
  padding: 8px 0;
}

.timeline-sort {
  margin-bottom: 16px;
}

.timeline-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.timeline-item {
  text-align: center;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;

  &:hover {
    background: $tab-back-color;
  }

  img {
    object-fit: contain;
  }
}

.timeline-item-name {
  font-size: 12px;
  line-height: 20px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
