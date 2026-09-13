<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import XtermTerminal from '../../../components/XtermTerminal.vue'
import { useAuthStore } from '../../../stores/auth'
import { connectSession, disconnectSession } from '../../../api/sessions'
import type { SessionItem } from '../../../api/sessions'
import { listAssetAccounts, listAssets } from '../../../api/assets'
import type { AssetItem } from '../../../api/assets'
import type { AssetAccountItem } from '../../../api/accounts'

/**
 * Web 终端工作台（L4）。
 *
 * 会话模块入口：选择资产 → 账号 → 协议 → 发起连接 → 全屏终端交互。
 * 资产/账号下拉与连接/断开已接真实接口（POST /api/sessions/connect 创建在线会话、
 * POST /api/sessions/{id}/disconnect 断开）；终端区在 SSH 网关代理就绪前保持 mock 回显模式。
 */

const auth = useAuthStore()
const route = useRoute()

const PROTOCOL_OPTIONS = ['SSH', 'RDP', 'VNC', 'MySQL', 'Redis']

/** 资产清单（演示规模一次性拉取，选项含地址便于区分） */
const assetOptions = ref<AssetItem[]>([])
const assetsLoading = ref(false)
/** 当前资产下的账号 */
const accountOptions = ref<AssetAccountItem[]>([])
const accountsLoading = ref(false)

/** 会话发起表单 */
const form = ref({
  assetId: undefined as number | undefined,
  accountId: undefined as number | undefined,
  protocol: 'SSH',
})

async function loadAssets() {
  assetsLoading.value = true
  try {
    const data = await listAssets({ page: 1, size: 200 })
    assetOptions.value = data.items
    // 资产清单就绪后处理入口 query 预选
    applyQueryAsset()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载资产清单失败')
  } finally {
    assetsLoading.value = false
  }
}

/** 从资产列表等入口带 query 预选资产（按资产名匹配） */
async function applyQueryAsset() {
  const name = route.query.asset
  if (typeof name !== 'string') return
  const target = assetOptions.value.find((item) => item.name === name)
  if (!target) return
  await handleAssetChange(target.id)
  form.value.assetId = target.id
  form.value.accountId = accountOptions.value[0]?.id
}

watch(() => route.query.asset, applyQueryAsset)

/** 选择资产后：拉取该资产账号并预选协议 */
async function handleAssetChange(value: unknown) {
  const assetId = typeof value === 'number' ? value : undefined
  form.value.accountId = undefined
  accountOptions.value = []
  if (assetId === undefined) return
  const asset = assetOptions.value.find((item) => item.id === assetId)
  if (asset && PROTOCOL_OPTIONS.includes(asset.protocol)) {
    form.value.protocol = asset.protocol
  }
  accountsLoading.value = true
  try {
    accountOptions.value = await listAssetAccounts(assetId)
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载账号清单失败')
  } finally {
    accountsLoading.value = false
  }
}

onMounted(loadAssets)

/** 会话状态：idle（未发起）/ connecting（连接中）/ connected */
const sessionState = ref<'idle' | 'connecting' | 'connected'>('idle')
/** 当前会话信息 */
const session = ref<SessionItem | null>(null)
const elapsed = ref(0)
let timer: number | null = null

/** 发起连接（真实创建在线会话，终端仍为 mock 回显） */
async function handleConnect() {
  if (form.value.assetId === undefined) {
    Message.warning('请选择资产')
    return
  }
  sessionState.value = 'connecting'
  try {
    const created = await connectSession({
      assetId: form.value.assetId,
      accountId: form.value.accountId ?? null,
      protocol: form.value.protocol,
    })
    session.value = created
    sessionState.value = 'connected'
    elapsed.value = 0
    timer = window.setInterval(() => {
      elapsed.value += 1
    }, 1000)
    Message.success(`已连接 ${created.asset}（${created.account} · ${created.protocol}）`)
  } catch (error) {
    sessionState.value = 'idle'
    Message.error(error instanceof Error ? error.message : '发起会话失败')
  }
}

/** 断开会话（真实断开后端在线会话记录） */
async function handleDisconnect() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
  const current = session.value
  session.value = null
  sessionState.value = 'idle'
  if (current) {
    try {
      await disconnectSession(current.id)
      Message.success('会话已断开')
    } catch (error) {
      Message.error(error instanceof Error ? error.message : '会话断开失败，请到在线会话页重试')
    }
  }
}

