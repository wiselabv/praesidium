<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import '@xterm/xterm/css/xterm.css'
import { Message } from '@arco-design/web-vue'
import { listRecordingObjects } from '../../../api/sessions'
import type { RecordingItem } from '../../../api/sessions'

/**
 * 录像回放播放器：按切片顺序拉取 MinIO 预签名对象，逐片写入 xterm 只读实例。
 *
 * 录像内容为网关记录的终端输出字节流（.cast），直接渲染回终端即可还原现场；
 * 切片按 recordingPath 顺序（0.cast → 1.cast …）拼接播放。
 */
const props = defineProps<{
  /** 待回放的录像（列表行） */
  recording: RecordingItem
}>()

const container = ref<HTMLDivElement | null>(null)
const state = ref<'loading' | 'playing' | 'done' | 'error'>('loading')
const progress = ref(0)

let term: Terminal | null = null
let fitAddon: FitAddon | null = null
let aborted = false

onMounted(async () => {
  term = new Terminal({
    cursorBlink: false,
    fontSize: 13,
    fontFamily: 'Consolas, "Cascadia Mono", "Courier New", monospace',
    scrollback: 100000,
    disableStdin: true,
  })
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  if (container.value) {
    term.open(container.value)
    try {
      fitAddon.fit()
    } catch {
      // 布局未就绪时忽略
    }
  }

  try {
    const objects = await listRecordingObjects(props.recording.id)
    if (objects.length === 0) {
      term.writeln('\x1b[33m[回放] 该会话暂无录像切片（可能仍在网关上传中）\x1b[0m')
      state.value = 'done'
      return
    }
    term.writeln(`\x1b[90m[回放] ${props.recording.user} @ ${props.recording.asset} · ${objects.length} 个切片\x1b[0m`)
    state.value = 'playing'
    for (let i = 0; i < objects.length && !aborted; i++) {
      const res = await fetch(objects[i].url)
      if (!res.ok) {
        throw new Error(`切片 ${i} 拉取失败（HTTP ${res.status}）`)
      }
      const buf = await res.arrayBuffer()
      term.write(new Uint8Array(buf))
      progress.value = Math.round(((i + 1) / objects.length) * 100)
    }
    if (!aborted) {
      term.writeln('\r\n\x1b[90m[回放] 录像播放完毕\x1b[0m')
      state.value = 'done'
    }
  } catch (error) {
    state.value = 'error'
    Message.error(error instanceof Error ? error.message : '录像拉取失败')
  }
})

onBeforeUnmount(() => {
  aborted = true
  term?.dispose()
  term = null
})
</script>

<template>
  <div class="recording-player">
    <div class="player-status" :data-state="state">
      <template v-if="state === 'loading'">正在获取录像切片…</template>
      <template v-else-if="state === 'playing'">回放中 · 已加载 {{ progress }}%</template>
      <template v-else-if="state === 'done'">回放完成</template>
      <template v-else>回放出错</template>
    </div>
    <div ref="container" class="player-terminal" />
  </div>
</template>

<style scoped>
.recording-player {
  display: flex;
  flex-direction: column;
  height: 60vh;
  min-height: 360px;
}

.player-status {
  padding: 4px 12px;
  font-size: 12px;
  color: #9ca3af;
  background: #1d2026;
  border-bottom: 1px solid #2a2d34;
}

.player-status[data-state='done'] {
  color: #4ade80;
}

.player-status[data-state='error'] {
  color: #f87171;
}

.player-terminal {
  flex: 1 1 auto;
  min-height: 0;
  padding: 8px;
  background: #16181d;
}
</style>
