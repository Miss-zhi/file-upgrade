<script setup lang="ts">
import HomeBanner from '@/components/home/HomeBanner.vue'
import HomeFeatures from '@/components/home/HomeFeatures.vue'
import HomeNotice from '@/components/home/HomeNotice.vue'
</script>

<template>
  <div class="home-view">
    <HomeBanner />
    <HomeFeatures />
    <HomeNotice />
  </div>
</template>

<style lang="scss" scoped>
.home-view {
  background: #fff;
}
</style>
