<script setup lang="ts">
interface FileItem {
  id: string
  fileName: string
  filePath: string
  fileSize: number
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
  delete: [id: string, name: string]
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
</script>

<template>
  <el-table
    :data="files"
    v-loading="loading"
    style="width: 100%"
  >
    <el-table-column label="文件名" min-width="300">
      <template #default="{ row }">
        <div
          class="file-name-cell"
          :class="{ 'is-folder': row.isFolder }"
          @click="row.isFolder ? emit('enter', row.filePath) : null"
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
      <template #default="{ row }">
        {{ row.isFolder ? '-' : formatSize(row.fileSize) }}
      </template>
    </el-table-column>

    <el-table-column label="修改时间" width="200">
      <template #default="{ row }">
        {{ row.createTime }}
      </template>
    </el-table-column>

    <el-table-column label="操作" width="120" fixed="right">
      <template #default="{ row }">
        <el-button
          v-if="!row.isFolder"
          link
          type="primary"
          size="small"
        >
          下载
        </el-button>
        <el-button
          link
          type="danger"
          size="small"
          @click="emit('delete', row.id, row.fileName)"
        >
          删除
        </el-button>
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
