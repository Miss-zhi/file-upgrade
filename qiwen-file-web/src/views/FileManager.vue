<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useFileListStore } from '@/stores/fileList'
import { ElMessageBox, ElMessage } from 'element-plus'
import { batchDeleteFiles, batchMoveFiles } from '_api/file'
import { createShare } from '_api/share'
import AsideMenu from '_c/file/AsideMenu.vue'
import FileTable from '_c/file/FileTable.vue'
import BreadCrumb from '_c/common/BreadCrumb.vue'
import UploadDialog from '_c/file/dialog/UploadDialog.vue'
import ShareDialog from '_c/file/dialog/ShareDialog.vue'
import PreviewDialog from '_c/file/dialog/PreviewDialog.vue'
import RenameDialog from '_c/file/dialog/RenameDialog.vue'
import MoveDialog from '_c/file/dialog/MoveDialog.vue'
import CopyDialog from '_c/file/dialog/CopyDialog.vue'
import DeleteDialog from '_c/file/dialog/DeleteDialog.vue'

const fileListStore = useFileListStore()
const { files, currentPath, loading } = storeToRefs(fileListStore)

const selectedFiles = ref<any[]>([])

// 对话框引用
const uploadRef = ref<InstanceType<typeof UploadDialog>>()
const shareRef = ref<InstanceType<typeof ShareDialog>>()
const previewRef = ref<InstanceType<typeof PreviewDialog>>()
const renameRef = ref<InstanceType<typeof RenameDialog>>()
const moveRef = ref<InstanceType<typeof MoveDialog>>()
const copyRef = ref<InstanceType<typeof CopyDialog>>()
const deleteRef = ref<InstanceType<typeof DeleteDialog>>()

// 导航
function handleNavigate(path: string) {
  fileListStore.fetchFiles(path)
}

// 进入文件夹
function handleEnter(path: string) {
  fileListStore.fetchFiles(path)
}

// 上传
function handleUploadConfirm(file: File, path: string) {
  fileListStore.uploadFile(file, path)
}

async function handleCreateFolder() {
  try {
    const result = await ElMessageBox.prompt('请输入文件夹名称', '新建', {
      confirmButtonText: '创建',
      cancelButtonText: '取消'
    })
    const folderName: string = (result as any).value
    if (folderName) {
      await fileListStore.createFolder(currentPath.value, folderName)
    }
  } catch { /* 取消 */ }
}

// 文件操作
function handleRename(file: any) { renameRef.value?.open(file) }
async function handleRenameConfirm(id: string, newName: string) {
  await fileListStore.renameFile(id, newName)
  ElMessage.success('重命名成功')
  fileListStore.fetchFiles(currentPath.value)
}

function handleMove(file: any) { moveRef.value?.open(file) }
async function handleMoveConfirm(id: string, targetPath: string) {
  await fileListStore.moveFile(id, targetPath)
  ElMessage.success('移动成功')
  fileListStore.fetchFiles(currentPath.value)
}

function handleCopy(file: any) { copyRef.value?.open(file) }
async function handleCopyConfirm(id: string, targetPath: string) {
  await fileListStore.copyFile(id, targetPath)
  ElMessage.success('复制成功')
  fileListStore.fetchFiles(currentPath.value)
}

function handleDelete(file: any) { deleteRef.value?.open(file) }
async function handleDeleteConfirm(id: string) {
  await fileListStore.deleteFile(id)
  ElMessage.success('删除成功')
  fileListStore.fetchFiles(currentPath.value)
}

function handleShare(file: any) { shareRef.value?.open(file.filePath) }
function handlePreview(file: any) { previewRef.value?.open(file) }

async function handleBatchDelete() {
  const ids = selectedFiles.value.map((f: any) => f.id)
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个文件吗？`, '批量删除', { type: 'warning' })
  const res: any = await batchDeleteFiles(ids)
  if (res.success) { ElMessage.success('批量删除成功'); selectedFiles.value = []; fileListStore.fetchFiles(currentPath.value) }
}

async function handleBatchMove() {
  const { value: targetPath } = await ElMessageBox.prompt('输入目标路径（如 /docs）', '批量移动')
  const ids = selectedFiles.value.map((f: any) => f.id)
  const res: any = await batchMoveFiles(ids, targetPath || '/')
  if (res.success) { ElMessage.success('批量移动成功'); selectedFiles.value = []; fileListStore.fetchFiles(currentPath.value) }
}
async function handleShareConfirm(data: any) {
  const res: any = await createShare(data)
  if (res.success) {
    shareRef.value?.setResult(window.location.origin + res.data.link)
  }
}

function handleDownload(file: any) {
  ElMessage.info('下载功能开发中')
}

function handleRefresh() {
  fileListStore.fetchFiles(currentPath.value)
}

onMounted(() => {
  fileListStore.fetchFiles('/')
})
</script>

<template>
  <div class="file-manager">
    <!-- 左侧目录树 -->
    <AsideMenu
      :current-path="currentPath"
      @select="handleNavigate"
    />

    <!-- 右侧操作区 -->
    <div class="main-area">
      <!-- 面包屑 -->
      <BreadCrumb
        :path="currentPath"
        @navigate="handleNavigate"
      />

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="uploadRef?.open(currentPath)">
            上传文件
          </el-button>
          <el-button @click="handleCreateFolder">
            新建文件夹
          </el-button>
        </div>
        <el-button :icon="'Refresh'" circle @click="handleRefresh" />
      </div>

      <!-- 文件表格 -->
      <BatchToolbar
        v-if="selectedFiles.length"
        :count="selectedFiles.length"
        @delete="handleBatchDelete"
        @move="handleBatchMove"
        @clear="selectedFiles = []"
      />
      <FileTable
        :files="files"
        :loading="loading"
        @enter="handleEnter"
        @rename="handleRename"
        @move="handleMove"
        @copy="handleCopy"
        @delete="handleDelete"
        @download="handleDownload"
        @share="handleShare"
        @preview="handlePreview"
        @selection-change="selectedFiles = $event"
      />
    </div>

    <!-- 对话框层 -->
    <UploadDialog ref="uploadRef" @confirm="handleUploadConfirm" />
    <ShareDialog ref="shareRef" @confirm="handleShareConfirm" />
    <PreviewDialog ref="previewRef" />
    <RenameDialog ref="renameRef" @confirm="handleRenameConfirm" />
    <MoveDialog ref="moveRef" @confirm="handleMoveConfirm" />
    <CopyDialog ref="copyRef" @confirm="handleCopyConfirm" />
    <DeleteDialog ref="deleteRef" @confirm="handleDeleteConfirm" />
  </div>
</template>

<style lang="stylus" scoped>
.file-manager
  display: flex
  height: calc(100vh - 60px)

  .main-area
    flex: 1
    padding: 16px 20px
    overflow-y: auto

    .toolbar
      display: flex
      justify-content: space-between
      align-items: center
      margin: 12px 0
      padding: 10px 16px
      background: #f5f7fa
      border-radius: 4px

      &-left
        display: flex
        gap: 8px
</style>
