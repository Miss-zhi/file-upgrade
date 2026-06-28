<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '_c/AppHeader.vue'
import AppFooter from '_c/AppFooter.vue'
</script>

<template>
  <div class="layout">
    <AppHeader />
    <div class="layout-main">
      <router-view />
    </div>
    <AppFooter />
  </div>
</template>

<style lang="stylus" scoped>
.layout
  display: flex
  flex-direction: column
  min-height: 100vh

  &-main
    flex: 1
    padding: 20px
</style>
