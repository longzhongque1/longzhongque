<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css'
import { deployApp, downloadAppCode, getAppVoById } from '@/api/appController.ts'
import { listAppChatHistoryVoByPage } from '@/api/chatHistoryController.ts'
import { BASE_URL } from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import AppDetailModal from '@/components/AppDetailModal.vue'
import { useVisualEditor } from '@/pages/app/useVisualEditor'

type ChatMessage = {
  id: string
  role: 'user' | 'ai'
  content: string
  createTime?: string
  isHistory?: boolean
}

const HISTORY_PAGE_SIZE = 10

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const appId = String(route.params.id ?? '')
const loading = ref(false)
const downloading = ref(false)
const deploying = ref(false)
const appInfo = ref<API.AppVO>()
const userInput = ref('')
const messageList = ref<ChatMessage[]>([])
const messageListRef = ref<HTMLElement | null>(null)
const generatedDone = ref(false)
const historyLoading = ref(false)
const loadingMoreHistory = ref(false)
const historyTotal = ref(0)
const historyCursor = ref<string>()
const hasMoreHistory = ref(false)
const deployedUrl = ref('')
const previewUrl = ref('')
const previewIframeRef = ref<HTMLIFrameElement | null>(null)
const detailVisible = ref(false)
const iframeCacheBuster = ref(0)
const tempMessageSeed = ref(0)
let eventSource: EventSource | null = null

const {
  isEditMode,
  selectedElementInfo,
  bindIframe,
  toggleEditMode,
  exitEditMode,
  clearSelectedElement,
  buildPromptWithSelectedElement,
  syncMode,
} = useVisualEditor()

watch(previewIframeRef, (val) => {
  bindIframe(val)
})

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('css', css)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch {
        // ignore
      }
    }
    const escaped = str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
    return `<pre class="hljs"><code>${escaped}</code></pre>`
  },
})

const renderMarkdown = (content: string) => {
  return md.render(content)
}

const codeGenTypeText = computed(() => {
  const type = String(appInfo.value?.codeGenType || '').toLowerCase()
  if (!type) {
    return '未知类型'
  }
  if (type === 'vue_project') {
    return 'Vue项目'
  }
  if (type === 'html') {
    return '原生html'
  }
  if (type === 'multi_file') {
    return '原生多文件'
  }
  return String(appInfo.value?.codeGenType)
})

const scrollMessageListToBottom = async () => {
  await nextTick()
  if (!messageListRef.value) {
    return
  }
  messageListRef.value.scrollTop = messageListRef.value.scrollHeight
}

const previewStaticKey = computed(() => {
  if (!appInfo.value?.id) {
    return ''
  }
  return `${appInfo.value.codeGenType || 'html'}_${appInfo.value.id}`
})

const previewSubPath = computed(() => {
  const codeGenType = String(appInfo.value?.codeGenType || '').toLowerCase()
  return codeGenType === 'vue_project' ? 'dist/index.html' : ''
})

const showPreview = computed(() => {
  if (!previewUrl.value) return false
  return generatedDone.value || historyTotal.value >= 2
})

const shouldShowPreviewPlaceholder = computed(() => {
  return generatedDone.value || historyTotal.value >= 2
})

const canChat = computed(() => {
  const ownerId = Number(appInfo.value?.userId ?? 0)
  const loginId = Number(loginUserStore.loginUser?.id ?? 0)
  if (!ownerId || !loginId) {
    return false
  }
  return ownerId === loginId
})

const closeStream = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

const createTempMessageId = (role: ChatMessage['role']) => {
  tempMessageSeed.value += 1
  return `temp-${role}-${Date.now()}-${tempMessageSeed.value}`
}

const resolveMessageRole = (record: API.ChatHistoryVO): ChatMessage['role'] => {
  const rawType = String(record.messageType ?? '').toLowerCase()
  const rawText = String(record.messageTypeText ?? '').toLowerCase()
  const rawValue = `${rawType} ${rawText}`
  if (
    rawValue.includes('user') ||
    rawValue.includes('human') ||
    rawValue.includes('用户') ||
    rawType === '1'
  ) {
    return 'user'
  }
  return 'ai'
}

