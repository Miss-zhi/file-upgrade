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
}>()

function formatSize(bytes: number | null): string {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit++
  }
  return size.toFixed(1) + ' ' + units[unit]
}

function onEnter(row: FileItem) { emit('enter', row.filePath) }
function onRename(row: FileItem) { emit('rename', row) }
function onMove(row: FileItem) { emit('move', row) }
function onCopy(row: FileItem) { emit('copy', row) }
function onDelete(row: FileItem) { emit('delete', row) }
function onDownload(row: FileItem) { emit('download', row) }
function isEditable(fileType: string): boolean {
  if (!fileType) return false
  return /doc|docx|xls|xlsx|ppt|pptx|odt|ods|odp|pdf/i.test(fileType)
}
</script>
<template>
  <el-table :data="files" v-loading="loading" style="width: 100%">
    <el-table-column label="文件名" min-width="300">
      <template #default="{ row }: any">
        <div
          class="file-name-cell"
          :class="{ 'is-folder': row.isFolder }"
          @click="row.isFolder ? onEnter(row) : null"
        >
          <el-icon :size="20">
            <Folder v-if="row.isFolder" />
            <Document v-else />
          </el-icon>
          <span>{{ row.fileName }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="大小" width="120" align="right">
      <template #default="{ row }: any">{{ row.isFolder ? '-' : formatSize(row.fileSize) }}</template>
    </el-table-column>
    <el-table-column label="修改时间" width="200">
      <template #default="{ row }: any">{{ row.createTime }}</template>
    </el-table-column>
    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{ row }: any">
        <el-button v-if="!row.isFolder" link type="primary" size="small" @click="onDownload(row)">
          下载
        </el-button>
        <el-button v-if="!row.isFolder && isEditable(row.fileType)" link type="success" size="small" @click="$router.push(`/onlyoffice/${row.id}`)">
          编辑
        </el-button>
        <el-dropdown trigger="click">
          <el-button link type="info" size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onRename(row)">重命名</el-dropdown-item>
              <el-dropdown-item @click="onMove(row)">移动到</el-dropdown-item>
              <el-dropdown-item @click="onCopy(row)">复制到</el-dropdown-item>
              <el-dropdown-item @click="emit('share', row)">分享</el-dropdown-item>
              <el-dropdown-item divided @click="onDelete(row)">
                <span style="color: #f56c6c">删除</span>
              </el-dropdown-item>
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
  cursor: default
  &.is-folder
    cursor: pointer
    color: #409eff
    &:hover
      text-decoration: underline
</style>
