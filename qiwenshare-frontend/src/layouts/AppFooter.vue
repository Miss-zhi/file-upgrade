<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSystemParams } from '@/api/home'
import type { SystemParam } from '@/api/home'

const route = useRoute()

/** 是否隐藏 Footer（由路由 meta 控制） */
const hideFooter = computed(() => route.meta.hideFooter === true)

/** 版权信息 */
const copyright = ref('')

/** 加载系统参数 */
onMounted(async () => {
  try {
    const params = await getSystemParams()
    const copyrightGroup = params.find(
      (g: SystemParam) => g.groupName === 'copyright',
    )
    if (copyrightGroup?.params?.['copyright']) {
      copyright.value = copyrightGroup.params['copyright']
    }
  } catch {
    // 静默失败，使用默认版权信息
    copyright.value = '© 2024 奇文网盘'
  }
})
</script>

<template>
  <footer v-if="!hideFooter" class="app-footer">
    <div class="footer-content">
      <img src="@/assets/logo.svg" alt="Logo" class="footer-logo" />
      <p class="footer-copyright">{{ copyright }}</p>
    </div>
  </footer>
</template>

<style lang="scss" scoped>
.app-footer {
  background: linear-gradient(to right, $primary, #66b1ff);
  padding: 16px 0 16px 5vw;
}

.footer-content {
  display: flex;
  flex-direction: column;
}

.footer-logo {
  width: 240px;
  margin-bottom: 8px;
}

.footer-copyright {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

@media screen and (max-width: 920px) {
  .footer-logo {
    width: 160px;
  }
}
</style>
