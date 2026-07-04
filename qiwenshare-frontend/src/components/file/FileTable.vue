<script setup lang="ts">
import { computed } from 'vue'
import { useFileListStore } from '@/stores/fileList'
import { useCommonStore } from '@/stores/common'
import { FileType } from '@/types/file'
import type { FileInfo, ShareInfo } from '@/types/file'
import { formatFileSize, getFileIconSrc, isVideoFile, isFolder } from '@/utils/file'

const fileListStore = useFileListStore()
const commonStore = useCommonStore()

const props = defineProps<{
  fileType: number
  highlightMap?: Map<number, string>
}>()

const emit = defineEmits<{
  (e: 'openFile', file: FileInfo): void
  (e: 'contextMenu', event: MouseEvent, file: FileInfo): void
  (e: 'sortChange', prop: string, order: string): void
}>()

/** 是否显示选择列 */
const showSelection = computed(() => props.fileType !== FileType.SHARE)

/** 是否显示路径列 */
const showPath = computed(() => {
  return props.fileType !== FileType.IMAGE &&
    props.fileType !== FileType.DOCUMENT &&
    props.fileType !== FileType.VIDEO &&
    props.fileType !== FileType.MUSIC &&
    props.fileType !== FileType.OTHER &&
    props.fileType !== FileType.SHARE &&
    commonStore.screenWidth > 768
})

/** 是否显示类型列 */
const showExtendName = computed(() =>
  fileListStore.selectedColumnList.includes('extendName'),
)

/** 是否显示大小列 */
const showFileSize = computed(() =>
  fileListStore.selectedColumnList.includes('fileSize'),
)

/** 是否显示修改日期列 */
const showUploadTime = computed(() =>
  fileListStore.selectedColumnList.includes('uploadTime'),
)

/** 是否显示删除日期列（仅回收站） */
const showDeleteTime = computed(() =>
  props.fileType === FileType.RECYCLE,
)

/** 是否显示分享专属列（过期时间、提取码） */
const isShareView = computed(() => props.fileType === FileType.SHARE)

/** 表格高度 */
const tableHeight = computed(() => {
  if (props.fileType === FileType.RECYCLE) return 'calc(100vh - 211px)'
  if (props.fileType === FileType.SHARE) return 'calc(100vh - 109px)'
  return 'calc(100vh - 206px)'
})

/** 行双击打开文件/文件夹 */
function handleRowDblClick(row: FileInfo): void {
  emit('openFile', row)
}

/** 行右键菜单 */
function handleRowContextMenu(row: FileInfo, column: any, event: MouseEvent): void {
  if (commonStore.screenWidth > 768) {
    event.preventDefault()
    event.stopPropagation()
    emit('contextMenu', event, row)
  }
}

/** 选择变化 */
function handleSelectionChange(rows: FileInfo[]): void {
  fileListStore.selectedFiles = rows
  fileListStore.isBatchOperation = rows.length > 0
}

/** 排序变化 */
function handleSortChange({ prop, order }: { prop: string; order: string }): void {
  emit('sortChange', prop, order || 'ascending')
}

/** 自定义排序：文件夹始终在前 */
function sortMethod(a: FileInfo, b: FileInfo): number {
  const aIsDir = isFolder(a.fileType) ? 0 : 1
  const bIsDir = isFolder(b.fileType) ? 0 : 1
  return aIsDir - bIsDir
}

/** 根据 userFileId 获取对应的 ShareInfo */
function getShareInfo(userFileId: number): ShareInfo | undefined {
  return fileListStore.shareList.find((s) => s.userFileId === userFileId)
}
</script>

