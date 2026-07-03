<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  width?: number
}>(), {
  width: 375,
})

const verified = defineModel<boolean>('verified', { default: false })

const sliderRef = ref<HTMLElement | null>(null)
const handlerRef = ref<HTMLElement | null>(null)

const handlerX = ref(0)
const isDragging = ref(false)
const startX = ref(0)

const maxDrag = computed(() => props.width - 40)

const sliderBg = computed(() => {
  const pct = Math.min((handlerX.value / maxDrag.value) * 100, 100)
  return `linear-gradient(to right, #67C23A 0%, #67C23A ${pct}%, #F5F7FA ${pct}%, #F5F7FA 100%)`
})

const successBg = 'linear-gradient(to right, #67C23A, #67C23A)'

function onMouseDown(e: MouseEvent): void {
  if (verified.value) return
  isDragging.value = true
  startX.value = e.clientX - handlerX.value
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function onTouchStart(e: TouchEvent): void {
  if (verified.value) return
  isDragging.value = true
  const touch = e.touches[0]
  if (touch) {
    startX.value = touch.clientX - handlerX.value
  }
  document.addEventListener('touchmove', onTouchMove)
  document.addEventListener('touchend', onTouchEnd)
}

function onMouseMove(e: MouseEvent): void {
  if (!isDragging.value) return
  let newX = e.clientX - startX.value
  newX = Math.max(0, Math.min(newX, maxDrag.value))
  handlerX.value = newX
}

function onTouchMove(e: TouchEvent): void {
  if (!isDragging.value) return
  const touch = e.touches[0]
  if (!touch) return
  let newX = touch.clientX - startX.value
  newX = Math.max(0, Math.min(newX, maxDrag.value))
  handlerX.value = newX
}

function onMouseUp(): void {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  checkSuccess()
}

function onTouchEnd(): void {
  isDragging.value = false
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
  checkSuccess()
}

function checkSuccess(): void {
  if (handlerX.value >= maxDrag.value - 2) {
    handlerX.value = maxDrag.value
    verified.value = true
  } else {
    handlerX.value = 0
  }
}

/** 重置滑块 */
function reset(): void {
  handlerX.value = 0
  verified.value = false
}

defineExpose({ reset })
</script>

<template>
  <div
    ref="sliderRef"
    class="drag-verify"
    :style="{ width: props.width + 'px', background: verified ? successBg : sliderBg }"
  >
    <div class="drag-verify-text">
      <span v-if="!verified">按住左边按钮拖动到右边</span>
      <span v-else class="success-text">验证成功</span>
    </div>
    <div
      ref="handlerRef"
      class="drag-verify-handler"
      :style="{ left: handlerX + 'px' }"
      @mousedown="onMouseDown"
      @touchstart="onTouchStart"
    >
      <span v-if="!verified">&raquo;</span>
      <span v-else>&#10003;</span>
    </div>
  </div>
</template>

<style scoped>
.drag-verify {
  height: 40px;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  user-select: none;
}

.drag-verify-text {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #909399;
  pointer-events: none;
}

.success-text {
  color: #fff;
}

.drag-verify-handler {
  position: absolute;
  top: 0;
  width: 40px;
  height: 40px;
  background: #F5F7FA;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  font-size: 18px;
  color: #909399;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: background 0.3s;
}

.drag-verify-handler:active {
  cursor: grabbing;
}
</style>
