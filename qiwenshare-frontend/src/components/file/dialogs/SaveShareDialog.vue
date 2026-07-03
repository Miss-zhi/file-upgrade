<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderAdd } from '@element-plus/icons-vue'
import type { TreeNode, SaveShareFileDTO } from '@/types/file'
import { getFileTree, saveShareFile } from '@/api/file'
import { useFileOperations } from '@/composables/useFileOperations'

const props = defineProps<{
  visible: boolean
  shareCode: string
  fileName: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const treeData = ref<TreeNode[]>([])
const selectedNodeId = ref<number | null>(null)
const targetPath = ref('/')
const loading = ref(false)
const saving = ref(false)
const fileOps = useFileOperations()

// 新建文件夹：当前正在编辑的节点 ID
const addingFolderNodeId = ref<number | null>(null)
const newFolderName = ref('')
const newFolderInputRef = ref<{ input: HTMLInputElement } | null>(null)

watch(() => props.visible, async (val) => {
  if (val) {
    selectedNodeId.value = null
    targetPath.value = '/'
    addingFolderNodeId.value = null
    newFolderName.value = ''
    loading.value = true
    try {
      treeData.value = await getFileTree()
    } finally {
      loading.value = false
    }
  }
})

function handleNodeClick(node: TreeNode): void {
  selectedNodeId.value = node.userFileId
  targetPath.value = node.filePath || '/'
  addingFolderNodeId.value = null
  newFolderName.value = ''
}

function handleRootClick(): void {
  selectedNodeId.value = null
  targetPath.value = '/'
  addingFolderNodeId.value = null
  newFolderName.value = ''
}

function handleAddFolderBtnClick(node: TreeNode): void {
  addingFolderNodeId.value = node.userFileId
  newFolderName.value = ''
  nextTick(() => {
    newFolderInputRef.value?.input?.focus()
  })
}

async function handleConfirmNewFolder(node: TreeNode): Promise<void> {
  const name = newFolderName.value.trim()
  if (!name) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  if (/[\\/:*?"<>|]/.test(name)) {
    ElMessage.warning('文件夹名称不能包含 \\/:*?"<>| 字符')
    return
  }
  const filePath = node.filePath || '/'
  const id = await fileOps.addFolder(name, filePath)
  if (id != null) {
    addingFolderNodeId.value = null
    newFolderName.value = ''
    loading.value = true
    try {
      treeData.value = await getFileTree()
    } finally {
      loading.value = false
    }
  }
}

function handleCancelNewFolder(): void {
  addingFolderNodeId.value = null
  newFolderName.value = ''
}

async function handleConfirm(): Promise<void> {
  saving.value = true
  try {
    const dto: SaveShareFileDTO = {
      shareCode: props.shareCode,
      targetNodeId: selectedNodeId.value,
    }
    await saveShareFile(dto)
    ElMessage.success('保存成功')
    emit('success')
    emit('update:visible', false)
  } catch (error) {
    console.error('保存分享文件失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="保存文件到网盘"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <p>保存文件：{{ fileName }}</p>

    <!-- 目标路径显示 -->
    <div class="target-path">
      <span class="label">目标路径：</span>
      <el-input v-model="targetPath" readonly size="small" />
    </div>

    <!-- 根目录选项 -->
    <div
      class="root-node"
      :class="{ 'is-selected': selectedNodeId === null }"
      @click="handleRootClick"
    >
      <span>根目录</span>
    </div>

    <!-- 文件目录树 -->
    <div class="tree-container">
      <el-tree
        v-loading="loading"
        :data="treeData"
        :props="{ children: 'children', label: 'fileName' }"
        node-key="userFileId"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        :current-node-key="selectedNodeId"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <span class="node-label">{{ node.label }}</span>
            <template v-if="addingFolderNodeId === data.userFileId">
              <el-input
                ref="newFolderInputRef"
                v-model="newFolderName"
                size="small"
                style="width: 120px"
                placeholder="文件夹名称"
                @click.stop
                @keyup.enter="handleConfirmNewFolder(data)"
                @keyup.escape="handleCancelNewFolder"
              />
              <el-button type="primary" size="small" link @click.stop="handleConfirmNewFolder(data)">
                确定
              </el-button>
              <el-button size="small" link @click.stop="handleCancelNewFolder">
                取消
              </el-button>
            </template>
            <el-button
              v-else
              class="add-folder-btn"
              type="primary"
              size="small"
              link
              @click.stop="handleAddFolderBtnClick(data)"
            >
              <el-icon><FolderAdd /></el-icon>
              新建文件夹
            </el-button>
          </span>
        </template>
      </el-tree>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleConfirm">
        保存到此处
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.target-path {
  display: flex;
  align-items: center;
  margin-top: 8px;
  margin-bottom: 8px;

  .label {
    width: 80px;
    flex-shrink: 0;
    font-size: 14px;
    color: #606266;
  }

  :deep(.el-input) {
    flex: 1;
  }
}

.root-node {
  display: flex;
  align-items: center;
  height: 34px;
  padding: 0 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px;

  &:hover {
    background: #f5f7fa;
  }

  &.is-selected {
    color: #409eff;
    background: #ecf5ff;
  }
}

.tree-container {
  height: 300px;
  overflow: auto;
  margin-top: 8px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;

  .node-label {
    flex: 1;
  }

  .add-folder-btn {
    display: none;
    font-size: 12px;
  }
}

:deep(.el-tree-node__content:hover) {
  .add-folder-btn {
    display: inline-flex;
  }
}
</style>
