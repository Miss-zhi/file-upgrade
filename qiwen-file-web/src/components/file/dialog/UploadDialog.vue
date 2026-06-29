<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile } from 'element-plus'
import { uploadChunk, mergeChunks } from '_api/filetransfer'
import { ElMessage } from 'element-plus'

const CHUNK_SIZE = 2 * 1024 * 1024

interface FileTask {
  file: UploadFile
  progress: number
  status: 'pending' | 'uploading' | 'done' | 'error'
}

const visible = ref(false)
const currentPath = ref('/')
const tasks = ref<FileTask[]>([])

defineEmits<{ confirm: [] }>()

function open(path: string) { currentPath.value = path; visible.value = true }

async function handleUpload(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  const task: FileTask = { file, progress: 0, status: 'uploading' }
  tasks.value.push(task)

  try {
    const totalChunks = Math.ceil(raw.size / CHUNK_SIZE)
    const identifier = raw.name + raw.size + Date.now()

    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, raw.size)
      const chunk = raw.slice(start, end)

      await uploadChunk(chunk, {
        chunkNum: i, totalChunks, identifier,
        fileName: raw.name,
        filePath: currentPath.value.replace(/\/$/, '') + '/' + raw.name,
        totalSize: raw.size
      })
      task.progress = Math.round(((i + 1) / totalChunks) * 100)
    }

    await mergeChunks(identifier, currentPath.value.replace(/\/$/, '') + '/' + raw.name)
    task.status = 'done'
    ElMessage.success(`${raw.name} 上传完成`)
  } catch {
    task.status = 'error'
    ElMessage.error(`${raw.name} 上传失败`)
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="上传文件" width="500px" destroy-on-close>
    <el-upload
      drag
      multiple
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleUpload"
      style="width:100%"
    >
      <el-icon :size="48" color="#409eff"><UploadFilled /></el-icon>
      <div class="upload-text">拖拽文件到此处 或 <em>点击选择</em></div>
    </el-upload>

    <div v-if="tasks.length" class="task-list">
      <div v-for="(t, i) in tasks" :key="i" class="task-item">
        <div class="task-header">
          <span>{{ t.file.name }}</span>
          <el-tag v-if="t.status === 'done'" type="success" size="small">完成</el-tag>
          <el-tag v-else-if="t.status === 'error'" type="danger" size="small">失败</el-tag>
          <el-tag v-else type="info" size="small">{{ t.progress }}%</el-tag>
        </div>
        <el-progress
          :percentage="t.progress"
          :status="t.status === 'error' ? 'exception' : t.status === 'done' ? 'success' : undefined"
          :stroke-width="6"
        />
      </div>
    </div>
  </el-dialog>
</template>

<style lang="stylus" scoped>
.upload-text
  margin-top: 12px
  color: #909399
  font-size: 14px
  em
    color: #409eff
    font-style: normal

.task-list
  margin-top: 16px

  .task-item
    margin-bottom: 12px

    .task-header
      display: flex
      justify-content: space-between
      align-items: center
      margin-bottom: 6px
      font-size: 13px
</style>
