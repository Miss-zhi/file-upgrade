<script setup lang="ts">
import { ref, computed, watch, inject } from 'vue'
import { Download, InfoFilled } from '@element-plus/icons-vue'
import { AUDIO_PREVIEW_KEY } from '@/composables/previewKeys'
import type { PreviewFileItem } from '@/types/file'
import { formatFileSize } from '@/utils/file'
import waveGif from '@/assets/images/audio/wave.gif'

const {
  visible,
  currentIndex,
  currentFile,
  fileList,
  total,
  previewUrl,
  isPlaying,
  currentTime,
  duration,
  volume,
  loopMode,
  musicMeta,
  albumImageUrl,
  lyrics,
  currentLyricIndex,
  formattedCurrentTime,
  formattedDuration,
  audioRef,
  goTo,
  prev,
  next,
  togglePlay,
  toggleLoopMode,
  setVolume,
  seek,
  onTimeUpdate,
  onLoadedMetadata,
  onPlay,
  onPause,
  onEnded,
  open,
  close,
} = inject(AUDIO_PREVIEW_KEY)!

defineExpose({ open })

function onDownload(): void {
  if (currentFile.value) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    window.open(`${baseUrl}/filetransfer/download/${currentFile.value.userFileId}`, '_blank')
  }
}

const loopModeIcon = computed(() => {
  if (loopMode.value === 1) return 'icon-xunhuanbofang'
  if (loopMode.value === 2) return 'icon-danquxunhuan1'
  return 'icon-suijibofang1'
})

const loopModeLabel = computed(() => {
  if (loopMode.value === 1) return '列表循环'
  if (loopMode.value === 2) return '单曲循环'
  return '随机播放'
})

function onProgressInput(val: number): void {
  seek(val)
}

const lyricsContainerRef = ref<HTMLElement | null>(null)

