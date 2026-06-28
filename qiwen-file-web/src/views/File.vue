<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useFileListStore } from '@/stores/fileList'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import FileList from '_c/file/FileList.vue'

const fileListStore = useFileListStore()
const { files, currentPath, loading } = storeToRefs(fileListStore)

// 面包屑
const breadcrumbPaths = computed(() => {
  const parts = currentPath.value.split('/').filter(Boolean)
  const crumbs: { name: string; path: string }[] = [{ name: '根目录', path: '/' }]
  let accumulated = '/'
  for (const part of parts) {
    accumulated += part + '/'
    crumbs.push({ name: part, path: accumulated })
  }
  return crumbs
})

// 面包屑点击
function handleBreadcrumbClick(crumb: { name: string; path: string }) {
  fileListStore.fetchFiles(crumb.path)
}

// 上传
async function handleUpload(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  await fileListStore.uploadFile(uploadFile.raw, currentPath.value)
}

// 新建文件夹
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
  } catch {
    // 用户取消
  }
}

// 删除
async function handleDelete(id: string, name: string) {
  try {
    await ElMessageBox.confirm(`确定删除 "${name}" 吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await fileListStore.deleteFile(id)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

// 刷新
function handleRefresh() {
  fileListStore.fetchFiles(currentPath.value)
}

onMounted(() => {
  fileListStore.fetchFiles('/')
})
</script>

<template>
  <div class="file-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item
            v-for="crumb in breadcrumbPaths"
            :key="crumb.path"
          >
            <el-link
              :underline="false"
              @click="handleBreadcrumbClick(crumb)"
            >
              {{ crumb.name }}
            </el-link>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="toolbar-right">
        <el-button @click="handleCreateFolder">
          新建文件夹
        </el-button>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleUpload"
        >
          <el-button type="primary">上传文件</el-button>
        </el-upload>
        <el-button
          :icon="'Refresh'"
          circle
          @click="handleRefresh"
        />
      </div>
    </div>

    <!-- 文件列表 -->
    <FileList
      :files="files"
      :loading="loading"
      @enter="(path: string) => fileListStore.fetchFiles(path)"
      @delete="handleDelete"
    />
  </div>
</template>

<style lang="stylus" scoped>
.file-page
  padding: 20px

  .toolbar
    display: flex
    justify-content: space-between
    align-items: center
    margin-bottom: 16px
    padding: 12px 16px
    background: #f5f7fa
    border-radius: 4px

    &-right
      display: flex
      align-items: center
      gap: 8px
</style>
