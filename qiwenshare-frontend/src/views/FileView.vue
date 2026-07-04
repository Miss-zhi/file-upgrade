<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, provide } from 'vue'
import { IMAGE_PREVIEW_KEY, VIDEO_PREVIEW_KEY, AUDIO_PREVIEW_KEY, CODE_PREVIEW_KEY, MARKDOWN_PREVIEW_KEY } from '@/composables/previewKeys'
import { useRoute, useRouter } from 'vue-router'
import { useFileListStore } from '@/stores/fileList'
import { useUploadFileStore } from '@/stores/uploadFile'
import { useCommonStore } from '@/stores/common'
import { FileType, FileViewMode } from '@/types/file'
import type { FileInfo } from '@/types/file'
import { isFolder, isOffice, isMarkdown } from '@/utils/file'
import { downloadFile, batchDownload, cancelShare } from '@/api/file'
import { ElMessage } from 'element-plus'
import { useUploadManager } from '@/composables/useUploadManager'
import { useImagePreview } from '@/composables/useImagePreview'
import { useVideoPreview } from '@/composables/useVideoPreview'
import { useAudioPreview } from '@/composables/useAudioPreview'
import { useCodePreview } from '@/composables/useCodePreview'
import { useMarkdownPreview } from '@/composables/useMarkdownPreview'
import { UploadFilled, Close } from '@element-plus/icons-vue'
import { usePreviewRouter, toPreviewFileItem } from '@/composables/usePreviewRouter'
import { useSearch } from '@/composables/useSearch'

// 预览 composable 实例
const imagePreview = useImagePreview()
provide(IMAGE_PREVIEW_KEY, imagePreview)
const videoPreview = useVideoPreview()
provide(VIDEO_PREVIEW_KEY, videoPreview)
const audioPreview = useAudioPreview()
provide(AUDIO_PREVIEW_KEY, audioPreview)
const codePreview = useCodePreview()
provide(CODE_PREVIEW_KEY, codePreview)
const markdownPreview = useMarkdownPreview()
provide(MARKDOWN_PREVIEW_KEY, markdownPreview)
const { openFilePreview, openOfficePreview } = usePreviewRouter()

// 组件导入
import OperationMenu from '@/components/file/OperationMenu.vue'
import BreadCrumb from '@/components/file/BreadCrumb.vue'
import FileTable from '@/components/file/FileTable.vue'
import FileGrid from '@/components/file/FileGrid.vue'
import FileTimeLine from '@/components/file/FileTimeLine.vue'
import Pagination from '@/components/file/Pagination.vue'
import ContextMenu from '@/components/file/ContextMenu.vue'
import UploadPanel from '@/components/file/UploadPanel.vue'
import AddFolderDialog from '@/components/file/dialogs/AddFolderDialog.vue'
import RenameDialog from '@/components/file/dialogs/RenameDialog.vue'
import DeleteDialog from '@/components/file/dialogs/DeleteDialog.vue'
import CopyFileDialog from '@/components/file/dialogs/CopyFileDialog.vue'
import MoveFileDialog from '@/components/file/dialogs/MoveFileDialog.vue'
import ShareDialog from '@/components/file/dialogs/ShareDialog.vue'
import FileDetailDialog from '@/components/file/dialogs/FileDetailDialog.vue'
import RestoreDialog from '@/components/file/dialogs/RestoreDialog.vue'
import UnzipDialog from '@/components/file/dialogs/UnzipDialog.vue'
import ImagePreview from '@/components/preview/ImagePreview.vue'
import VideoPreview from '@/components/preview/VideoPreview.vue'
import AudioPreview from '@/components/preview/AudioPreview.vue'
import CodePreview from '@/components/preview/CodePreview.vue'
import MarkdownPreview from '@/components/preview/MarkdownPreview.vue'

const route = useRoute()
const router = useRouter()
const fileListStore = useFileListStore()
const uploadFileStore = useUploadFileStore()
const commonStore = useCommonStore()
const uploadManager = useUploadManager()
const search = useSearch()

