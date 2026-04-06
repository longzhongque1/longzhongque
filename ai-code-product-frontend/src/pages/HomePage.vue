<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()

const creating = ref(false)
const initPrompt = ref('')
const myLoading = ref(false)
const goodLoading = ref(false)
const activeTab = ref<'my' | 'good'>('my')

const myApps = ref<API.AppVO[]>([])
const goodApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const goodTotal = ref(0)

const mySearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 8,
})

const goodSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 8,
})

const createAppByPrompt = async () => {
  if (!initPrompt.value.trim()) {
    message.warning('请输入应用需求提示词')
    return
  }
  creating.value = true
  try {
    const res = await addApp({
      initPrompt: initPrompt.value.trim(),
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建应用成功，开始生成代码')
      await router.push(`/app/chat/${res.data.data}?initPrompt=${encodeURIComponent(initPrompt.value.trim())}`)
      initPrompt.value = ''
      return
    }
    message.error('创建应用失败，' + res.data.message)
  } finally {
    creating.value = false
  }
}

const fetchMyApps = async () => {
  myLoading.value = true
  try {
    const res = await listMyAppVoByPage({
      ...mySearchParams,
      pageSize: Math.min(mySearchParams.pageSize ?? 8, 20),
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myTotal.value = Number(res.data.data.totalRow ?? 0)
    } else {
      message.error('获取我的应用失败，' + res.data.message)
    }
  } finally {
    myLoading.value = false
  }
}

const fetchGoodApps = async () => {
  goodLoading.value = true
  try {
    const res = await listGoodAppVoByPage({
      ...goodSearchParams,
      pageSize: Math.min(goodSearchParams.pageSize ?? 8, 20),
    })
    if (res.data.code === 0 && res.data.data) {
      goodApps.value = res.data.data.records ?? []
      goodTotal.value = Number(res.data.data.totalRow ?? 0)
    } else {
      message.error('获取精选应用失败，' + res.data.message)
    }
  } finally {
    goodLoading.value = false
  }
}

const goToAppChat = (id?: string | number) => {
  if (!id) {
    return
  }
  router.push(`/app/chat/${String(id)}`)
}

const goToAppWork = (deployKey?: string) => {
  const key = String(deployKey ?? '').trim()
  if (!key) {
    return
  }
  const url = `${import.meta.env.VITE_DEPLOY_BASE_URL}/${key}/`
  window.open(url, '_blank', 'noopener,noreferrer')
}

const myPagination = computed(() => ({
  current: mySearchParams.pageNum,
  pageSize: mySearchParams.pageSize,
  total: myTotal.value,
  showSizeChanger: false,
}))

const goodPagination = computed(() => ({
  current: goodSearchParams.pageNum,
  pageSize: goodSearchParams.pageSize,
  total: goodTotal.value,
  showSizeChanger: false,
}))

onMounted(() => {
  fetchMyApps()
  fetchGoodApps()
})
</script>

<template>
  <div id="homePage">
    <div class="hero-area">
      <div class="hero-content">
        <h1>AI 应用生成平台</h1>
        <p>一句话轻松创建网站应用</p>
      </div>
      <div class="prompt-box">
        <a-textarea
          v-model:value="initPrompt"
          :rows="4"
          :maxlength="2000"
          placeholder="帮我创建个人博客网站"
        />
        <div class="create-action">
          <a-space wrap>
            <a-button @click="initPrompt = '帮我生成一个个人博客网站，顶部导航栏包含首页、文章分类、关于我，首页展示文章列表卡片，每张卡片有标题、摘要和发布日期，页面风格简洁现代，配色以深蓝和白色为主'">
              个人博客
            </a-button>
            <a-button @click="initPrompt = '帮我生成一个餐饮店铺官网，顶部有 Logo 和导航栏，banner 展示招牌菜品大图，下方分区展示菜单推荐、门店环境照片轮播、营业时间和地址信息，底部有联系电话和微信二维码占位'">
              餐饮官网
            </a-button>
            <a-button @click="initPrompt = '帮我生成一个产品发布落地页，顶部大标题和副标题介绍产品核心卖点，中间用图标加文字展示三个核心功能特性，接着是产品截图展示区，底部有立即体验的行动号召按钮和常见问题 FAQ 手风琴'">
              产品落地页
            </a-button>
            <a-button @click="initPrompt = '帮我生成一个在线简历网页，左侧固定侧边栏展示头像、姓名、联系方式和社交链接，右侧主区域从上到下依次是个人简介、工作经历时间轴、项目经验卡片和技能进度条，风格专业简洁'">
              在线简历
            </a-button>
            <a-button type="primary" :loading="creating" @click="createAppByPrompt">创建应用</a-button>
          </a-space>
        </div>
      </div>
    </div>

    <div class="case-wrapper">
      <div class="case-header">
        <h2>案例广场</h2>
        <a-space>
          <a-button :type="activeTab === 'my' ? 'primary' : 'default'" @click="activeTab = 'my'">
            我的应用
          </a-button>
          <a-button :type="activeTab === 'good' ? 'primary' : 'default'" @click="activeTab = 'good'">
            精选应用
          </a-button>
        </a-space>
      </div>
      <a-form
        v-if="activeTab === 'my'"
        layout="inline"
        :model="mySearchParams"
        class="search-form"
        @finish="fetchMyApps"
      >
        <a-form-item>
          <a-input v-model:value="mySearchParams.appName" allow-clear placeholder="按名称搜索我的应用" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
      <a-form
        v-else
        layout="inline"
        :model="goodSearchParams"
        class="search-form"
        @finish="fetchGoodApps"
      >
        <a-form-item>
          <a-input v-model:value="goodSearchParams.appName" allow-clear placeholder="按名称搜索精选应用" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>

      <div v-if="activeTab === 'my'" class="card-grid">
        <a-spin :spinning="myLoading">
          <div class="grid-inner">
            <AppCard
              v-for="item in myApps"
              :key="item.id"
              :app="item"
              @view-chat="goToAppChat"
              @view-work="goToAppWork"
            />
          </div>
        </a-spin>
        <a-pagination
          v-model:current="mySearchParams.pageNum"
          v-model:page-size="mySearchParams.pageSize"
          class="pager"
          :total="myPagination.total"
          :show-size-changer="false"
          @change="fetchMyApps"
        />
      </div>

      <div v-else class="card-grid">
        <a-spin :spinning="goodLoading">
          <div class="grid-inner">
            <AppCard
              v-for="item in goodApps"
              :key="item.id"
              :app="item"
              placeholder-text="精选"
              @view-chat="goToAppChat"
              @view-work="goToAppWork"
            />
          </div>
        </a-spin>
        <a-pagination
          v-model:current="goodSearchParams.pageNum"
          v-model:page-size="goodSearchParams.pageSize"
          class="pager"
          :total="goodPagination.total"
          :show-size-changer="false"
          @change="fetchGoodApps"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  padding: 0 0 24px;
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #0f0c29 0%, #1a1a4e 25%, #24243e 50%, #1b1b3a 75%, #0f0c29 100%);
}

.hero-area {
  min-height: 420px;
  padding-top: 100px;
}

.hero-content {
  text-align: center;
}

.hero-content h1 {
  font-size: 52px;
  margin-bottom: 8px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
}

.hero-content p {
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 32px;
  font-size: 18px;
}

.prompt-box {
  max-width: 740px;
  margin: 0 auto;
  padding: 16px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
}

.prompt-box :deep(.ant-input) {
  background: rgba(255, 255, 255, 0.06);
  border: none;
  color: #fff;
  resize: none;
}

.prompt-box :deep(.ant-input::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.prompt-box :deep(.ant-input:focus) {
  box-shadow: none;
}

.create-action {
  margin-top: 12px;
  text-align: right;
}

.create-action :deep(.ant-btn-default) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.85);
}

