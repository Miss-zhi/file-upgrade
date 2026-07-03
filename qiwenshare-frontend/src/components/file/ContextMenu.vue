<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import {
  View, Delete, RefreshLeft, CopyDocument, Promotion,
  Edit, Share, Download, Files, Document, Link, CircleClose,
} from '@element-plus/icons-vue'
import { FileType } from '@/types/file'
import type { FileInfo } from '@/types/file'
import { isArchive, canEditOnline } from '@/utils/file'
import wordImg from '@/assets/images/file/file_word.svg'
import excelImg from '@/assets/images/file/file_excel.svg'
import pptImg from '@/assets/images/file/file_ppt.svg'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  selectedFile: FileInfo | null
  fileType: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'action', action: string, file?: FileInfo): void
}>()

const menuRef = ref<HTMLElement | null>(null)

function close(): void {
  emit('update:visible', false)
}

function handleAction(action: string): void {
  emit('action', action, props.selectedFile || undefined)
  close()
}

/** 智能定位（对齐旧项目：右侧不足138px时向左展开，底部不足时向上展开） */
function adjustPosition(): void {
  if (!menuRef.value) return
  const menu = menuRef.value
  const rect = menu.getBoundingClientRect()
  const viewportW = window.innerWidth
  const viewportH = window.innerHeight

  if (viewportW - props.x < 138) {
    menu.style.left = 'auto'
    menu.style.right = `${viewportW - props.x}px`
  }

  const itemCount = menuItems.value.filter((i) => !i.divider).length
  const menuHeight = itemCount * 36 + 10
  if (viewportH - props.y < menuHeight) {
    menu.style.top = 'auto'
    menu.style.bottom = `${viewportH - props.y}px`
  }
}

/** 菜单项类型 */
interface MenuItem {
  label: string
  action: string
  /** 图标组件名（用于 v-if 分支渲染） */
  iconName?: string
  /** 图片路径（Office 文件类型用 SVG） */
  img?: string
  divider?: boolean
}

/** 文件右键菜单项 — 显示条件严格对齐旧项目 Box.vue computed */
const fileMenuItems = computed((): MenuItem[] => {
  if (!props.selectedFile) return []
  const items: MenuItem[] = []
  const ft = props.fileType
  const ext = props.selectedFile.extendName?.toLowerCase() || ''
  // 旧项目条件：![6,8].includes(fileType) && !['Share'].includes(routeName)
  // 新项目用 fileType 判断，SHARE=8 即分享页
  const isNormal = ft !== FileType.RECYCLE && ft !== FileType.SHARE

  // 查看（回收站不显示）— 旧: fileType !== 6
  if (ft !== FileType.RECYCLE) {
    items.push({ label: '查看', action: 'view', iconName: 'view' })
  }

  // 删除（回收站和分享页不显示）— 旧: fileType !== 8 && !Share
  if (isNormal) {
    items.push({ label: '删除', action: 'delete', iconName: 'delete' })
  }

  // 还原（仅回收站）— 旧: fileType === 6
  if (ft === FileType.RECYCLE) {
    items.push({ label: '还原', action: 'restore', iconName: 'restore' })
    items.push({ label: '彻底删除', action: 'permanentDelete', iconName: 'permanentDelete' })
  }

  // 复制到（回收站和分享页不显示）— 旧: ![6,8].includes(fileType) && !Share
  if (isNormal) {
    items.push({ label: '复制到', action: 'copy', iconName: 'copy' })
  }

  // 移动（回收站和分享页不显示）
  if (isNormal) {
    items.push({ label: '移动', action: 'move', iconName: 'move' })
  }

  // 重命名（回收站和分享页不显示）
  if (isNormal) {
    items.push({ label: '重命名', action: 'rename', iconName: 'rename' })
  }

  // 分享（回收站和分享页不显示）
  if (isNormal) {
    items.push({ label: '分享', action: 'share', iconName: 'share' })
  }

  // 下载（回收站不显示）— 旧: fileType !== 6
  if (ft !== FileType.RECYCLE) {
    items.push({ label: '下载', action: 'download', iconName: 'download' })
  }

  // 解压缩（归档文件，回收站和分享页不显示）
  if (isNormal && isArchive(ext)) {
    items.push({ label: '解压缩', action: 'unzip', iconName: 'unzip' })
  }

  // 在线编辑（回收站和分享页不显示，且文件可编辑）
  if (isNormal && canEditOnline(props.selectedFile.extendName, props.selectedFile.fileType)) {
    items.push({ label: '在线编辑', action: 'edit', iconName: 'edit' })
  }

  // 复制链接（仅分享页）— 旧: fileType === 8
  if (ft === FileType.SHARE) {
    items.push({ label: '复制链接', action: 'copyLink', iconName: 'copyLink' })
    items.push({ label: '取消分享', action: 'cancelShare', iconName: 'cancelShare' })
  }

  // 文件详情（始终显示）
  items.push({ label: '文件详情', action: 'detail', iconName: 'detail' })

  return items
})