// ---- 路由参数 ----

const currentFileType = computed(() => Number(route.query.fileType) || FileType.ALL)
const currentFilePath = computed(() => (route.query.filePath as string) || '/')

// ---- 右键菜单状态 ----

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuFile = ref<FileInfo | null>(null)

// ---- 弹窗状态 ----

const addFolderVisible = ref(false)
const renameVisible = ref(false)
const renameFile = ref<FileInfo | null>(null)
const deleteVisible = ref(false)
const deleteFiles = ref<FileInfo[]>([])
const deleteMode = ref<1 | 2>(1)
const copyVisible = ref(false)
const copyFile = ref<FileInfo | null>(null)
const moveVisible = ref(false)
const moveFiles = ref<FileInfo[]>([])
const shareVisible = ref(false)
const shareFile = ref<FileInfo | null>(null)
const detailVisible = ref(false)
const detailFile = ref<FileInfo | null>(null)
const restoreVisible = ref(false)
const restoreIds = ref<number[]>([])
const unzipVisible = ref(false)
const unzipFile = ref<FileInfo | null>(null)

// ---- 拖拽上传 ----
// 用计数器解决 dragenter/dragleave 在子元素间移动时频繁触发导致遮罩闪烁的问题

let dragCounter = 0

function handleDragEnter(): void {
  dragCounter++
  if (dragCounter === 1) {
    uploadFileStore.showUploadMask = true
  }
}

function handleDragLeave(): void {
  dragCounter--
  if (dragCounter <= 0) {
    dragCounter = 0
    uploadFileStore.showUploadMask = false
  }
}

function handleDragOver(e: DragEvent): void {
  e.preventDefault()
}

function handleDrop(e: DragEvent): void {
  e.preventDefault()
  dragCounter = 0
  uploadFileStore.showUploadMask = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    uploadManager.uploadFiles(Array.from(files), currentFilePath.value, refreshFileList)
  }
}

// ---- 文件操作 ----

function refreshFileList(): void {
  fileListStore.fetchFileList({
    fileType: currentFileType.value,
    filePath: currentFilePath.value,
    page: 0,
    size: 20,
  })
}

function handleOpenFile(file: FileInfo): void {
  if (isFolder(file.fileType)) {
    // 进入文件夹
    const newPath = file.filePath === '/'
      ? `/${file.fileName}`
      : `${file.filePath}/${file.fileName}`
    router.push({
      path: '/file',
      query: { fileType: String(currentFileType.value), filePath: newPath },
    })
  } else {
    // 预览路由：根据文件类型路由到对应预览
    const handled = openFilePreview(file, fileListStore.fileList, {
      onImage: (f, list) => imagePreview.open(f, list),
      onVideo: (f, list) => videoPreview.open(f, list),
      onAudio: (f, list) => audioPreview.open(f, list),
      onCode: (f) => codePreview.open(f),
      onMarkdown: (f) => markdownPreview.open(f),
    })
    // Office/PDF 在 openFilePreview 内部处理（新标签页打开）
    // 未识别类型在 openFilePreview 内部走下载
  }
}

function handleContextMenu(e: MouseEvent, file: FileInfo): void {
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuFile.value = file
  contextMenuVisible.value = true
}

function handleBlankContextMenu(e: MouseEvent): void {
  e.preventDefault()
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuFile.value = null
  contextMenuVisible.value = true
}

// ---- 右键菜单动作 ----

