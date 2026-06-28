<script setup lang="ts">
import SearchBar from '_c/common/SearchBar.vue'
</script>

<template>
  <header class="app-header">
    <span class="logo">奇文网盘</span>
    <div class="header-center">
      <SearchBar />
    </div>
    <div class="header-right" />
  </header>
</template>

<style lang="stylus" scoped>
.app-header
  height: 60px
  background: #409eff
  color: #fff
  display: flex
  align-items: center
  padding: 0 20px

  .logo
    font-size: 20px
    font-weight: bold
    flex-shrink: 0

  .header-center
    flex: 1
    display: flex
    justify-content: center

  .header-right
    flex-shrink: 0
</style>
