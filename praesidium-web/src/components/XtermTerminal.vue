<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebglAddon } from '@xterm/addon-webgl'
import '@xterm/xterm/css/xterm.css'

/**
 * 浏览器终端组件（xterm.js）。
 *
 * 与 Rust 核心代理的 WebSocket 约定：
 * - 客户端 → 服务端：JSON 帧 `{ type: 'auth' | 'input' | 'resize', ... }`
 * - 服务端 → 客户端：终端输出文本（直接写入 xterm）
 */
const props = defineProps<{
  /** WebSocket 地址，如 ws://127.0.0.1:8081/ws/terminal */
  wsUrl: string
  /** 可选：Java 签发的访问令牌，连接建立后作为首条消息发送 */
  token?: string
}>()

const container = ref<HTMLDivElement | null>(null)
const status = ref<'connecting' | 'connected' | 'closed'>('connecting')

let term: Terminal | null = null
let fitAddon: FitAddon | null = null
let socket: WebSocket | null = null
let resizeObserver: ResizeObserver | null = null

function send(payload: Record<string, unknown>) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
  }
}

function fitAndNotify() {
  try {
    fitAddon?.fit()
  } catch {
    // 容器尚未完成布局时忽略本次尺寸调整
  }
  if (term) {
    send({ type: 'resize', cols: term.cols, rows: term.rows })
  }
}

function connect() {
  status.value = 'connecting'
  const ws = new WebSocket(props.wsUrl)
  socket = ws

  ws.onopen = () => {
    status.value = 'connected'
    if (props.token) {
      send({ type: 'auth', token: props.token })
    }
    fitAndNotify()
  }

  ws.onmessage = (event: MessageEvent<string>) => {
    term?.write(event.data)
  }

  ws.onclose = () => {
    status.value = 'closed'
  }

  ws.onerror = () => {
    term?.write('\r\n\x1b[31m[连接错误] 无法连接终端网关\x1b[0m\r\n')
  }
}

onMounted(() => {
  term = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Consolas, "Cascadia Mono", "Courier New", monospace',
    scrollback: 5000,
  })

  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)

  if (container.value) {
    term.open(container.value)
    try {
      term.loadAddon(new WebglAddon())
    } catch {
      // WebGL 不可用时自动回退到 Canvas 渲染
    }
    fitAndNotify()
  }

  term.onData((data) => {
    send({ type: 'input', data })
  })

  connect()

  resizeObserver = new ResizeObserver(() => fitAndNotify())
  if (container.value) {
    resizeObserver.observe(container.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  socket?.close()
  socket = null
  term?.dispose()
  term = null
})

watch(
  () => props.wsUrl,
  () => {
    socket?.close()
    connect()
  },
)
</script>

<template>
  <div class="xterm-wrapper">
    <div ref="container" class="xterm-container" />
    <div class="xterm-status" :data-status="status">
      {{ status === 'connected' ? '已连接' : status === 'connecting' ? '连接中…' : '已断开' }}
    </div>
  </div>
</template>

<style scoped>
.xterm-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.xterm-container {
  flex: 1 1 auto;
  min-height: 0;
  padding: 8px;
  background: #16181d;
}

.xterm-status {
  padding: 4px 12px;
  font-size: 12px;
  border-top: 1px solid #2a2d34;
  color: #9ca3af;
}

.xterm-status[data-status='connected'] {
  color: #4ade80;
}

.xterm-status[data-status='connecting'] {
  color: #facc15;
}

.xterm-status[data-status='closed'] {
  color: #f87171;
}
</style>
