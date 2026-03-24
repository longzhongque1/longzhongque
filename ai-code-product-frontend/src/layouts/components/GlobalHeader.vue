<script setup lang="ts">
import { ref } from 'vue'

const menuItems = [
  { key: '1', label: '首页' },
  { key: '2', label: '项目' },
  { key: '3', label: '文档' },
  { key: '4', label: '关于' }
]

const selectedKeys = ref<string[]>(['1'])
</script>

<template>
  <div class="global-header">
    <div class="header-left">
      <img src="@/assets/kun.jpg" alt="Logo" class="logo" />
      <span class="site-title">AI 代码生成平台</span>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="header-menu"
      />
    </div>
    
    <div class="header-right">
      <a-button type="primary">登录</a-button>
    </div>
  </div>
</template>

<style scoped>
.global-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 6px;
}

.site-title {
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
}

.header-menu {
  border-bottom: none;
  background: transparent;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>