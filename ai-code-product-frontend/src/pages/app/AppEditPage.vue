<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppVoById, getAppVoByIdByAdmin, updateApp, updateAppByAdmin } from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const appId = String(route.params.id ?? '')
const loading = ref(false)
const appInfo = ref<API.AppVO>()

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const formState = reactive<API.AppAdminUpdateRequest>({
  // @ts-ignore 保留 long 精度需要以 string 形式传入
  id: appId as unknown as number,
  appName: '',
  cover: '',
  priority: 0,
})

const fetchDetail = async () => {
  // @ts-ignore 保留 long 精度需要以 string 形式传入
  const res = isAdmin.value
    ? await getAppVoByIdByAdmin({ id: appId as unknown as number })
    : await getAppVoById({ id: appId as unknown as number })
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    formState.appName = res.data.data.appName || ''
    formState.cover = res.data.data.cover || ''
    formState.priority = Number(res.data.data.priority || 0)
    if (!isAdmin.value && res.data.data.userId !== loginUserStore.loginUser.id) {
      message.error('你只能编辑自己的应用')
      router.push('/')
    }
    return
  }
  message.error('获取应用信息失败，' + res.data.message)
}

const doSubmit = async () => {
  loading.value = true
  try {
    // @ts-ignore 保留 long 精度需要以 string 形式传入
    const res = isAdmin.value
      ? await updateAppByAdmin({
          id: appId as unknown as number,
          appName: formState.appName,
          cover: formState.cover,
          priority: formState.priority,
        })
      : await updateApp({
          id: appId as unknown as number,
          appName: formState.appName,
        })
    if (res.data.code === 0) {
      message.success('更新成功')
      await router.push('/')
      return
    }
    message.error('更新失败，' + res.data.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div id="appEditPage">
    <a-card :title="`编辑应用：${appInfo?.appName || ''}`">
      <a-form layout="vertical" :model="formState" @finish="doSubmit">
        <a-form-item label="应用名称" name="appName" :rules="[{ required: true, message: '请输入应用名称' }]">
          <a-input v-model:value="formState.appName" placeholder="请输入应用名称" />
        </a-form-item>
        <template v-if="isAdmin">
          <a-form-item label="应用封面" name="cover">
            <a-input v-model:value="formState.cover" placeholder="请输入封面地址" />
          </a-form-item>
          <a-form-item label="优先级" name="priority">
            <a-input-number v-model:value="formState.priority" :min="0" :max="999" style="width: 100%" />
          </a-form-item>
        </template>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="loading">保存</a-button>
            <a-button @click="router.back()">返回</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
#appEditPage {
  padding: 24px;
}
</style>
