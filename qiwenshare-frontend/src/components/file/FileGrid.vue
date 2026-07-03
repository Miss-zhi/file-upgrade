<script setup lang="ts">
import { computed } from 'vue'
import { useFileListStore } from '@/stores/fileList'
import type { FileInfo } from '@/types/file'
import { getFileIconSrc, isVideoFile, isFolder } from '@/utils/file'

const fileListStore = useFileListStore()

const props = defineProps<{
  fileType: number
  highlightMap?: Map<number, string>
}>()

const emit = defineEmits<{
  (e: 'openFile', file: FileInfo): void
  (e: 'contextMenu', event: MouseEvent, file: FileInfo): void
}>()

/** 网格项宽度 */
const itemWidth = computed(() => fileListStore.gridSize + 40)

/** 网格项中的图片尺寸 */
const imgSize = computed(() => fileListStore.gridSize)

function handleDblClick(file: FileInfo): void {
  emit('openFile', file)
}

function handleContextMenu(e: MouseEvent, file: FileInfo): void {
  e.preventDefault()
  e.stopPropagation()
  emit('contextMenu', e, file)
}

function isSelected(file: FileInfo): boolean {
  return fileListStore.selectedFiles.some((f) => f.userFileId === file.userFileId)
}

function toggleSelect(file: FileInfo): void {
  const idx = fileListStore.selectedFiles.findIndex(
    (f) => f.userFileId === file.userFileId,
  )
  if (idx >= 0) {
    fileListStore.selectedFiles.splice(idx, 1)
  } else {
    fileListStore.selectedFiles.push(file)
  }
  fileListStore.isBatchOperation = fileListStore.selectedFiles.length > 0
}
</script>

<template>
  <div v-loading="fileListStore.loading" class="file-grid-container">
    <ul class="file-grid">
      <li
        v-for="file in fileListStore.fileList"
        :key="file.userFileId"
        class="file-grid-item"
        :style="{ width: itemWidth + 'px' }"
        :class="{ 'is-selected': isSelected(file) }"
        @dblclick="handleDblClick(file)"
        @contextmenu="handleContextMenu($event, file)"
      >
        <!-- 批量选择覆盖层 -->
        <div
          v-if="fileListStore.isBatchOperation"
          class="grid-checkbox"
          @click.stop="toggleSelect(file)"
        >
          <el-checkbox :model-value="isSelected(file)" />
        </div>

        <div class="grid-icon">
          <video
            v-if="isVideoFile(file.extendName)"
            :src="getFileIconSrc(file.extendName, file.fileType, file.userFileId)"
            :alt="file.fileName"
            title="点击预览"
            :style="{ width: imgSize + 'px', height: imgSize + 'px' }"
            muted
            preload="metadata"
          />
          <img
            v-else
            :src="getFileIconSrc(file.extendName, file.fileType, file.userFileId)"
            :alt="file.fileName"
            title="点击预览"
            :style="{ width: imgSize + 'px', height: imgSize + 'px' }"
          />
        </div>
        <div class="grid-name" :title="'点击预览: ' + file.fileName">
          <span v-if="highlightMap?.has(file.userFileId)" v-html="highlightMap.get(file.userFileId)" />
          <template v-else>{{ file.fileName }}</template>
        </div>
      </li>
    </ul>

    <el-empty
      v-if="!fileListStore.loading && fileListStore.fileList.length === 0"
      description="暂无文件"
    />
  </div>
</template>

<style lang="scss" scoped>
.file-grid-container {
  height: calc(100vh - 206px);
  overflow-y: auto;
  padding: 8px 0;
}

.file-grid {
  display: flex;
  flex-wrap: wrap;
  list-style: none;
  margin: 0;
  padding: 0;
}

.file-grid-item {
  position: relative;
  margin: 0 16px 16px 0;
  padding: 8px;
  text-align: center;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;

  &:hover {
    background: $tab-back-color;
  }

  &.is-selected {
    background: $primary-hover;
  }
}

.grid-checkbox {
  position: absolute;
  top: 4px;
  left: 4px;
  z-index: 1;
}

.grid-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;

  img {
    object-fit: contain;
  }

  video {
    object-fit: cover;
    border-radius: 2px;
    background: #000;
  }
}

.grid-name {
  font-size: 12px;
  line-height: 22px;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
  color: $primary-text;

  .file-grid-item:hover & {
    font-weight: 550;
  }

  :deep(em) {
    color: $primary;
    font-style: normal;
    font-weight: 600;
  }
}
</style>