const buildHistoryMessage = (record: API.ChatHistoryVO, index: number): ChatMessage => {
  const createTime = record.createTime ? String(record.createTime) : undefined
  return {
    id:
      record.id != null
        ? `history-${record.id}`
        : `history-${createTime ?? 'unknown'}-${record.messageType ?? 'unknown'}-${index}`,
    role: resolveMessageRole(record),
    content: String(record.message ?? ''),
    createTime,
    isHistory: true,
  }
}

const mergeMessageList = (messages: ChatMessage[]) => {
  const seen = new Set<string>()
  return messages.filter((item) => {
    if (seen.has(item.id)) {
      return false
    }
    seen.add(item.id)
    return true
  })
}

const updateHistoryState = () => {
  const historyMessages = messageList.value.filter((item) => item.isHistory)
  historyTotal.value = Math.max(historyTotal.value, historyMessages.length)
  hasMoreHistory.value = historyMessages.length < historyTotal.value
}

const extractFragmentFromSseData = (raw: string) => {
  const normalized = raw.trim()
  if (!normalized) return ''
  if (normalized === '[DONE]' || normalized.toUpperCase() === 'DONE') return ''

  const walk = (value: any): string => {
    if (value == null) return ''
    if (typeof value === 'string') return value
    if (Array.isArray(value)) return value.map((item) => walk(item)).join('')
    if (typeof value === 'object') {
      // 常见字段：content / message / data / d
      const candidateKeys = ['content', 'message', 'data', 'd', 'delta', 'text', 'output']
      for (const key of candidateKeys) {
        if (key in value) {
          const candidate = walk(value[key])
          if (candidate) return candidate
        }
      }
      // 兜底：递归拼接对象所有 value，适配不固定结构
      return Object.values(value)
        .map((item) => walk(item))
        .join('')
    }
    return String(value)
  }

  // 情况 1：单个 JSON
  try {
    return walk(JSON.parse(normalized))
  } catch {
    // ignore
  }

  // 情况 2：连续 JSON 对象拼接，如 {"d":"你"}{"d":"好"}
  const objectChunks = normalized.match(/\{[\s\S]*?\}/g)
  if (objectChunks?.length) {
    let merged = ''
    for (const chunk of objectChunks) {
      try {
        merged += walk(JSON.parse(chunk))
      } catch {
        // ignore invalid chunk
      }
    }
    if (merged) {
      return merged
    }
  }

  // 非 JSON：直接当做 token 片段
  return raw
}

const refreshPreviewUrl = () => {
  const staticKey = previewStaticKey.value
  if (!staticKey) {
    previewUrl.value = ''
    return
  }
  const bust = iframeCacheBuster.value ? `?t=${iframeCacheBuster.value}` : ''
  previewUrl.value = `${BASE_URL}/static/${staticKey}/${previewSubPath.value}${bust}`
}

const loadChatHistory = async (loadMore = false) => {
  if (!appId) {
    return 0
  }
  if (loadMore) {
    if (loadingMoreHistory.value || !hasMoreHistory.value || !historyCursor.value) {
      return messageList.value.filter((item) => item.isHistory).length
    }
    loadingMoreHistory.value = true
  } else {
    historyLoading.value = true
  }
  try {
    // @ts-ignore 保留 long 精度需要以 string 形式传入
    const res = await listAppChatHistoryVoByPage({
      appId: appId as unknown as number,
      pageSize: HISTORY_PAGE_SIZE,
      sortField: 'createTime',
      sortOrder: 'desc',
      lastCreateTime: loadMore ? historyCursor.value : undefined,
    })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('获取对话历史失败，' + res.data.message)
      return messageList.value.filter((item) => item.isHistory).length
    }
    const records = res.data.data.records ?? []
    historyTotal.value = Number(res.data.data.totalRow ?? 0)
    const sortedRecords = [...records].sort((a, b) => {
      const left = a.createTime ? dayjs(a.createTime).valueOf() : 0
      const right = b.createTime ? dayjs(b.createTime).valueOf() : 0
      return left - right
    })
    const nextMessages = sortedRecords.map((record, index) => buildHistoryMessage(record, index))
    if (loadMore) {
      messageList.value = mergeMessageList([...nextMessages, ...messageList.value])
    } else {
      messageList.value = nextMessages
    }
    historyCursor.value = nextMessages[0]?.createTime ?? historyCursor.value
    updateHistoryState()
    return messageList.value.filter((item) => item.isHistory).length
  } finally {
    if (loadMore) {
      loadingMoreHistory.value = false
    } else {
      historyLoading.value = false
    }
  }
}

