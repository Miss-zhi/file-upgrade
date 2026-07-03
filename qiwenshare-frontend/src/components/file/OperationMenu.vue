<script setup lang="ts">
import { ref, computed } from 'vue'
import { useFileListStore } from '@/stores/fileList'
import { useCommonStore } from '@/stores/common'
import { useUploadFileStore } from '@/stores/uploadFile'
import { FileViewMode, FileType } from '@/types/file'

const fileListStore = useFileListStore()
const commonStore = useCommonStore()
const uploadFileStore = useUploadFileStore()

const props = defineProps<{
  fileType: number
}>()

const emit = defineEmits<{
  (e: 'uploadFile'): void
  (e: 'uploadFolder'): void
  (e: 'dragUpload'): void
  (e: 'createFolder'): void
  (e: 'createFile', fileType: string): void
  (e: 'batchDelete'): void
  (e: 'batchRestore'): void
  (e: 'batchMove'): void
  (e: 'batchDownload'): void
  (e: 'batchShare'): void
  (e: 'refresh'): void
  (e: 'search', keyword: string): void
}>()

const searchKeyword = ref('')
const showSettings = ref(false)

/** 是否全部文件页面 */
const isAllFiles = computed(() => props.fileType === FileType.ALL)
/** 是否回收站 */
const isRecycle = computed(() => props.fileType === FileType.RECYCLE)
/** 是否批量操作 */
const isBatch = computed(() => fileListStore.isBatchOperation)
/** 是否移动端 */
const isMobile = computed(() => commonStore.screenWidth <= 768)

function handleUploadFile(): void {
  emit('uploadFile')
}

function handleUploadFolder(): void {
  emit('uploadFolder')
}

function handleDragUpload(): void {
  uploadFileStore.showUploadMask = true
  emit('dragUpload')
}

function handleCreateFolder(): void {
  emit('createFolder')
}

function handleCreateFile(type: string): void {
  emit('createFile', type)
}

function handleRefresh(): void {
  emit('refresh')
}

function handleSearch(): void {
  emit('search', searchKeyword.value)
}

function setViewMode(mode: FileViewMode): void {
  fileListStore.setFileModel(mode)
}
</script>

