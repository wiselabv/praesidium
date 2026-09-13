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
 *
 * mock 模式：网关未就绪时的本地回显演示，不建 WebSocket，模拟基础 shell
 * （help / pwd / whoami / ls / uptime / df / free / exit，其余提示 command not found）。
 */
const props = defineProps<{
  /** WebSocket 地址，如 ws://127.0.0.1:8081/ws/terminal */
  wsUrl: string
  /** 可选：Java 签发的访问令牌，连接建立后作为首条消息发送 */
  token?: string
  /** mock 模式：不连 WS，本地模拟 shell（网关未就绪时的开发演示） */
  mock?: boolean
  /** mock 模式下模拟的目标环境（真实实现由网关注入） */
  mockUser?: string
  mockHost?: string
}>()

const emit = defineEmits<{
  /** mock shell 收到 exit 时触发（真实模式由 WS 关闭驱动） */
  (e: 'exit'): void
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
  if (term && !props.mock) {
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

/* ---------------- mock shell ---------------- */

/** 行编辑缓冲 */
let lineBuffer = ''

function mockBanner() {
  const user = props.mockUser ?? 'deploy'
  const host = props.mockHost ?? 'asset'
  term?.writeln('\x1b[1;36m  Praesidium 终端（mock 回显模式）\x1b[0m')
  term?.writeln(`\x1b[90m  已建立到 \x1b[0m${host}\x1b[90m 的会话，SSH 网关代理开发中。\x1b[0m`)
  term?.write('\r\n')
  mockPrompt(user, host)
}

function mockPrompt(user: string, host: string) {
  term?.write(`\x1b[32m${user}@${host}\x1b[0m:\x1b[34m~\x1b[0m$ `)
}

/** 模拟命令输出 */
function mockExecute(cmd: string) {
  const user = props.mockUser ?? 'deploy'
  const host = props.mockHost ?? 'asset'
  const trimmed = cmd.trim()
  if (!trimmed) {
    mockPrompt(user, host)
    return
  }
  term?.write('\r\n')
  const [head] = trimmed.split(/\s+/)
  switch (head) {
    case 'help':
      term?.writeln('可用命令：help / pwd / whoami / ls / uptime / df -h / free -m / exit')
      break
    case 'exit':
    case 'logout':
      term?.writeln('logout')
      emit('exit')
      return
    case 'pwd':
      term?.writeln(`/home/${user}`)
      break
    case 'whoami':
      term?.writeln(user)
      break
    case 'ls':
      term?.writeln('app  backup  logs  nginx.conf  .bashrc')
      break
    case 'uptime':
      term?.writeln(' 10:52:33 up 42 days,  3:17,  1 user,  load average: 0.08, 0.12, 0.09')
      break
    case 'df':
      term?.writeln('Filesystem     1K-blocks     Used Available Use% Mounted on')
      term?.writeln('/dev/vda1       51475068 18330224  30497288  38% /')
      break
    case 'free':
      term?.writeln('              total        used        free      shared  buff/cache   available')
      term?.writeln('Mem:        8167884     2213696     4021028      192884     1933160     5461988')
      break
    default:
      term?.writeln(`bash: ${head}: command not found`)
  }
  mockPrompt(user, host)
}

function handleMockData(data: string) {
  for (const ch of data) {
    if (ch === '\r') {
      term?.write('\r\n')
      mockExecute(lineBuffer)
      lineBuffer = ''
    } else if (ch === '\x7f') {
      // Backspace
      if (lineBuffer.length > 0) {
        lineBuffer = lineBuffer.slice(0, -1)
        term?.write('\b \b')
      }
    } else if (ch === '\x03') {
      // Ctrl+C
      term?.write('^C\r\n')
      lineBuffer = ''
      mockPrompt(props.mockUser ?? 'deploy', props.mockHost ?? 'asset')
    } else {
      lineBuffer += ch
      term?.write(ch)
    }
  }
}

/* ---------------- lifecycle ---------------- */

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
    if (props.mock) {
      handleMockData(data)
      return
    }
    send({ type: 'input', data })
  })

  if (props.mock) {
    status.value = 'connected'
    mockBanner()
  } else {
    connect()
  }

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
    if (props.mock) return
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