/** 空白区域右键菜单项 — 文案和图标对齐旧项目 */
const blankMenuItems = computed((): MenuItem[] => {
  if (props.selectedFile) return []
  if (props.fileType !== FileType.ALL) return []
  return [
    { label: '刷新', action: 'refresh' },
    { label: '', action: '', divider: true },
    { label: '新建文件夹', action: 'createFolder' },
    { label: '新建 Word 文档', action: 'createWord', img: wordImg },
    { label: '新建 Excel 工作表', action: 'createExcel', img: excelImg },
    { label: '新建 PPT 演示文稿', action: 'createPpt', img: pptImg },
    { label: '', action: '', divider: true },
    { label: '上传文件', action: 'uploadFile' },
    { label: '上传文件夹', action: 'uploadFolder' },
    { label: '拖拽上传', action: 'dragUpload' },
  ]
})

const menuItems = computed(() =>
  props.selectedFile ? fileMenuItems.value : blankMenuItems.value,
)

function handleDocumentClick(e: MouseEvent): void {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    close()
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  nextTick(adjustPosition)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="context-menu"
      :style="{ left: x + 'px', top: y + 'px' }"
    >
      <template v-for="(item, idx) in menuItems" :key="idx">
        <div v-if="item.divider" class="context-menu-divider" />
        <div
          v-else
          class="context-menu-item"
          @click="handleAction(item.action)"
        >
          <!-- 文件右键菜单图标：用 v-if 分支直接渲染，避免 component :is 动态解析失败 -->
          <template v-if="item.iconName === 'view'">
            <el-icon class="menu-icon"><View /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'delete'">
            <el-icon class="menu-icon"><Delete /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'restore'">
            <el-icon class="menu-icon"><RefreshLeft /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'permanentDelete'">
            <el-icon class="menu-icon" style="color: #F56C6C"><Delete /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'copy'">
            <el-icon class="menu-icon"><CopyDocument /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'move'">
            <el-icon class="menu-icon"><Promotion /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'rename'">
            <el-icon class="menu-icon"><Edit /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'share'">
            <el-icon class="menu-icon"><Share /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'download'">
            <el-icon class="menu-icon"><Download /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'unzip'">
            <el-icon class="menu-icon"><Files /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'edit'">
            <el-icon class="menu-icon"><Edit /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'copyLink'">
            <el-icon class="menu-icon"><Link /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'cancelShare'">
            <el-icon class="menu-icon" style="color: #F56C6C"><CircleClose /></el-icon>
          </template>
          <template v-else-if="item.iconName === 'detail'">
            <el-icon class="menu-icon"><Document /></el-icon>
          </template>
          <!-- 空白菜单 Office 图标 -->
          <img v-else-if="item.img" :src="item.img" class="menu-img" />
          <span>{{ item.label }}</span>
        </div>
      </template>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.context-menu {
  position: fixed;
  z-index: 2000;
  background: #fff;
  border: 1px solid #EBEEF5;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 8px 0;
  min-width: 138px;
  color: #606266;
  font-size: 14px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  height: 36px;
  padding: 0 16px;
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    background: #ecf5ff;
    color: #409EFF;
  }

  .menu-icon {
    margin-right: 8px;
    font-size: 16px;
    flex-shrink: 0;
  }

  .menu-img {
    margin-right: 4px;
    height: 20px;
    width: 20px;
    flex-shrink: 0;
  }
}

.context-menu-divider {
  height: 1px;
  margin: 2px 0;
  background: #EBEEF5;
}
</style>