function handleContextAction(action: string, file?: FileInfo): void {
  switch (action) {
    case 'view':
      if (file) handleOpenFile(file)
      break
    case 'delete':
      if (file) {
        deleteFiles.value = [file]
        deleteMode.value = 1
        deleteVisible.value = true
      }
      break
    case 'permanentDelete':
      if (file) {
        deleteFiles.value = [file]
        deleteMode.value = 2
        deleteVisible.value = true
      }
      break
    case 'restore':
      if (file) {
        restoreIds.value = [file.userFileId]
        restoreVisible.value = true
      }
      break
    case 'copy':
      if (file) {
        copyFile.value = file
        copyVisible.value = true
      }
      break
    case 'move':
      if (file) {
        moveFiles.value = [file]
        moveVisible.value = true
      }
      break
    case 'rename':
      if (file) {
        renameFile.value = file
        renameVisible.value = true
      }
      break
    case 'share':
      if (file) {
        shareFile.value = file
        shareVisible.value = true
      }
      break
    case 'download':
      if (file) downloadFile(file.userFileId)
      break
    case 'detail':
      if (file) {
        detailFile.value = file
        detailVisible.value = true
      }
      break
    case 'unzip':
      if (file) {
        unzipFile.value = file
        unzipVisible.value = true
      }
      break
    case 'edit':
      if (file) {
        const ext = file.extendName?.toLowerCase() || ''
        if (isOffice(ext)) {
          // Office 文件：新标签页打开 OnlyOffice 编辑
          openOfficePreview(file, true)
        } else if (isMarkdown(ext)) {
          // Markdown 文件：打开 Markdown 编辑器（可编辑模式）
          markdownPreview.openEdit(file)
        } else {
          // 代码文件：打开代码编辑器（可编辑模式）
          codePreview.openEdit(file)
        }
      }
      break
    case 'refresh':
      refreshFileList()
      break
    case 'createFolder':
      addFolderVisible.value = true
      break
    case 'uploadFile':
      triggerFileUpload()
      break
    case 'uploadFolder':
      triggerFolderUpload()
      break
    case 'dragUpload':
      uploadFileStore.showUploadMask = true
      break
    case 'copyLink':
      if (file) handleCopyShareLink(file)
      break
    case 'cancelShare':
      if (file) handleCancelShare(file)
      break
  }
}

// ---- 上传 ----

const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerFileUpload(): void {
  const input = document.createElement('input')
  input.type = 'file'
  input.multiple = true
  input.onchange = () => {
    if (input.files) {
      uploadManager.uploadFiles(Array.from(input.files), currentFilePath.value, refreshFileList)
    }
  }
  input.click()
}

function triggerFolderUpload(): void {
  const input = document.createElement('input')
  input.type = 'file'
  input.webkitdirectory = true
  input.onchange = () => {
    if (input.files) {
      uploadManager.uploadFiles(Array.from(input.files), currentFilePath.value, refreshFileList)
    }
  }
  input.click()
}

// ---- 批量操作 ----

function handleBatchDelete(): void {
  deleteFiles.value = [...fileListStore.selectedFiles]
  deleteMode.value = currentFileType.value === FileType.RECYCLE ? 2 : 1
  deleteVisible.value = true
}

function handleBatchRestore(): void {
  restoreIds.value = fileListStore.selectedFiles.map((f) => f.userFileId)
  restoreVisible.value = true
}

function handleBatchMove(): void {
  moveFiles.value = [...fileListStore.selectedFiles]
  moveVisible.value = true
}

function handleBatchDownload(): void {
  const ids = fileListStore.selectedFiles.map((f) => f.userFileId)
  batchDownload(ids)
}

function handleBatchShare(): void {
  // 批量分享：逐个为选中文件创建分享链接
  const files = fileListStore.selectedFiles
  if (files.length === 0) return
  // 只分享第一个文件（ShareDialog 一次只能处理一个文件）
  shareFile.value = files[0]
  shareVisible.value = true
}

/** 复制分享链接到剪贴板 */
async function handleCopyShareLink(file: FileInfo): Promise<void> {
  try {
    // 从 shareList 中找到对应的分享记录
    const share = fileListStore.shareList.find((s) => s.userFileId === file.userFileId)
    if (!share) {
      ElMessage.warning('未找到该文件的分享记录')
      return
    }
    const shareUrl = `${window.location.origin}/share/${share.shareCode}`
    const text = share.extractCode
      ? `${shareUrl}\n提取码: ${share.extractCode}`
      : shareUrl
    await navigator.clipboard.writeText(text)
    ElMessage.success('分享链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制链接失败')
  }
}