<template>
  <el-table
    :data="fileListStore.fileList"
    v-loading="fileListStore.loading"
    fit
    highlight-current-row
    :height="tableHeight"
    :sort-method="sortMethod"
    class="file-table"
    @row-dblclick="handleRowDblClick"
    @row-contextmenu="handleRowContextMenu"
    @selection-change="handleSelectionChange"
    @sort-change="handleSortChange"
  >
    <!-- 选择列 -->
    <el-table-column
      v-if="showSelection"
      type="selection"
      width="56"
      align="center"
    />

    <!-- 图标列 -->
    <el-table-column width="56" align="center">
      <template #default="{ row }">
        <video
          v-if="isVideoFile(row.extendName)"
          :src="getFileIconSrc(row.extendName, row.fileType, row.userFileId)"
          :alt="row.fileName"
          title="点击预览"
          class="file-icon"
          muted
          preload="metadata"
        />
        <img
          v-else
          :src="getFileIconSrc(row.extendName, row.fileType, row.userFileId)"
          :alt="row.fileName"
          title="点击预览"
          class="file-icon"
        />
      </template>
    </el-table-column>

    <!-- 文件名 -->
    <el-table-column
      prop="fileName"
      label="文件名"
      sortable="custom"
      show-overflow-tooltip
      min-width="200"
    >
      <template #default="{ row }">
        <span class="file-name" :class="{ 'is-folder': isFolder(row.fileType) }" title="点击预览">
          <span v-if="highlightMap?.has(row.userFileId)" v-html="highlightMap.get(row.userFileId)" />
          <template v-else>{{ row.fileName }}</template>
        </span>
      </template>
    </el-table-column>

    <!-- 路径 -->
    <el-table-column
      v-if="showPath"
      prop="filePath"
      label="路径"
      show-overflow-tooltip
      width="200"
    />

    <!-- 类型 -->
    <el-table-column
      v-if="showExtendName"
      prop="extendName"
      label="类型"
      width="80"
    />

    <!-- 大小 -->
    <el-table-column
      v-if="showFileSize"
      prop="fileSize"
      label="大小"
      width="100"
      align="right"
    >
      <template #default="{ row }">
        {{ isFolder(row.fileType) ? '' : formatFileSize(row.fileSize) }}
      </template>
    </el-table-column>

    <!-- 修改日期 -->
    <el-table-column
      v-if="showUploadTime"
      prop="uploadTime"
      label="修改日期"
      width="160"
      sortable="custom"
    />

    <!-- 删除日期（回收站） -->
    <el-table-column
      v-if="showDeleteTime"
      prop="deleteTime"
      label="删除日期"
      width="160"
    />

    <!-- 分享专属列：过期时间 -->
    <el-table-column
      v-if="isShareView"
      label="过期时间"
      width="180"
    >
      <template #default="{ row }">
        <span v-if="getShareInfo(row.userFileId)?.expireTime">
          {{ getShareInfo(row.userFileId)?.expireTime }}
          <el-tag
            v-if="getShareInfo(row.userFileId)?.isExpired"
            type="danger"
            size="small"
            style="margin-left: 4px"
          >已过期</el-tag>
          <el-tag v-else type="success" size="small" style="margin-left: 4px">有效</el-tag>
        </span>
        <span v-else>永久有效</span>
      </template>
    </el-table-column>

    <!-- 分享专属列：提取码 -->
    <el-table-column
      v-if="isShareView"
      label="提取码"
      width="100"
    >
      <template #default="{ row }">
        <span>{{ getShareInfo(row.userFileId)?.extractCode || '-' }}</span>
      </template>
    </el-table-column>

    <!-- 更多（移动端） -->
    <el-table-column
      v-if="commonStore.screenWidth <= 768"
      width="48"
      align="center"
    >
      <template #default>
        <el-icon><More /></el-icon>
      </template>
    </el-table-column>
  </el-table>
</template>

<style lang="scss" scoped>
.file-table {
  :deep(.el-table__header) {
    th {
      padding: 4px 0;
      background: #fff;
      color: $secondary-text;
      font-size: 13px;
    }
  }

  :deep(.el-table__row) {
    td {
      padding: 8px 0;
    }

    &:hover td {
      background: $tab-back-color;
    }
  }

  :deep(.el-table__body-wrapper) {
    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: #C0C4CC;
      border-radius: 3px;
    }
  }
}

.file-icon {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

video.file-icon {
  object-fit: cover;
  border-radius: 2px;
  background: #000;
}

.file-name {
  cursor: pointer;

  &.is-folder {
    font-weight: 500;
  }

  &:hover {
    color: $primary;
  }

  :deep(em) {
    color: $primary;
    font-style: normal;
    font-weight: 600;
  }
}
</style>
