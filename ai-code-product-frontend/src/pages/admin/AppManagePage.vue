<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  deleteAppByAdmin,
  listAppVoByPageByAdmin,
  updateAppByAdmin,
} from '@/api/appController.ts'

const router = useRouter()

const columns = [
  { title: 'id', dataIndex: 'id' },
  { title: '应用名称', dataIndex: 'appName' },
  { title: '封面', dataIndex: 'cover' },
  { title: '初始提示词', dataIndex: 'initPrompt' },
  { title: '代码类型', dataIndex: 'codeGenType' },
  { title: '部署标识', dataIndex: 'deployKey' },
  { title: '优先级', dataIndex: 'priority' },
  { title: '用户 id', dataIndex: 'userId' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = Number(res.data.data.totalRow ?? 0)
      return
    }
    message.error('获取应用列表失败，' + res.data.message)
  } finally {
    loading.value = false
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 20,
  total: total.value,
  showSizeChanger: true,
  showTotal: (v: number) => `共 ${v} 条`,
}))

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doEdit = (id?: string | number) => {
  if (!id) {
    return
  }
  router.push(`/app/edit/${String(id)}`)
}

const doDelete = async (id?: string | number) => {
  if (!id) {
    return
  }
  const res = await deleteAppByAdmin({ id: id as unknown as number })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
    return
  }
  message.error('删除失败，' + res.data.message)
}

const doSetGood = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }
  const isGood = (record.priority ?? 0) > 0
  const res = await updateAppByAdmin({
    id: record.id,
    appName: record.appName,
    cover: record.cover,
    priority: isGood ? 0 : 99,
  })
  if (res.data.code === 0) {
    message.success(isGood ? '取消精选成功' : '设置精选成功')
    fetchData()
  } else {
    message.error((isGood ? '取消精选失败，' : '设置精选失败，') + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="appManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="用户 id">
        <a-input-number v-model:value="searchParams.userId" placeholder="输入用户 id" />
      </a-form-item>
      <a-form-item label="代码类型">
        <a-input v-model:value="searchParams.codeGenType" placeholder="输入代码类型" />
      </a-form-item>
      <a-form-item label="优先级">
        <a-input-number v-model:value="searchParams.priority" placeholder="输入优先级" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="doEdit(record.id)">编辑</a-button>
            <a-popconfirm title="确定删除该应用吗？" @confirm="doDelete(record.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
            <a-button type="link" @click="doSetGood(record)">
              {{ (record.priority ?? 0) > 0 ? '取消精选' : '精选' }}
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#appManagePage {
  padding: 24px;
  background: white;
  margin-top: 16px;
}
</style>