const fetchAppDetail = async () => {
  // @ts-ignore 保留 long 精度需要以 string 形式传入
  const res = await getAppVoById({ id: appId as unknown as number })
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    refreshPreviewUrl()
    return
  }
  message.error('获取应用详情失败，' + res.data.message)
}

const sendMessage = async (input?: string) => {
  if (!canChat.value) {
    return
  }
  const content = (input ?? userInput.value).trim()
  if (!content || loading.value) {
    return
  }
  const messageToSend = buildPromptWithSelectedElement(content)
  loading.value = true
  generatedDone.value = false
  messageList.value.push({
    id: createTempMessageId('user'),
    role: 'user',
    content,
  })
  const aiMessage: ChatMessage = {
    id: createTempMessageId('ai'),
    role: 'ai',
    content: '',
  }
  messageList.value.push(aiMessage)
  userInput.value = ''
  clearSelectedElement()
  exitEditMode()
  await scrollMessageListToBottom()

  closeStream()

  const url = `${BASE_URL}/app/chat/gen/code?appId=${encodeURIComponent(appId)}&message=${encodeURIComponent(messageToSend)}`
  try {
    eventSource = new EventSource(url, { withCredentials: true })

    eventSource.onmessage = (event) => {
      const rawData = event.data ?? ''
      const normalized = String(rawData).trim()
      if (!normalized) {
        return
      }
      if (normalized === '[DONE]' || normalized.toUpperCase() === 'DONE') {
        loading.value = false
        generatedDone.value = true
        iframeCacheBuster.value = Date.now()
        refreshPreviewUrl()
        closeStream()
        return
      }

      const fragment = extractFragmentFromSseData(normalized)
      if (fragment) {
        aiMessage.content += fragment
        messageList.value = [...messageList.value]
        scrollMessageListToBottom()
      }
    }

    // 监听 business-error 事件
    eventSource.addEventListener('business-error', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.error && data.message) {
          // 添加错误消息到对话列表
          const errorMessage: ChatMessage = {
            id: createTempMessageId('ai'),
            role: 'ai',
            content: `❌ 错误: ${data.message}${data.code ? ` (代码: ${data.code})` : ''}`,
          }
          messageList.value.push(errorMessage)
          messageList.value = [...messageList.value]
          scrollMessageListToBottom()
        }
      } catch (e) {
        console.error('解析 business-error 事件数据失败:', e)
      }
    })

    // 监听 done 事件
    eventSource.addEventListener('done', (event) => {
      loading.value = false
      generatedDone.value = true
      iframeCacheBuster.value = Date.now()
      refreshPreviewUrl()
      closeStream()
    })

    eventSource.onerror = () => {
      loading.value = false
      generatedDone.value = true
      iframeCacheBuster.value = Date.now()
      refreshPreviewUrl()
      closeStream()
    }
  } catch (e: any) {
    loading.value = false
    generatedDone.value = true
    iframeCacheBuster.value = Date.now()
    refreshPreviewUrl()
    message.error('生成失败，' + (e?.message || '未知错误'))
  }
}

const doDeploy = async () => {
  deploying.value = true
  try {
    // @ts-ignore 保留 long 精度需要以 string 形式传入
    const res = await deployApp({ appId: appId as unknown as number })
    if (res.data.code === 0) {
      deployedUrl.value = res.data.data ?? ''
      await fetchAppDetail()
      iframeCacheBuster.value = Date.now()
      refreshPreviewUrl()
      message.success('部署成功')
      return
    }
    message.error('部署失败，' + res.data.message)
  } finally {
    deploying.value = false
  }
}

const parseDownloadFileName = (contentDisposition: string, fallback: string) => {
  const utf8Name = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (utf8Name) {
    try {
      return decodeURIComponent(utf8Name)
    } catch {
      return utf8Name
    }
  }
  const normalName = contentDisposition.match(/filename="?([^";]+)"?/i)?.[1]
  return normalName || fallback
}