/** 取消分享 */
async function handleCancelShare(file: FileInfo): Promise<void> {
  try {
    // 从 shareList 中找到对应的分享记录
    const share = fileListStore.shareList.find((s) => s.userFileId === file.userFileId)
    if (!share) {
      ElMessage.warning('未找到该文件的分享记录')
      return
    }
    await cancelShare(share.shareId)
    ElMessage.success('取消分享成功')
    refreshFileList()
  } catch {
    ElMessage.error('取消分享失败')
  }
}

// ---- 弹窗成功回调 ----

function handleDialogSuccess(): void {
  refreshFileList()
  fileListStore.clearSelection()
}

// ---- 分页 ----

function handlePageChange(page: number): void {
  if (search.isSearch.value) {
    search.searchPageChange(page)
  } else {
    fileListStore.fetchFileList({
      fileType: currentFileType.value,
      filePath: currentFilePath.value,
      page,
      size: fileListStore.pageSize,
    })
  }
}

function handleSizeChange(size: number): void {
  fileListStore.pageSize = size
  if (search.isSearch.value) {
    search.searchPageChange(0)
  } else {
    fileListStore.fetchFileList({
      fileType: currentFileType.value,
      filePath: currentFilePath.value,
      page: 0,
      size,
    })
  }
}

// ---- 排序 ----

function handleSortChange(prop: string, order: string): void {
  if (search.isSearch.value) {
    search.searchSortChange(prop, order)
  } else {
    fileListStore.fetchFileList({
      fileType: currentFileType.value,
      filePath: currentFilePath.value,
      page: 0,
      size: fileListStore.pageSize,
      order: prop,
      sort: order,
    })
  }
}

// ---- 搜索 ----

function handleSearch(keyword: string): void {
  search.search(keyword)
}

// ---- 新建文件 ----

function handleCreateFile(fileType: string): void {
  // 创建空文件（docx/xlsx/pptx）
  import('@/api/file').then(({ createFile }) => {
    createFile({
      fileName: `新建文件.${fileType}`,
      filePath: currentFilePath.value,
    }).then(() => {
      refreshFileList()
    })
  })
}

// ---- 路由监听 ----

watch(
  () => [route.query.fileType, route.query.filePath],
  () => {
    search.clear()
    refreshFileList()
    fileListStore.clearSelection()
  },
)

// ---- 窗口大小监听 ----

function handleResize(): void {
  commonStore.updateScreenWidth()
}

onMounted(() => {
  // 确保所有预览组件初始状态为关闭
  imagePreview.close()
  videoPreview.close()
  audioPreview.close()
  codePreview.close()
  markdownPreview.close()
  
  refreshFileList()
  window.addEventListener('resize', handleResize)
  window.addEventListener('paste', handlePaste as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('paste', handlePaste as EventListener)
})

// ---- Ctrl+V 截图粘贴上传 ----

function handlePaste(e: ClipboardEvent): void {
  const items = e.clipboardData?.items
  if (!items) return
  const imageFiles: File[] = []
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) imageFiles.push(file)
    }
  }
  if (imageFiles.length > 0) {
    e.preventDefault()
    uploadManager.uploadFiles(imageFiles, currentFilePath.value, refreshFileList)
  }
}
</script>

