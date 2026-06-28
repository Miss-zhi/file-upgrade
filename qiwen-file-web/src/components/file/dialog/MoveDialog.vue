<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFileListByPath } from '_api/file'

interface FileItem {
  id: string
  fileName: string
  filePath: string
  isFolder: boolean
}

interface FolderNode {
  name: string
  path: string
  children?: FolderNode[]
}

const visible = ref(false)
const file = ref<FileItem | null>(null)
const treeData = ref<FolderNode[]>([])
const selectedPath = ref('/')

const emit = defineEmits<{
  confirm: [id: string, targetPath: string]
}>()

function open(item: FileItem) {
  file.value = item
  visible.value = true
  loadFolders('/')
}

async function loadFolders(path: string) {
  try {
    const res: any = await getFileListByPath({ path })
    if (res.success && res.data) {
      const folders = res.data.filter((f: any) => f.isFolder)
      const nodes: FolderNode[] = [
        { name: '根目录', path: '/' },
        ...folders.map((f: any) => ({
          name: f.fileName,
          path: f.filePath
        }))
      ]
      if (path === '/') {
        treeData.value = nodes
      }
    }
  } catch {
    // handled
  }
}

function handleConfirm() {
  if (file.value && selectedPath.value) {
    emit('confirm', file.value.id, selectedPath.value)
    visible.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="移动到" width="450px">
    <p style="margin-bottom:12px;color:#606266">
      将 "{{ file?.fileName }}" 移动到：
    </p>
    <el-tree
      :data="treeData"
      :props="{ label: 'name', children: 'children' }"
      node-key="path"
      highlight-current
      @node-click="(data: any) => selectedPath = data.path"
    />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">移动到此处</el-button>
    </template>
  </el-dialog>
</template>