const doDownloadCode = async () => {
  if (!appId) {
    message.error('应用 id 无效')
    return
  }
  downloading.value = true
  try {
    // @ts-ignore 保留 long 精度需要以 string 形式传入
    const res = await downloadAppCode(
      { appId: appId as unknown as number },
      {
        responseType: 'blob',
      },
    )
    const contentDisposition = String(res.headers?.['content-disposition'] ?? '')
    const contentType = String(res.headers?.['content-type'] ?? 'application/zip')
    const fileName = parseDownloadFileName(contentDisposition, `${appId}.zip`)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data], { type: contentType })
    if (!blob.size) {
      throw new Error('下载文件为空')
    }
    if (contentType.includes('application/json') || contentType.includes('text/plain')) {
      const text = await blob.text()
      throw new Error(text || '后端返回了错误信息，请检查下载接口')
    }
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    link.rel = 'noopener'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    // 下载大文件时浏览器写盘可能耗时较久，避免过早 revoke 导致下载中断
    window.setTimeout(() => {
      window.URL.revokeObjectURL(objectUrl)
    }, 10 * 60 * 1000)
    message.success('已开始下载，请等待浏览器完成保存')
  } catch (e: any) {
    // XHR 下载失败时，回退为浏览器原生下载，直接走后端 Content-Disposition
    const fallbackUrl = `${BASE_URL}/app/download/${encodeURIComponent(appId)}`
    window.open(fallbackUrl, '_blank', 'noopener,noreferrer')
    message.warning('下载切换为浏览器直连模式，请检查新窗口下载情况')
  } finally {
    downloading.value = false
  }
}

const goHome = () => {
  router.push('/')
}

const onDetailDeleted = () => {
  detailVisible.value = false
  router.push('/')
}

onMounted(async () => {
  await fetchAppDetail()
  const historyCount = await loadChatHistory()
  const initPrompt = String(route.query.initPrompt || '').trim()
  if (initPrompt && canChat.value && historyCount === 0) {
    await sendMessage(initPrompt)
  }
})

onBeforeUnmount(() => {
  exitEditMode()
  closeStream()
})
</script>

<template>
  <div id="appChatPage">
    <div class="top-bar">
      <div class="title-area">
        <h2>{{ appInfo?.appName || '应用对话页' }}</h2>
        <a-tag color="blue">{{ codeGenTypeText }}</a-tag>
      </div>
      <a-space>
        <a-button @click="goHome">返回主页</a-button>
        <a-button @click="detailVisible = true">应用详情</a-button>
        <a-button :loading="downloading" @click="doDownloadCode">下载代码</a-button>
        <a-button type="primary" :loading="deploying" @click="doDeploy">部署</a-button>
        <a v-if="deployedUrl" :href="deployedUrl" target="_blank">访问部署地址</a>
      </a-space>
    </div>
    <div class="content">
      <div class="chat-panel">
        <div v-if="hasMoreHistory" class="load-more-bar">
          <a-button type="link" :loading="loadingMoreHistory" @click="loadChatHistory(true)">
            加载更多
          </a-button>
        </div>
        <div ref="messageListRef" class="message-list">
          <a-spin :spinning="historyLoading">
            <a-empty v-if="!messageList.length && !historyLoading" description="暂无对话消息" />
          </a-spin>
          <div
            v-for="(chatMessage, index) in messageList"
            :key="chatMessage.id"
            class="message-item"
            :class="chatMessage.role"
          >
            <div
              v-if="chatMessage.role === 'ai' && chatMessage.content"
              class="markdown-body"
              v-html="renderMarkdown(chatMessage.content)"
            />
            <template v-else>
              {{ chatMessage.content || (loading && chatMessage.role === 'ai' ? '正在生成中...' : '') }}
            </template>
          </div>
        </div>
        <div class="input-box">
          <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            show-icon
            closable
            :message="`已选中元素：${selectedElementInfo.tagName}`"
            :description="`选择器：${selectedElementInfo.selector || '-'}${selectedElementInfo.textPreview ? ` | 文本：${selectedElementInfo.textPreview}` : ''}`"
            @close="clearSelectedElement"
          />
          <a-tooltip :title="canChat ? '' : '无法在别人的作品下对话哦~'">
            <span class="textarea-wrapper">
              <a-textarea
                v-model:value="userInput"
                :rows="3"
                :disabled="!canChat"
                :placeholder="canChat ? '请描述你想生成的网站，越详细效果越好哦' : '无法在别人的作品下对话哦~'"
              />
            </span>
          </a-tooltip>
          <div class="send-action">
            <a-button :type="isEditMode ? 'default' : 'dashed'" :disabled="!showPreview" @click="toggleEditMode">
              {{ isEditMode ? '退出编辑模式' : '进入编辑模式' }}
            </a-button>
            <a-button type="primary" :loading="loading" :disabled="!canChat" @click="sendMessage()">发送</a-button>
          </div>
        </div>
      </div>
      <div class="preview-panel">
        <iframe
          v-if="showPreview"
          ref="previewIframeRef"
          class="preview-frame"
          :src="previewUrl"
          @load="syncMode"
        />
        <div v-else-if="shouldShowPreviewPlaceholder">
          <a-empty description="代码生成完成后，这里会展示网页效果" />
        </div>
        <a-empty v-else description="代码生成完成后，这里会展示网页效果" />
      </div>
    </div>
  </div>
  <AppDetailModal
    :open="detailVisible"
    :app="appInfo"
    @update:open="detailVisible = $event"
    @deleted="onDetailDeleted"
  />
