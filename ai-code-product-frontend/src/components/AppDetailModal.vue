<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deleteApp } from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser'

const props = defineProps<{
  open: boolean
  app?: API.AppVO
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  deleted: []
}>()

const router = useRouter()
const loginUserStore = useLoginUserStore()

const canManage = computed(() => {
  const loginId = Number(loginUserStore.loginUser?.id ?? 0)
  const ownerId = Number(props.app?.userId ?? 0)
  const role = loginUserStore.loginUser?.userRole
  if (!loginId) return false
  return ownerId === loginId || role === 'admin'
})

const goToEdit = () => {
  if (!props.app?.id) return
  router.push(`/app/edit/${props.app.id}`)
}

const doDelete = async () => {
  if (!props.app?.id) return
  const res = await deleteApp({ id: props.app.id as unknown as number })
  if (res.data.code === 0) {
    message.success('删除成功')
    emit('deleted')
  } else {
    message.error('删除失败，' + res.data.message)
  }
}
</script>

<template>
  <a-modal
    :open="open"
    title="应用详情"
    :footer="null"
    :width="420"
    @update:open="emit('update:open', $event)"
    @cancel="emit('update:open', false)"
  >
    <div class="detail-section">
      <h4>应用基础信息</h4>
      <div class="detail-item">
        <span class="detail-label">创建者：</span>
        <span class="detail-value creator-info">
          <a-avatar v-if="app?.user?.userAvatar" :src="app.user.userAvatar" :size="24" />
          <a-avatar v-else :size="24">{{ (app?.user?.userName || '?')[0] }}</a-avatar>
          <span class="creator-name">{{ app?.user?.userName || '未知用户' }}</span>
        </span>
      </div>
      <div class="detail-item">
        <span class="detail-label">创建时间：</span>
        <span class="detail-value">{{ app?.createTime ? dayjs(app.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}</span>
      </div>
    </div>
    <div v-if="canManage" class="detail-section">
      <h4>操作</h4>
      <a-space>
        <a-button type="primary" @click="goToEdit">修改</a-button>
        <a-popconfirm title="确定删除该应用吗？" @confirm="doDelete">
          <a-button danger>删除</a-button>
        </a-popconfirm>
      </a-space>
    </div>
  </a-modal>
</template>

<style scoped>
.detail-section {
  margin-bottom: 16px;
}

.detail-section h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.detail-label {
  color: rgba(0, 0, 0, 0.45);
  flex-shrink: 0;
}

.detail-value {
  color: rgba(0, 0, 0, 0.85);
}

.creator-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