// 歌词滚动到当前行
watch(currentLyricIndex, (idx) => {
  if (lyricsContainerRef.value && idx >= 0) {
    const el = lyricsContainerRef.value.querySelector(`.lyric-line-${idx}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
})
</script>

<template>
  <Teleport to="body">
    <div v-show="visible" class="audio-preview-overlay">
      <!-- 模糊专辑背景 -->
      <div
        v-if="albumImageUrl"
        class="blur-background"
        :style="{ backgroundImage: `url(${albumImageUrl})` }"
      />

      <!-- 右上角操作提示 + 关闭按钮 -->
      <div class="top-right-box">
        <el-tooltip effect="dark" placement="bottom">
          <template #content>
            <div style="line-height: 2">
              操作提示: <br />
              1. 按 Esc 键可退出查看；<br />
              2. 支持键盘控制：<br />
              &nbsp;&nbsp;空格 - 暂停/播放<br />
              &nbsp;&nbsp;左方向键 - 播放上一个<br />
              &nbsp;&nbsp;右方向键 - 播放下一个<br />
              &nbsp;&nbsp;上方向键 - 音量调大<br />
              &nbsp;&nbsp;下方向键 - 音量减小
            </div>
          </template>
          <el-link :underline="false" class="action-link tip-icon-link">
            <span class="tip-text">操作提示</span>
            <el-icon><InfoFilled /></el-icon>
          </el-link>
        </el-tooltip>
        <el-link :underline="false" class="action-link" @click="close()">
          ✕
        </el-link>
      </div>

      <!-- 主体区域 -->
      <div class="audio-list-wrapper">
        <!-- 左侧曲目列表 -->
        <div class="audio-list">
          <div class="audio-list-header">
            <span class="header-name">文件名</span>
            <span class="header-size">大小</span>
            <span class="header-path">路径</span>
          </div>
          <div
            v-for="(item, idx) in fileList"
            :key="item.userFileId"
            class="audio-item"
            :class="{ active: idx === currentIndex }"
            @click="goTo(idx)"
          >
            <div class="item-play-btn" @click.stop="togglePlay">
              <i v-if="idx === currentIndex && isPlaying" class="iconfont icon-icon-3" />
              <i v-else class="iconfont icon-icon-7" />
            </div>
            <span class="item-name">
              {{ item.fileName }}
              <img v-if="idx === currentIndex && isPlaying" :src="waveGif" class="wave-icon" alt="playing" />
            </span>
            <el-link :underline="false" class="item-dl" @click.stop="onDownload">
              <el-icon :size="14"><Download /></el-icon>
            </el-link>
            <span class="item-size">{{ formatFileSize(item.fileSize) }}</span>
          </div>
        </div>

        <!-- 右侧专辑封面 + 歌词 -->
        <div class="img-and-lyrics">
          <div class="album-cover">
            <img v-if="albumImageUrl" :src="albumImageUrl" alt="album" />
            <div v-else class="default-cover">♪</div>
          </div>
          <div class="music-info">
            <p class="track-name">{{ currentFile?.fileName?.replace(/\.[^.]+$/, '') || '' }}</p>
            <p v-if="musicMeta.artist" class="artist">{{ musicMeta.artist }}</p>
            <p v-if="musicMeta.album" class="album">{{ musicMeta.album }}</p>
          </div>
          <div ref="lyricsContainerRef" class="lyrics-container">
            <p
              v-for="(line, idx) in lyrics"
              :key="idx"
              :class="['lyric-line', `lyric-line-${idx}`, { active: idx === currentLyricIndex }]"
            >
              {{ line.text }}
            </p>
            <p v-if="lyrics.length === 0" class="no-lyrics">暂无歌词</p>
          </div>
        </div>
      </div>

      <!-- 底部控制栏 -->
      <div class="control-wrapper">
        <!-- 音频元素（隐藏） -->
        <audio
          ref="audioRef"
          :src="previewUrl"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onLoadedMetadata"
          @play="onPlay"
          @pause="onPause"
          @ended="onEnded"
        />

        <div class="control-left">
          <div class="control-buttons">
            <el-button text class="ctrl-btn" @click="prev">
              <i class="iconfont icon-shangyishou" />
            </el-button>
            <el-button text class="ctrl-btn play-btn" @click="togglePlay">
              <i :class="['iconfont', isPlaying ? 'icon-icon-3' : 'icon-icon-7']" />
            </el-button>
            <el-button text class="ctrl-btn" @click="next">
              <i class="iconfont icon-xiayishou" />
            </el-button>
          </div>

          <div class="progress-area">
            <span class="time">{{ formattedCurrentTime }}</span>
            <el-slider
              :model-value="currentTime"
              :min="0"
              :max="duration || 1"
              :step="0.1"
              class="progress-slider"
              @input="onProgressInput"
            />
            <span class="time">{{ formattedDuration }}</span>
          </div>
        </div>

        <div class="control-right">
          <el-button text class="ctrl-btn" @click="toggleLoopMode">
            <i :class="['iconfont', loopModeIcon]" />
            <span class="loop-label">{{ loopModeLabel }}</span>
          </el-button>

          <el-link :underline="false" class="ctrl-btn" @click="onDownload">
            <el-icon :size="18"><Download /></el-icon>
          </el-link>

          <div class="volume-control">
            <i :class="['iconfont', volume === 0 ? 'icon-jingyin01' : 'icon-yinliang101']" />
            <el-slider
              :model-value="volume"
              :min="0"
              :max="1"
              :step="0.01"
              :show-tooltip="false"
              style="width: 100px"
              @input="setVolume"
            />
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.audio-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  background: #303133;
  color: #DCDFE6;
}

.blur-background {
  position: fixed;
  top: -50%;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-size: cover;
  background-position: center;
  filter: blur(65px);
  opacity: 0.6;
  z-index: -1;
}

.top-right-box {
  position: fixed;
  top: 16px;
  right: 32px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-link {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 30px;
  &:hover { color: #E6A23C !important; }
}

.tip-icon-link {
  font-size: 14px !important;
  .tip-text {
    margin-right: 4px;
    font-size: 13px;
  }
}

.audio-list-wrapper {
  margin: 0 auto;
  width: 85%;
  height: calc(100vh - 120px);
  padding-top: 32px;
  display: flex;
}

.audio-list {
  flex: 1;
  overflow-y: auto;
  margin-right: 24px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 2px; }
}

.audio-list-header {
  display: flex;
  align-items: center;
  height: 56px;
  border-radius: 8px;
  padding: 0 16px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  .header-name { flex: 1; }
  .header-size { width: 80px; }
  .header-path { width: 120px; }
}

.audio-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 16px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.2s;

  &:hover { background: rgba(0, 0, 0, 0.1); color: #fff; }
  &.active { color: #E6A23C; background: rgba(0, 0, 0, 0.1); }
}

.item-play-btn {
  color: inherit;
  cursor: pointer;
  font-size: 16px;
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.wave-icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.item-dl { color: inherit !important; }
.item-size { font-size: 11px; color: rgba(255,255,255,0.3); width: 60px; }

.img-and-lyrics {
  width: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.album-cover {
  width: 160px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.default-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.1);
  font-size: 48px;
  color: rgba(255,255,255,0.3);
}

.music-info {
  text-align: center;
  color: #fff;
  margin-bottom: 16px;

  .track-name { font-size: 14px; font-weight: 500; }
  .artist { font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 4px; }
  .album { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
}

.lyrics-container {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  text-align: center;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(255,255,255,0.6) 15%, #fff 25%, #fff 75%, rgba(255,255,255,0.6) 85%, transparent);

  &::-webkit-scrollbar { width: 2px; }
}

.lyric-line {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 40px;
  transition: all 0.3s;

  &:hover { color: #fff; }
  &.active { color: #E6A23C; font-size: 14px; font-weight: 500; }
}

.no-lyrics { color: rgba(255,255,255,0.3); font-size: 12px; }

.control-wrapper {
  margin: 0 auto;
  width: 85%;
  height: 120px;
  padding: 24px 0 32px;
  display: flex;
  align-items: center;
}

.control-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.control-right {
  width: 340px;
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: flex-end;
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ctrl-btn {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 40px;
  &:hover { color: #E6A23C !important; }
}

.play-btn { font-size: 40px; }

.loop-label {
  font-size: 12px;
  margin-left: 4px;
}

.progress-area {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 600px;
  margin-top: 8px;
}

.time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  width: 40px;
}

.progress-slider {
  flex: 1;

  :deep(.el-slider__runway) { height: 2px; }
  :deep(.el-slider__bar) { background-color: #E6A23C; height: 2px; }
  :deep(.el-slider__button) { border: none; background: #E6A23C; width: 10px; height: 10px; }
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;

  :deep(.el-slider__runway) { height: 2px; }
  :deep(.el-slider__bar) { background-color: #E6A23C; height: 2px; }
  :deep(.el-slider__button) { border: none; background: #E6A23C; width: 10px; height: 10px; }
}
</style>
