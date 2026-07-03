<script setup lang="ts">
import { inject, computed } from 'vue'
import { ArrowLeft, ArrowRight, RefreshRight, Download, Close, InfoFilled } from '@element-plus/icons-vue'
import { IMAGE_PREVIEW_KEY } from '@/composables/previewKeys'
import type { PreviewFileItem } from '@/types/file'

const {
  visible,
  currentIndex,
  currentFile,
  fileList,
  total,
  zoom,
  rotation,
  sidebarCollapsed,
  previewUrl,
  thumbnailUrls,
  loading,
  goTo,
  prev,
  next,
  zoomByWheel,
  setZoom,
  rotate,
  toggleSidebar,
  open,
  close,
} = inject(IMAGE_PREVIEW_KEY)!

/** 暴露 open 方法供外部调用 */
defineExpose({ open })

/** 序号跳转 */
function onIndexInput(e: Event): void {
  const target = e.target as HTMLInputElement
  const num = parseInt(target.value)
  if (!isNaN(num) && num >= 1 && num <= total.value) {
    goTo(num - 1)
  }
}

/** 下载当前图片 */
function onDownload(): void {
  if (currentFile.value) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    window.open(`${baseUrl}/filetransfer/download/${currentFile.value.userFileId}`, '_blank')
  }
}

/** 图片样式 */
const imgStyle = computed(() => ({
  zoom: `${zoom.value}%`,
  transform: `rotate(${rotation.value}deg)`,
}))

/** 主区域样式 */
const mainAreaStyle = computed(() => ({
  left: sidebarCollapsed.value || total.value <= 1 ? '0' : '120px',
}))
</script>

<template>
  <Teleport to="body">
    <div v-show="visible" class="image-preview-overlay" @click.self="close">
      <!-- 顶部栏 -->
      <div class="tip-wrapper">
        <div class="tip-left">
          <el-button
            v-if="total > 1"
            :icon="sidebarCollapsed ? ArrowRight : ArrowLeft"
            text
            class="tip-btn"
            @click="toggleSidebar"
          />
          <span class="file-name">{{ currentFile?.fileName || '' }}</span>
        </div>
        <div class="tip-center" v-if="total > 1">
          <input
            class="index-input"
            :value="currentIndex + 1"
            @change="onIndexInput"
          />
          <span class="total-text">/ {{ total }}</span>
        </div>
        <div class="tip-right">
          <el-tooltip effect="dark" placement="bottom">
            <template #content>
              <div style="line-height: 1.8">
                1. 点击图片以外的区域可退出预览<br />
                2. 按 Escape 键可退出预览<br />
                3. 按左、右方向键可切换为上一张、下一张图片<br />
                4. 鼠标滚轮可放大、缩小图片<br />
                5. 点击左上角箭头图标可折叠、展开缩略图
              </div>
            </template>
            <el-button text class="tip-btn tip-help-btn">
              <span class="help-text">操作提示</span>
              <el-icon><InfoFilled /></el-icon>
            </el-button>
          </el-tooltip>
          <el-button :icon="RefreshRight" text class="tip-btn" @click="rotate" />
          <el-link :underline="false" class="tip-link" @click="onDownload">
            <el-icon :size="20"><Download /></el-icon>
          </el-link>
          <el-link :underline="false" class="tip-link close-btn" @click="close">
            <el-icon :size="18"><Close /></el-icon>
          </el-link>
        </div>
      </div>

      <!-- 左侧缩略图侧栏 -->
      <div v-if="total > 1 && !sidebarCollapsed" class="min-img-list">
        <div
          v-for="(item, idx) in fileList"
          :key="item.userFileId"
          class="min-img-item"
          :class="{ active: idx === currentIndex }"
          @click="goTo(idx)"
        >
          <img v-if="thumbnailUrls.get(item.userFileId)" :src="thumbnailUrls.get(item.userFileId)" :alt="item.fileName" />
        </div>
      </div>

      <!-- 主图区域 -->
      <div class="img-wrapper" :style="mainAreaStyle">
        <div v-if="loading" class="loading-mask">加载中...</div>
        <img
          v-else-if="previewUrl"
          :src="previewUrl"
          :alt="currentFile?.fileName"
          :style="imgStyle"
          class="main-image"
          @wheel.prevent="zoomByWheel(($event as WheelEvent).deltaY)"
        />

        <!-- 左右箭头 -->
        <div v-if="total > 1" class="arrow left-arrow" @click="prev">
          <el-icon :size="60"><ArrowLeft /></el-icon>
        </div>
        <div v-if="total > 1" class="arrow right-arrow" @click="next">
          <el-icon :size="60"><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 底部缩放栏 -->
      <div class="zoom-bar">
        <el-slider
          :model-value="zoom"
          :min="1"
          :max="200"
          :step="1"
          @update:model-value="setZoom"
        />
        <span class="zoom-text">{{ zoom }}%</span>
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.image-preview-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.8);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.tip-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2;
  color: #fff;
}

.tip-left, .tip-center, .tip-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-name {
  font-size: 16px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tip-btn {
  color: #fff !important;
}

.tip-help-btn {
  .help-text {
    margin-right: 4px;
    font-size: 13px;
  }
}

.tip-link {
  color: #fff !important;
}

.close-btn {
  font-size: 18px;
}

.index-input {
  width: 50px;
  height: 28px;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
}

.total-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.min-img-list {
  position: absolute;
  top: 48px;
  left: 0;
  bottom: 0;
  width: 120px;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.3);
  padding: 4px;
  z-index: 5;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 2px; }
}

.min-img-item {
  width: 80px;
  height: 80px;
  margin-bottom: 4px;
  border: 2px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 80px;
    height: 80px;
    background: #000;
    opacity: 0.4;
    pointer-events: none;
  }

  &:hover::after {
    opacity: 0.2;
  }

  &.active {
    border-color: #409EFF;
    &::after { display: none; }
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.img-wrapper {
  position: absolute;
  top: 48px;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.main-image {
  max-width: 90%;
  max-height: 80%;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.loading-mask {
  color: #fff;
  font-size: 18px;
}

.arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  z-index: 3;
  transition: color 0.2s;

  &:hover { color: #fff; }
}

.left-arrow { left: 64px; }
.right-arrow { right: 64px; }

.zoom-bar {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 600px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10;

  .el-slider {
    flex: 1;
  }

  :deep(.el-slider__bar) {
    background-color: #303133;
  }

  :deep(.el-slider__button) {
    border-color: #303133;
    background: #303133;
  }
}

.zoom-text {
  color: #fff;
  font-size: 13px;
  width: 50px;
  text-align: right;
}
</style>
