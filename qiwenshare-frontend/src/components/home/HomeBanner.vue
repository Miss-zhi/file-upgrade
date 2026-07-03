<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goToFiles(): void {
  router.push('/file')
}
</script>

<template>
  <section class="home-banner">
    <el-carousel height="360px" :interval="5000">
      <el-carousel-item>
        <div class="banner-content">
          <div class="banner-text">
            <h1 class="banner-title">一款功能齐全的文件管理系统</h1>
            <p class="banner-desc">支持多种存储方式、在线预览、文件分享</p>
            <p class="banner-desc">分片断点续传、全局搜索、回收站</p>
            <p class="banner-desc">安全可靠，开源免费</p>
            <el-button type="primary" size="large" @click="goToFiles">
              开源免费，立即体验
            </el-button>
          </div>
          <div class="banner-image">
            <img src="@/assets/logo.svg" alt="illustration" />
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </section>
</template>

<style lang="scss" scoped>
.home-banner {
  background: linear-gradient(to right, $primary, #79bbff);
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 85%;
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  color: #fff;
}

.banner-text {
  flex: 1;
}

.banner-title {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 20px;
}

.banner-desc {
  font-size: 16px;
  margin-bottom: 8px;
  opacity: 0.9;
}

.banner-image {
  max-width: 443px;

  img {
    width: 100%;
  }
}
</style>
