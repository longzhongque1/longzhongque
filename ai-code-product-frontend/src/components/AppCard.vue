<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  app: API.AppVO
  placeholderText?: string
}>()

const emit = defineEmits<{
  viewChat: [id: string]
  viewWork: [deployKey: string]
}>()

const router = useRouter()

const onViewChat = (id?: string | number) => {
  if (!id) return
  emit('viewChat', String(id))
}

const onViewWork = (deployKey?: string) => {
  const key = String(deployKey ?? '').trim()
  if (!key) return
  emit('viewWork', key)
}
</script>

<template>
  <a-card class="app-card" :bordered="false" hoverable>
    <div class="app-cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="cover-placeholder">{{ placeholderText || 'NoCode' }}</div>
    </div>
    <div class="app-info">
      <a-avatar v-if="app.user?.userAvatar" :src="app.user.userAvatar" :size="36" />
      <a-avatar v-else :size="36">{{ (app.user?.userName || '?')[0] }}</a-avatar>
      <div class="app-info-right">
        <div class="app-title">{{ app.appName || '未命名应用' }}</div>
        <div class="app-username">{{ app.user?.userName || '未知用户' }}</div>
      </div>
    </div>
    <div class="app-desc">{{ app.initPrompt || '暂无描述' }}</div>
    <div class="app-actions">
      <a-space :size="[8, 4]" wrap>
        <a-button type="link" @click="onViewChat(app.id)">查看对话</a-button>
        <a-button v-if="app.deployKey" type="link" @click="onViewWork(app.deployKey)">
          查看作品
        </a-button>
      </a-space>
    </div>
  </a-card>
</template>

<style scoped>
.app-card {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  height: 100%;
}

:deep(.app-card .ant-card-body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-cover {
  height: 148px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3f8cff, #8e59ff);
  overflow: hidden;
  margin-bottom: 10px;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: rgba(255, 255, 255, 0.9);
}

.app-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.app-info-right {
  min-width: 0;
  flex: 1;
}

.app-title {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #fff;
}

.app-username {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  margin-bottom: 8px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.app-actions {
  margin-top: auto;
  overflow: hidden;
}

.app-actions :deep(.ant-space) {
  width: 100%;
  flex-wrap: wrap;
}
</style>
