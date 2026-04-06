<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { listChatHistoryVoByPageByAdmin } from '@/api/chatHistoryController.ts'

const columns = [
  { title: 'id', dataIndex: 'id' },
  { title: '消息内容', dataIndex: 'message' },
  { title: '消息类型', dataIndex: 'messageType' },
  { title: '应用 id', dataIndex: 'appId' },
  { title: '用户 id', dataIndex: 'userId' },
  { title: '创建时间', dataIndex: 'createTime' },
]

const data = ref<API.ChatHistoryVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listChatHistoryVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = Number(res.data.data.totalRow ?? 0)
      return
    }
    message.error('获取对话列表失败，' + res.data.message)
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

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="chatHistoryManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="消息内容">
        <a-input v-model:value="searchParams.message" placeholder="输入消息内容" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-input v-model:value="searchParams.messageType" placeholder="输入消息类型" />
      </a-form-item>
      <a-form-item label="应用 id">
        <a-input-number v-model:value="searchParams.appId" placeholder="输入应用 id" />
      </a-form-item>
      <a-form-item label="用户 id">
        <a-input-number v-model:value="searchParams.userId" placeholder="输入用户 id" />
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
        <template v-if="column.dataIndex === 'message'">
          <div class="message-cell" :title="record.message">
            {{ record.message || '-' }}
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'messageType'">
          {{ record.messageTypeText || record.messageType || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#chatHistoryManagePage {
  padding: 24px;
  background: white;
  margin-top: 16px;
}

.message-cell {
  max-width: 420px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>