/** 终端 mock 退出（输入 exit）时复位 */
function handleTerminalExit() {
  handleDisconnect()
}

/** 时长格式化 mm:ss / hh:mm:ss */
function formatElapsed(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
}

// 开发环境经 Vite 代理（/ws → Rust 核心代理）；生产环境通过 VITE_WS_BASE 指定网关。
const wsBase =
  import.meta.env.VITE_WS_BASE ||
  `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}`
const wsUrl = `${wsBase}/ws/terminal`

onBeforeUnmount(() => {
  if (timer !== null) window.clearInterval(timer)
})
</script>

<template>
  <div class="terminal-view">
    <!-- 会话发起区 / 会话信息条 -->
    <header class="terminal-view__header">
      <template v-if="sessionState !== 'connected'">
        <strong>Web 终端</strong>
        <a-select
          v-model="form.assetId"
          placeholder="选择资产"
          class="header-select"
          :loading="assetsLoading"
          :disabled="sessionState === 'connecting'"
          @change="handleAssetChange"
        >
          <a-option v-for="a in assetOptions" :key="a.id" :value="a.id">
            {{ a.name }}（{{ a.address }}）
          </a-option>
        </a-select>
        <a-select
          v-model="form.accountId"
          placeholder="选择账号"
          class="header-select header-select--small"
          :loading="accountsLoading"
          :disabled="sessionState === 'connecting' || form.assetId === undefined"
          allow-clear
        >
          <a-option v-for="a in accountOptions" :key="a.id" :value="a.id">{{ a.name }}</a-option>
        </a-select>
        <a-select
          v-model="form.protocol"
          class="header-select header-select--small"
          :disabled="sessionState === 'connecting'"
        >
          <a-option v-for="p in PROTOCOL_OPTIONS" :key="p" :value="p">{{ p }}</a-option>
        </a-select>
        <a-button
          type="primary"
          size="small"
          :loading="sessionState === 'connecting'"
          @click="handleConnect"
        >
          <template #icon><icon-poweroff /></template>
          连接
        </a-button>
        <span v-if="sessionState === 'connecting'" class="header-hint">正在建立会话…</span>
        <span v-else class="header-hint">选择资产与账号发起会话</span>
      </template>
      <template v-else>
        <strong>Web 终端</strong>
        <a-tag size="small" color="green">{{ session?.asset }}</a-tag>
        <span class="session-info">{{ session?.account }} · {{ session?.protocol }}</span>
        <span class="header-hint">已连接 · 时长 {{ formatElapsed(elapsed) }}</span>
        <span class="header-spacer" />
        <a-button size="small" status="danger" @click="handleDisconnect">
          <template #icon><icon-poweroff /></template>
          断开
        </a-button>
      </template>
    </header>

    <!-- 终端区：连接前空态，连接后全屏终端 -->
    <div class="terminal-view__body">
      <div v-if="sessionState !== 'connected'" class="terminal-empty">
        <icon-code-square class="empty-icon" />
        <p class="empty-title">尚未建立会话</p>
        <p class="empty-tip">在上方选择目标资产与账号后点击「连接」发起会话</p>
        <p class="empty-tip">（SSH 网关代理开发中，当前为 mock 回显模式）</p>
      </div>
      <XtermTerminal
        v-else
        :key="session ? `${session.id}-live` : 'term'"
        :ws-url="wsUrl"
        :token="auth.accessToken"
        :mock="true"
        :mock-user="session?.account"
        :mock-host="session?.asset"
        @exit="handleTerminalExit"
      />
    </div>
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
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-row) var(--spacing-page);
  background: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.header-select {
  width: 200px;
}

.header-select--small {
  width: 150px;
}

.header-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

.header-spacer {
  flex: 1;
}

.session-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-2);
}

.terminal-view__body {
  flex: 1 1 auto;
  min-height: 0;
  position: relative;
}

/* 未连接空态 */
.terminal-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-row);
  background: var(--color-card-bg);
}

.empty-icon {
  font-size: 48px;
  color: var(--color-text-4);
}

.empty-title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-2);
}

.empty-tip {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}
</style>
