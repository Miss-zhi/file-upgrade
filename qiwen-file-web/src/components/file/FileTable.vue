<script setup lang="ts">
interface FileItem {
  id: string
  fileName: string
  filePath: string
  fileSize: number | null
  fileType: string
  isFolder: boolean
  createTime: string
}

defineProps<{
  files: FileItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  enter: [path: string]
  rename: [file: FileItem]
  move: [file: FileItem]
  copy: [file: FileItem]
  delete: [file: FileItem]
  download: [file: FileItem]
  share: [file: FileItem]
  preview: [file: FileItem]
  'selection-change': [files: FileItem[]]
}>()

function formatSize(bytes: number | null): string {
  if (bytes == null || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let s = bytes, u = 0
  while (s >= 1024 && u < units.length - 1) { s /= 1024; u++ }
  return s.toFixed(1) + ' ' + units[u]
}

function handleSelectionChange(rows: FileItem[]) { emit('selection-change', rows) }
function onEnter(row: FileItem) { emit('enter', row.filePath) }
function onRename(row: FileItem) { emit('rename', row) }
function onMove(row: FileItem) { emit('move', row) }
function onCopy(row: FileItem) { emit('copy', row) }
function onDelete(row: FileItem) { emit('delete', row) }
function onDownload(row: FileItem) { emit('download', row) }
function onShare(row: FileItem) { emit('share', row) }
function isPreviewable(fileName: string): boolean {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  return /jpg|jpeg|png|gif|webp|svg|mp4|webm|txt|md|json|pdf|xml|html|css|js/i.test(ext)
}
</script>

<template>
  <el-table :data="files" v-loading="loading" style="width: 100%" @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50" />
    <el-table-column label="文件名" min-width="300">
      <template #default="{ row }: any">
        <div class="file-name-cell" :class="{ clickable: row.isFolder }" @click="row.isFolder && onEnter(row)">
          <el-icon><component :is="row.isFolder ? 'Folder' : 'Document'" /></el-icon>
          <span>{{ row.fileName }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="大小" width="120">
      <template #default="{ row }: any">{{ row.isFolder ? '-' : formatSize(row.fileSize) }}</template>
    </el-table-column>
    <el-table-column label="类型" width="100">
      <template #default="{ row }: any">{{ row.isFolder ? '文件夹' : row.fileType || '-' }}</template>
    </el-table-column>
    <el-table-column prop="createTime" label="创建时间" width="180" />
    <el-table-column label="操作" width="240" fixed="right">
      <template #default="{ row }: any">
        <el-button v-if="!row.isFolder" link type="primary" size="small" @click="onDownload(row)">下载</el-button>
        <el-button v-if="!row.isFolder" link type="success" size="small" @click="$router.push(`/onlyoffice/${row.id}`)">编辑</el-button>
        <el-button v-if="!row.isFolder && isPreviewable(row.fileName)" link type="warning" size="small" @click="onShare(row)">分享</el-button>
        <el-dropdown trigger="click">
          <el-button link type="info" size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onRename(row)">重命名</el-dropdown-item>
              <el-dropdown-item @click="onMove(row)">移动到</el-dropdown-item>
              <el-dropdown-item @click="onCopy(row)">复制</el-dropdown-item>
              <el-dropdown-item divided @click="onDelete(row)">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </el-table-column>
  </el-table>
</template>

<style lang="stylus" scoped>
.file-name-cell
  display: flex
  align-items: center
  gap: 8px
  &.clickable
    cursor: pointer
    color: #409eff
</style>
