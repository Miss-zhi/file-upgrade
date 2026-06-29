<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFileListByPath, deleteFile } from '_api/file'
import http from '_api/http'

interface FileItem {
  id: string
  fileName: string
  filePath: string
  fileSize: number
  createTime: string
}

const files = ref<FileItem[]>([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  const res: any = await http.post('/file/recycle')
  if (res.success) files.value = res.data || []
  loading.value = false
}

async function handleRestore(file: FileItem) {
  const res: any = await http.post('/file/restore', { id: file.id })
  if (res.success) { ElMessage.success('已恢复'); fetchData() }
}

async function handlePermanentDelete(file: FileItem) {
  await ElMessageBox.confirm(`彻底删除 "${file.fileName}" 后不可恢复`, '确认', { type: 'warning' })
  const res: any = await http.delete(`/file/permanent/${file.id}`)
  if (res.success) { ElMessage.success('已彻底删除'); fetchData() }
}

onMounted(fetchData)
</script>

<template>
  <div class="recycle-page">
    <h2>回收站</h2>
    <el-table :data="files" v-loading="loading" style="width:100%">
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column label="大小" width="120"><template #default="{ row }">{{ row.fileSize }} B</template></el-table-column>
      <el-table-column prop="createTime" label="删除时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleRestore(row)">恢复</el-button>
          <el-button link type="danger" size="small" @click="handlePermanentDelete(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style lang="stylus" scoped>
.recycle-page
  padding: 24px
  h2
    margin-bottom: 16px
</style>
