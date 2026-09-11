<script setup lang="ts">
import XtermTerminal from '../../../components/XtermTerminal.vue'
import { useAuthStore } from '../../../stores/auth'

const auth = useAuthStore()

// 开发环境经 Vite 代理（/ws → Rust 核心代理）；生产环境通过 VITE_WS_BASE 指定网关。
const wsBase =
  import.meta.env.VITE_WS_BASE ||
  `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}`
const wsUrl = `${wsBase}/ws/terminal`
</script>

<template>
  <div class="terminal-view">
    <header class="terminal-view__header">
      <strong>Praesidium</strong>
      <span>终端 · 占位回显模式（SSH 代理开发中）</span>
    </header>
    <XtermTerminal :ws-url="wsUrl" :token="auth.accessToken" />
  </div>
</template>

<style scoped>
.terminal-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.terminal-view__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid #2a2d34;
}

.terminal-view__header span {
  font-size: 12px;
  color: #9ca3af;
}
</style>