<template>
  <div class="operation-menu" :class="{ 'is-recycle': isRecycle }">
    <!-- 左侧：上传 + 新建按钮组 -->
    <div v-if="isAllFiles && !isBatch" class="operation-left">
      <!-- 上传按钮组 -->
      <el-dropdown trigger="click" @command="(cmd: string) => {
        if (cmd === 'file') handleUploadFile()
        else if (cmd === 'folder') handleUploadFolder()
        else if (cmd === 'drag') handleDragUpload()
      }">
        <el-button type="primary" size="small">
          上传<el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="file">上传文件</el-dropdown-item>
            <el-dropdown-item command="folder">上传文件夹</el-dropdown-item>
            <el-dropdown-item command="drag">拖拽上传</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 新建按钮组 -->
      <el-dropdown trigger="click" @command="(cmd: string) => {
        if (cmd === 'folder') handleCreateFolder()
        else handleCreateFile(cmd)
      }">
        <el-button size="small">
          新建<el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="folder">新建文件夹</el-dropdown-item>
            <el-dropdown-item divided command="docx">新建 Word</el-dropdown-item>
            <el-dropdown-item command="xlsx">新建 Excel</el-dropdown-item>
            <el-dropdown-item command="pptx">新建 PPT</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 中部：批量操作按钮 -->
    <div v-if="isBatch" class="operation-left">
      <el-button size="small" type="danger" @click="emit('batchDelete')">
        {{ isRecycle ? '彻底删除' : '批量删除' }}
      </el-button>
      <el-button v-if="isRecycle" size="small" type="success" @click="emit('batchRestore')">批量还原</el-button>
      <template v-else>
        <el-button size="small" @click="emit('batchMove')">批量移动</el-button>
        <el-button size="small" @click="emit('batchDownload')">批量下载</el-button>
        <el-button size="small" @click="emit('batchShare')">批量分享</el-button>
      </template>
    </div>

    <!-- 右侧：搜索 + 视图切换 + 设置 -->
    <div class="operation-right">
      <!-- 搜索框 -->
      <el-input
        v-if="isAllFiles && !isBatch"
        v-model="searchKeyword"
        placeholder="搜索文件"
        size="small"
        clearable
        style="width: 250px"
        class="search-input"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <!-- 刷新 -->
      <el-icon class="op-icon" @click="handleRefresh"><Refresh /></el-icon>

      <template v-if="!isMobile">
        <span class="op-divider" />

        <!-- 视图模式切换 -->
        <el-icon
          class="op-icon"
          :class="{ active: fileListStore.fileModel === FileViewMode.LIST }"
          @click="setViewMode(FileViewMode.LIST)"
        >
          <List />
        </el-icon>
        <el-icon
          class="op-icon"
          :class="{ active: fileListStore.fileModel === FileViewMode.GRID }"
          @click="setViewMode(FileViewMode.GRID)"
        >
          <Grid />
        </el-icon>
        <el-icon
          v-if="fileType === FileType.IMAGE"
          class="op-icon"
          :class="{ active: fileListStore.fileModel === FileViewMode.TIMELINE }"
          @click="setViewMode(FileViewMode.TIMELINE)"
        >
          <Clock />
        </el-icon>

        <span class="op-divider" />
      </template>

      <!-- 设置 -->
      <el-popover placement="bottom-end" :width="300" trigger="click">
        <template #reference>
          <el-icon class="op-icon"><Setting /></el-icon>
        </template>
        <div class="settings-popover">
          <h4>设置</h4>
          <!-- 列显隐 -->
          <div class="setting-item">
            <span>显示列</span>
            <el-checkbox-group v-model="fileListStore.selectedColumnList" size="small">
              <el-checkbox value="extendName">类型</el-checkbox>
              <el-checkbox value="fileSize">大小</el-checkbox>
              <el-checkbox value="uploadTime">修改日期</el-checkbox>
              <el-checkbox v-if="isRecycle" value="deleteTime">删除日期</el-checkbox>
            </el-checkbox-group>
          </div>
          <!-- 图标大小（网格模式） -->
          <div v-if="fileListStore.fileModel === FileViewMode.GRID" class="setting-item">
            <span>图标大小</span>
            <el-slider
              v-model="fileListStore.gridSize"
              :min="40"
              :max="150"
              :step="10"
              :format-tooltip="(val: number) => val + 'px'"
            />
          </div>
          <!-- 移动端视图切换 -->
          <div v-if="isMobile" class="setting-item">
            <span>视图模式</span>
            <el-radio-group v-model="fileListStore.fileModel" size="small">
              <el-radio-button :value="FileViewMode.LIST">列表</el-radio-button>
              <el-radio-button :value="FileViewMode.GRID">网格</el-radio-button>
              <el-radio-button v-if="fileType === FileType.IMAGE" :value="FileViewMode.TIMELINE">时间线</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.operation-menu {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;

  &.is-recycle {
    margin: 8px 0;
    justify-content: flex-end;
  }
}

.operation-left {
  display: flex;
  gap: 8px;
}

.operation-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  margin-right: 8px;
}

.op-icon {
  font-size: 20px;
  color: $secondary-text;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    color: $primary;
    background: $primary-hover;
  }

  &.active {
    color: $primary;
  }
}

.op-divider {
  display: inline-block;
  width: 1px;
  height: 16px;
  background: $border-light;
  margin: 0 4px;
}

.settings-popover {
  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    color: $primary-text;
  }
}

.setting-item {
  margin-bottom: 12px;

  > span {
    display: block;
    font-size: 13px;
    color: $regular-text;
    margin-bottom: 6px;
  }
}
</style>
