import { onBeforeUnmount, ref } from 'vue'

const VISUAL_EDITOR_CHANNEL = 'app-visual-editor'

type VisualEditorMessage = {
  channel?: string
  type?: string
  payload?: any
}

export type SelectedElementInfo = {
  tagName: string
  id: string
  className: string
  textPreview: string
  selector: string
}

export function useVisualEditor() {
  const isEditMode = ref(false)
  const selectedElementInfo = ref<SelectedElementInfo | null>(null)

  let iframeEl: HTMLIFrameElement | null = null
  let iframeLoadHandler: (() => void) | null = null

  const getBridge = () => {
    return (iframeEl?.contentWindow as any)?.__APP_VISUAL_EDITOR__
  }

  const onMessage = (event: MessageEvent<VisualEditorMessage>) => {
    if (event.origin !== window.location.origin) {
      return
    }
    const data = event.data
    if (!data || data.channel !== VISUAL_EDITOR_CHANNEL) {
      return
    }
    if (data.type === 'selected-element') {
      selectedElementInfo.value = data.payload as SelectedElementInfo
    }
    if (data.type === 'selection-cleared') {
      selectedElementInfo.value = null
    }
  }

  const injectVisualEditorAssets = (iframe: HTMLIFrameElement) => {
    const doc = iframe.contentDocument
    if (!doc) {
      return
    }
    if (!doc.getElementById('__app_visual_editor_style')) {
      const styleEl = doc.createElement('style')
      styleEl.id = '__app_visual_editor_style'
      styleEl.textContent = `
        [data-ve-hover="1"] {
          outline: 2px solid #69b1ff !important;
          cursor: pointer !important;
        }
        [data-ve-selected="1"] {
          outline: 2px solid #1677ff !important;
          box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.35) !important;
        }
      `
      doc.head.appendChild(styleEl)
    }

    if (doc.getElementById('__app_visual_editor_script')) {
      return
    }

    const scriptEl = doc.createElement('script')
    scriptEl.id = '__app_visual_editor_script'
    scriptEl.type = 'text/javascript'
    scriptEl.text = `
      ;(() => {
        if (window.__APP_VISUAL_EDITOR__) {
          return
        }

        const CHANNEL = '${VISUAL_EDITOR_CHANNEL}'
        const HOVER_ATTR = 'data-ve-hover'
        const SELECTED_ATTR = 'data-ve-selected'
        const IGNORE_TAGS = new Set(['HTML', 'BODY', 'SCRIPT', 'STYLE', 'LINK', 'META'])

        let enabled = false
        let hoverEl = null
        let selectedEl = null

        const post = (type, payload) => {
          try {
            window.parent.postMessage({ channel: CHANNEL, type, payload }, window.location.origin)
          } catch {
            // ignore
          }
        }

        const isValidElement = (el) => {
          return !!el && el.nodeType === 1 && !IGNORE_TAGS.has(el.tagName)
        }

        const clearHover = () => {
          if (hoverEl && hoverEl !== selectedEl) {
            hoverEl.removeAttribute(HOVER_ATTR)
          }
          hoverEl = null
        }

        const clearSelection = () => {
          if (selectedEl) {
            selectedEl.removeAttribute(SELECTED_ATTR)
          }
          selectedEl = null
          post('selection-cleared')
        }

        const getSelector = (el) => {
          if (!el || !el.tagName) {
            return ''
          }
          const paths = []
          let cur = el
          while (cur && cur.nodeType === 1 && paths.length < 6) {
            let segment = cur.tagName.toLowerCase()
            if (cur.id) {
              segment += '#' + cur.id
              paths.unshift(segment)
              break
            }
            if (cur.classList && cur.classList.length) {
              segment += '.' + Array.from(cur.classList).slice(0, 2).join('.')
            }
            const parent = cur.parentElement
            if (parent) {
              const siblings = Array.from(parent.children).filter((child) => child.tagName === cur.tagName)
              if (siblings.length > 1) {
                segment += ':nth-of-type(' + (siblings.indexOf(cur) + 1) + ')'
              }
            }
            paths.unshift(segment)
            cur = parent
          }
          return paths.join(' > ')
        }

        const buildInfo = (el) => {
          const text = String(el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 120)
          return {
            tagName: String(el.tagName || '').toLowerCase(),
            id: String(el.id || ''),
            className: String(el.className || ''),
            textPreview: text,
            selector: getSelector(el),
          }
        }

        const onMouseOver = (event) => {
          if (!enabled) {
            return
          }
          const target = event.target
          if (!isValidElement(target)) {
            return
          }
          if (hoverEl && hoverEl !== selectedEl) {
            hoverEl.removeAttribute(HOVER_ATTR)
          }
          hoverEl = target
          if (hoverEl !== selectedEl) {
            hoverEl.setAttribute(HOVER_ATTR, '1')
          }
        }

        const onMouseOut = (event) => {
          if (!enabled) {
            return
          }
          const target = event.target
          if (target === hoverEl && hoverEl !== selectedEl) {
            hoverEl.removeAttribute(HOVER_ATTR)
            hoverEl = null
          }
        }

        const onClick = (event) => {
          if (!enabled) {
            return
          }
          const target = event.target
          if (!isValidElement(target)) {
            return
          }
          event.preventDefault()
          event.stopPropagation()
          if (selectedEl) {
            selectedEl.removeAttribute(SELECTED_ATTR)
          }
          if (hoverEl && hoverEl !== target) {
            hoverEl.removeAttribute(HOVER_ATTR)
          }
          selectedEl = target
          selectedEl.setAttribute(SELECTED_ATTR, '1')
          selectedEl.removeAttribute(HOVER_ATTR)
          post('selected-element', buildInfo(selectedEl))
        }

        const enable = () => {
          if (enabled) {
            return
          }
          enabled = true
          document.addEventListener('mouseover', onMouseOver, true)
          document.addEventListener('mouseout', onMouseOut, true)
          document.addEventListener('click', onClick, true)
        }

        const disable = () => {
          if (!enabled) {
            return
          }
          enabled = false
          document.removeEventListener('mouseover', onMouseOver, true)
          document.removeEventListener('mouseout', onMouseOut, true)
          document.removeEventListener('click', onClick, true)
          clearHover()
          if (selectedEl) {
            selectedEl.removeAttribute(SELECTED_ATTR)
          }
        }

        window.__APP_VISUAL_EDITOR__ = {
          enable,
          disable,
          clearSelection,
        }
      })()
    `
    ;(doc.body || doc.documentElement).appendChild(scriptEl)
  }

  const syncMode = () => {
    const bridge = getBridge()
    if (!bridge) {
      return
    }
    if (isEditMode.value) {
      bridge.enable?.()
    } else {
      bridge.disable?.()
    }
  }

  const bindIframe = (iframe: HTMLIFrameElement | null) => {
    if (iframeEl && iframeLoadHandler) {
      iframeEl.removeEventListener('load', iframeLoadHandler)
    }
    iframeEl = iframe
    if (!iframeEl) {
      return
    }
    iframeLoadHandler = () => {
      injectVisualEditorAssets(iframeEl as HTMLIFrameElement)
      syncMode()
    }
    iframeEl.addEventListener('load', iframeLoadHandler)
    injectVisualEditorAssets(iframeEl)
    syncMode()
  }

  const toggleEditMode = () => {
    isEditMode.value = !isEditMode.value
    syncMode()
  }

  const exitEditMode = () => {
    isEditMode.value = false
    syncMode()
  }

  const clearSelectedElement = () => {
    selectedElementInfo.value = null
    getBridge()?.clearSelection?.()
  }

  const buildPromptWithSelectedElement = (originPrompt: string) => {
    const selected = selectedElementInfo.value
    if (!selected) {
      return originPrompt
    }
    const lines = [
      originPrompt,
      '',
      '[当前选中元素]',
      `- 标签: ${selected.tagName || '-'}`,
      `- id: ${selected.id || '-'}`,
      `- class: ${selected.className || '-'}`,
      `- 选择器: ${selected.selector || '-'}`,
      `- 文本: ${selected.textPreview || '-'}`,
      '请优先修改该元素及其相关样式，同时保持页面整体风格一致。',
    ]
    return lines.join('\n')
  }

  window.addEventListener('message', onMessage)
  onBeforeUnmount(() => {
    if (iframeEl && iframeLoadHandler) {
      iframeEl.removeEventListener('load', iframeLoadHandler)
    }
    window.removeEventListener('message', onMessage)
  })

  return {
    isEditMode,
    selectedElementInfo,
    bindIframe,
    toggleEditMode,
    exitEditMode,
    clearSelectedElement,
    buildPromptWithSelectedElement,
    syncMode,
  }
}