</template>

<style scoped>
#appChatPage {
  padding: 8px 12px;
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.top-bar h2 {
  margin: 0;
}

.title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content {
  display: grid;
  grid-template-columns: 2fr 3fr;
  gap: 8px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.chat-panel {
  background: #fff;
  border: 1px solid #eaeef5;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.load-more-bar {
  text-align: center;
  padding-bottom: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 8px;
}

.message-item {
  max-width: 100%;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  word-break: break-word;
  overflow-wrap: anywhere;
  min-width: 0;
}

.message-item.ai {
  background: #fff;
  white-space: normal;
}

.message-item.user {
  margin-left: auto;
  max-width: 80%;
  background: #e6f4ff;
  white-space: pre-wrap;
}

.message-item :deep(.markdown-body) {
  font-size: 14px;
  line-height: 1.7;
  overflow: hidden;
}

.message-item :deep(.markdown-body p) {
  margin: 0 0 8px;
}

.message-item :deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}

.message-item :deep(.markdown-body pre.hljs) {
  background: #f6f8fa;
  color: #24292f;
  border-radius: 6px;
  padding: 12px 14px;
  overflow-x: auto;
  max-width: 100%;
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.message-item :deep(.markdown-body code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 13px;
}

.message-item :deep(.markdown-body pre.hljs code) {
  background: none;
  padding: 0;
  border-radius: 0;
  color: inherit;
}

.message-item :deep(.markdown-body ul),
.message-item :deep(.markdown-body ol) {
  padding-left: 20px;
  margin: 4px 0 8px;
}

.message-item :deep(.markdown-body blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #d0d7de;
  color: #656d76;
}

.message-item :deep(.markdown-body h1),
.message-item :deep(.markdown-body h2),
.message-item :deep(.markdown-body h3),
.message-item :deep(.markdown-body h4) {
  margin: 12px 0 6px;
  line-height: 1.4;
}

.message-item :deep(.markdown-body table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.message-item :deep(.markdown-body th),
.message-item :deep(.markdown-body td) {
  border: 1px solid #d0d7de;
  padding: 6px 12px;
  text-align: left;
}

.message-item :deep(.markdown-body th) {
  background: #f6f8fa;
  font-weight: 600;
}

.input-box {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.selected-element-alert {
  margin-bottom: 8px;
}

.textarea-wrapper {
  display: block;
}

.send-action {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.preview-panel {
  background: #fff;
  border: 1px solid #eaeef5;
  border-radius: 12px;
  padding: 10px;
  min-width: 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 8px;
}

@media (max-width: 1200px) {
  #appChatPage {
    height: auto;
    overflow: visible;
  }

  .content {
    grid-template-columns: 1fr;
    min-height: auto;
    overflow: visible;
  }

  .preview-panel {
    min-height: 560px;
  }
}
</style>