.create-action :deep(.ant-btn-default:hover) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}

.case-wrapper {
  max-width: 1280px;
  margin: 32px auto 0;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 20px;
}

.case-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.case-header h2 {
  margin: 0;
  font-size: 24px;
  color: #fff;
}

.case-header :deep(.ant-btn-default) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.75);
}

.case-header :deep(.ant-btn-default:hover) {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.case-header :deep(.ant-btn-primary) {
  box-shadow: none;
}

.search-form {
  margin-top: 16px;
}

.search-form :deep(.ant-input) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.search-form :deep(.ant-input::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.card-grid {
  margin-top: 16px;
}

.grid-inner {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.pager {
  margin-top: 18px;
  text-align: center;
}

.pager :deep(.ant-pagination-item) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.15);
}

.pager :deep(.ant-pagination-item a) {
  color: rgba(255, 255, 255, 0.7);
}

.pager :deep(.ant-pagination-item-active) {
  border-color: #1677ff;
}

.pager :deep(.ant-pagination-prev .ant-pagination-item-link),
.pager :deep(.ant-pagination-next .ant-pagination-item-link) {
  color: rgba(255, 255, 255, 0.7);
}

@media (max-width: 1200px) {
  .grid-inner {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .hero-content h1 {
    font-size: 40px;
  }

  .grid-inner {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-area {
    padding: 96px 12px 0;
  }

  .case-wrapper {
    margin: 0 12px;
  }

  .grid-inner {
    grid-template-columns: 1fr;
  }
}
</style>