<script setup lang="ts">
import { ref } from 'vue'
import GlobalHeader from './components/GlobalHeader.vue'
import GlobalFooter from './components/GlobalFooter.vue'

const selectedKeys = ref<string[]>(['1'])
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="layout-header">
      <GlobalHeader />
    </a-layout-header>
    
    <a-layout-content class="layout-content">
      <router-view />
    </a-layout-content>
    
    <a-layout-footer class="layout-footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.layout-header {
  border-bottom: none !important;
  padding: 0 !important;
  height: auto !important;
  line-height: 1 !important;
  background: transparent !important;
}

.layout-footer {
  border-top: none !important;
  padding: 0 !important;
  background: transparent !important;
}

.layout-content {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 128px);
}
</style>