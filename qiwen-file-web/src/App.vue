<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div id="qiwen-app">
    <router-view />
  </div>
</template>

<style lang="stylus">
#qiwen-app
  width: 100%
  min-height: 100vh
</style>
