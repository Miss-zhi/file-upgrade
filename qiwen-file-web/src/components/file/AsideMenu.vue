<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFileListByPath } from '_api/file'

interface TreeNode {
  name: string
  path: string
  children?: TreeNode[]
  leaf?: boolean
}

const props = defineProps<{
  currentPath: string
}>()

const emit = defineEmits<{
  select: [path: string]
}>()

const treeData = ref<TreeNode[]>([])
const loading = ref(false)

async function loadChildren(node: TreeNode): Promise<void> {
  try {
    const res: any = await getFileListByPath({ path: node.path })
    if (res.success && res.data) {
      const folders = res.data.filter((f: any) => f.isFolder)
      node.children = folders.map((f: any) => ({
        name: f.fileName,
        path: f.filePath,
        children: [],
        leaf: false
      }))
    }
  } catch {
    // handled
  }
}

async function loadRoot() {
  loading.value = true
  treeData.value = []
  try {
    const res: any = await getFileListByPath({ path: '/' })
    if (res.success && res.data) {
      const folders = res.data.filter((f: any) => f.isFolder)
      treeData.value = folders.map((f: any) => ({
        name: f.fileName,
        path: f.filePath,
        children: [],
        leaf: false
      }))
    }
  } catch {
    // handled
  }
  loading.value = false
}

function handleNodeClick(data: TreeNode) {
  emit('select', data.path)
}

function handleNodeExpand(data: TreeNode) {
  loadChildren(data)
}

onMounted(loadRoot)
</script>

<template>
  <div class="aside-menu" v-loading="loading">
    <div class="aside-title">目录</div>
    <el-tree
      :data="treeData"
      :props="{ label: 'name', children: 'children' }"
      node-key="path"
      :highlight-current="true"
      :default-expanded-keys="[]"
      lazy
      :load="(node: any, resolve: any) => {
        if (node.level === 0) return resolve(treeData)
        loadChildren(node.data).then(() => resolve(node.data.children))
      }"
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon><Folder /></el-icon>
          <span>{{ data.name }}</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<style lang="stylus" scoped>
.aside-menu
  width: 240px
  border-right: 1px solid #e4e7ed
  padding: 12px
  overflow-y: auto

  .aside-title
    font-size: 16px
    font-weight: bold
    color: #303133
    margin-bottom: 12px
    padding-bottom: 8px
    border-bottom: 1px solid #e4e7ed

  .tree-node
    display: flex
    align-items: center
    gap: 6px
    font-size: 14px
    color: #606266
</style>