<template>
  <div
    class="file-view"
    @dragenter.prevent="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <el-container class="file-container">
      <el-main class="file-main">
        <!-- 工具栏 -->
        <OperationMenu
          :file-type="currentFileType"
          @upload-file="triggerFileUpload"
          @upload-folder="triggerFolderUpload"
          @create-folder="addFolderVisible = true"
          @create-file="handleCreateFile"
          @batch-delete="handleBatchDelete"
          @batch-restore="handleBatchRestore"
          @batch-move="handleBatchMove"
          @batch-download="handleBatchDownload"
          @batch-share="handleBatchShare"
          @refresh="refreshFileList"
          @search="handleSearch"
        />

        <!-- 面包屑 -->
        <BreadCrumb
          :file-type="currentFileType"
          :file-path="currentFilePath"
        />

        <!-- 文件列表 -->
        <div class="file-list-area" @contextmenu.prevent="handleBlankContextMenu">
          <FileTable
            v-if="fileListStore.fileModel === FileViewMode.LIST"
            :file-type="currentFileType"
            :highlight-map="search.highlightMap.value"
            @open-file="handleOpenFile"
            @context-menu="handleContextMenu"
            @sort-change="handleSortChange"
          />
          <FileGrid
            v-else-if="fileListStore.fileModel === FileViewMode.GRID"
            :file-type="currentFileType"
            :highlight-map="search.highlightMap.value"
            @open-file="handleOpenFile"
            @context-menu="handleContextMenu"
          />
          <FileTimeLine
            v-else-if="fileListStore.fileModel === FileViewMode.TIMELINE"
            :file-type="currentFileType"
            @open-file="handleOpenFile"
          />
        </div>

        <!-- 分页 -->
        <Pagination
          v-if="fileListStore.total > 0"
          :current-page="fileListStore.currentPage"
          :total="fileListStore.total"
          @page-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </el-main>
    </el-container>

    <!-- 拖拽上传遮罩 -->
    <div v-if="uploadFileStore.showUploadMask" class="upload-mask" @click.self="uploadFileStore.showUploadMask = false">
      <div class="upload-mask-content">
        <el-button class="upload-mask-close" text size="large" @click="uploadFileStore.showUploadMask = false">
          <el-icon :size="20"><Close /></el-icon>
        </el-button>
        <el-icon :size="48"><UploadFilled /></el-icon>
        <p>截图粘贴或将文件拖拽至此区域上传</p>
      </div>
    </div>

    <!-- 右键菜单 -->
    <ContextMenu
      v-model:visible="contextMenuVisible"
      :x="contextMenuX"
      :y="contextMenuY"
      :selected-file="contextMenuFile"
      :file-type="currentFileType"
      @action="handleContextAction"
    />

    <!-- 预览组件（全屏覆盖层） -->
    <ImagePreview />
    <VideoPreview />
    <AudioPreview />
    <CodePreview />
    <MarkdownPreview />

    <!-- 上传面板 -->
    <UploadPanel />

    <!-- 弹窗 -->
    <AddFolderDialog
      v-model:visible="addFolderVisible"
      :file-path="currentFilePath"
      @success="handleDialogSuccess"
    />
    <RenameDialog
      v-model:visible="renameVisible"
      :file="renameFile"
      @success="handleDialogSuccess"
    />
    <DeleteDialog
      v-model:visible="deleteVisible"
      :files="deleteFiles"
      :mode="deleteMode"
      @success="handleDialogSuccess"
    />
    <CopyFileDialog
      v-model:visible="copyVisible"
      :file="copyFile"
      @success="handleDialogSuccess"
    />
    <MoveFileDialog
      v-model:visible="moveVisible"
      :files="moveFiles"
      @success="handleDialogSuccess"
    />
    <ShareDialog
      v-model:visible="shareVisible"
      :file="shareFile"
    />
    <FileDetailDialog
      v-model:visible="detailVisible"
      :file="detailFile"
    />
    <RestoreDialog
      v-model:visible="restoreVisible"
      :user-file-ids="restoreIds"
      @success="handleDialogSuccess"
    />
    <UnzipDialog
      v-model:visible="unzipVisible"
      :file="unzipFile"
    />
  </div>
</template>

<style lang="scss" scoped>
.file-view {
  position: relative;
  height: calc(100vh - $header-height);
}

.file-container {
  height: 100%;
}

.file-main {
  padding: 0 16px;
  overflow: auto;
}

.file-list-area {
  min-height: 200px;
}

.upload-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.85);
  border: 5px dashed #8091a5;
  z-index: 19;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-mask-close {
  position: absolute;
  top: 16px;
  right: 16px;
  color: #909399;
  font-size: 20px;

  &:hover {
    color: #409eff;
  }
}

.upload-mask-content {
  text-align: center;
  color: $info;

  p {
    margin-top: 16px;
    font-size: 30px;
  }
}
</style>
