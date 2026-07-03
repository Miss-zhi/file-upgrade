<script setup lang="ts">
import { computed, watch, inject } from 'vue'
import { Download, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { VIDEO_PREVIEW_KEY } from '@/composables/previewKeys'
import type { PreviewFileItem } from '@/types/file'
import { formatFileSize } from '@/utils/file'

const {
  visible,
  currentIndex,
  currentFile,
  fileList,
  total,
  previewUrl,
  playbackRate,
  playbackRates,
  playlistCollapsed,
  unsupported,
  loading,
  videoRef,
  goTo,
  prev,
  next,
  togglePlaylist,
  setPlaybackRate,
  open,
  close,
} = inject(VIDEO_PREVIEW_KEY)!

defineExpose({ open })

function onDownload(): void {
  if (currentFile.value) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    window.open(`${baseUrl}/filetransfer/download/${currentFile.value.userFileId}`, '_blank')
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-show="visible" class="video-preview-overlay">
      <!-- 顶部栏 -->
      <div class="top">
        <div class="top-left">
          <span class="file-name">{{ currentFile?.fileName || '' }}</span>
          <span v-if="currentFile" class="file-size">{{ formatFileSize(currentFile.fileSize) }}</span>
        </div>
        <div class="top-right">
          <el-link :underline="false" class="action-link" @click="onDownload">
            <el-icon :size="20"><Download /></el-icon>
          </el-link>
          <el-link
            v-if="total > 1"
            :underline="false"
            class="action-link"
            @click="togglePlaylist"
          >
            {{ playlistCollapsed ? '展开列表' : '折叠列表' }}
          </el-link>
          <el-link :underline="false" class="action-link close-btn" @click="close()">
            ✕
          </el-link>
        </div>
      </div>

      <!-- 下部播放区域 -->
      <div class="bottom">
        <!-- 视频播放器 -->
        <div class="video-player-area">
          <div v-if="loading" class="loading-text">加载中...</div>
          <div v-else-if="unsupported" class="unsupported-text">
            该视频格式不支持在线播放
            <el-button type="primary" size="small" @click="onDownload">下载视频</el-button>
          </div>
          <video
            v-else-if="previewUrl"
            ref="videoRef"
            :src="previewUrl"
            controls
            class="video-element"
            :playbackRate="playbackRate"
          >
            您的浏览器不支持 video 标签
          </video>

          <!-- 播放速率选择 -->
          <div v-if="previewUrl && !unsupported" class="rate-selector">
            <el-radio-group
              :model-value="playbackRate"
              size="small"
              @change="setPlaybackRate"
            >
              <el-radio-button
                v-for="rate in playbackRates"
                :key="rate"
                :value="rate"
              >
                {{ rate }}x
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 播放列表 -->
        <div v-if="total > 1 && !playlistCollapsed" class="video-list-wrapper">
          <div class="list-title">播放列表</div>
          <div
            v-for="(item, idx) in fileList"
            :key="item.userFileId"
            class="video-list-item"
            :class="{ active: idx === currentIndex }"
            @click="goTo(idx)"
          >
            <span class="item-name">{{ item.fileName }}</span>
            <span class="item-size">{{ formatFileSize(item.fileSize) }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.video-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.75);
}

.top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 0 16px;
  background: #000;
  margin-bottom: 8px;
  z-index: 2;
  color: #fff;
}

.top-left, .top-right {
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

.file-size {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.action-link {
  color: #fff !important;
}

.close-btn { font-size: 22px; }

.bottom {
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
}

.video-player-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.video-element {
  max-width: 100%;
  max-height: calc(100% - 50px);
}

.loading-text, .unsupported-text {
  color: #fff;
  font-size: 16px;
  text-align: center;
}

.unsupported-text {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.rate-selector {
  margin-top: 12px;
}

.video-list-wrapper {
  width: 280px;
  background: #000;
  overflow-y: auto;
  padding: 0;

  &::-webkit-scrollbar { width: 8px; }
  &::-webkit-scrollbar-track { background: #EBEEF5; }
  &::-webkit-scrollbar-thumb { background: #909399; border-radius: 2em; }
}

.list-title {
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 2px solid #606266;
}

.video-list-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  transition: color 0.2s;

  &:hover { color: #409EFF; }
  &.active { color: #409EFF; background: #000; }
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.item-size {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-left: 8px;
}

@media (max-width: 768px) {
  .video-list-wrapper { display: none; }
}
</style>
