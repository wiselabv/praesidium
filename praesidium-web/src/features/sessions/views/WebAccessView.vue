<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { listAssets } from '../../../api/assets'
import type { AssetItem } from '../../../api/assets'
import { connectSession, disconnectSession } from '../../../api/sessions'

/**
 * Web 资产访问页（L4）。
 *
 * Web 站点资产（HTTP/HTTPS）的两种访问通道：
 * - RemoteApp 浏览器：堡垒机受控环境内运行浏览器，用户浏览器端仅接收渲染画面，
 *   凭据不下发客户端，会话全程可录屏审计（真实实现经 RDP/Guacamole 网关推流）。
 * - HTTP 代理：用户浏览器经堡垒机反向代理访问目标站点（真实实现为网关反向代理 + SSO 注入）。
 *
 * 资产信息与连接/断开已接真实接口（/api/assets 定位资产、/api/sessions/connect 创建会话、
 * /api/sessions/{id}/disconnect 断开）；网关画面/代理通道未就绪前保持模拟渲染。
 */

type AccessMethod = 'remoteapp' | 'proxy'

const route = useRoute()
const router = useRouter()

const asset = computed(() => (typeof route.query.asset === 'string' ? route.query.asset : ''))
const account = computed(() => (typeof route.query.account === 'string' ? route.query.account : ''))
const method = computed<AccessMethod>(() => (route.query.method === 'proxy' ? 'proxy' : 'remoteapp'))

/** 按名字定位资产（演示规模全量拉取后前端匹配） */
const assetItem = ref<AssetItem | null>(null)
const site = computed(() =>
  assetItem.value
    ? { url: `${assetItem.value.protocol === 'HTTP' ? 'http' : 'https'}://${assetItem.value.address}`, title: assetItem.value.name }
    : null,
)

/** 后端会话记录（connect 创建 / disconnect 关闭） */
const sessionId = ref<number | null>(null)

/** 会话计时 */
const elapsed = ref(0)
let timer: number | null = null

/** RemoteApp 会话状态：connecting（建立会话）→ ready（画面就绪） */
const remoteState = ref<'connecting' | 'ready'>('connecting')
/** 代理 iframe 加载状态 */
const proxyState = ref<'loading' | 'ready'>('loading')

function formatElapsed(seconds: number): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
}

/** 任务栏时间（随 elapsed 每秒刷新） */
const taskbarTime = computed(() => {
  void elapsed.value
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

/** 断开访问：停止计时、关闭后端会话记录并返回资产列表 */
async function handleDisconnect() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
  if (sessionId.value !== null) {
    try {
      await disconnectSession(sessionId.value)
      Message.success(`已结束 ${asset.value} 的 Web 访问会话`)
    } catch (error) {
      Message.error(error instanceof Error ? error.message : '会话断开失败')
    }
  }
  router.push('/assets/inventory')
}

onMounted(async () => {
  timer = window.setInterval(() => {
    elapsed.value += 1
  }, 1000)
  // 定位资产并创建后端会话记录
  try {
    const data = await listAssets({ page: 1, size: 200 })
    const target = data.items.find((item) => item.name === asset.value)
    if (!target) return
    assetItem.value = target
    const created = await connectSession({
      assetId: target.id,
      accountId: null,
      protocol: target.protocol,
    })
    sessionId.value = created.session.id
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '创建访问会话失败')
  }
  if (method.value === 'remoteapp') {
    // 模拟 RemoteApp 会话建立耗时
    window.setTimeout(() => {
      remoteState.value = 'ready'
    }, 1200)
  }
})

onBeforeUnmount(() => {
  if (timer !== null) window.clearInterval(timer)
})
</script>

<template>
  <div class="web-access">
    <!-- 会话信息条 -->
    <div class="session-bar">
      <a-button size="small" @click="handleDisconnect">
        <template #icon><icon-left /></template>
        返回
      </a-button>
      <strong>Web 访问</strong>
      <a-tag size="small" color="green">{{ asset }}</a-tag>
      <span class="session-info">
        {{ account }} · {{ method === 'remoteapp' ? 'RemoteApp 浏览器' : 'HTTP 代理' }}
      </span>
      <span class="session-hint">已连接 · 时长 {{ formatElapsed(elapsed) }}</span>
      <span class="session-spacer" />
      <a-button size="small" status="danger" @click="handleDisconnect">
        <template #icon><icon-poweroff /></template>
        断开
      </a-button>
    </div>

    <!-- 未知资产容错 -->
    <div v-if="!site" class="web-access__empty">
      <icon-desktop class="empty-icon" />
      <p>未找到 Web 资产「{{ asset }}」，请从资产列表发起连接。</p>
      <a-button type="primary" @click="router.push('/assets/inventory')">返回资产列表</a-button>
    </div>

    <!-- RemoteApp 视图 -->
    <template v-else-if="method === 'remoteapp'">
      <div v-if="remoteState === 'connecting'" class="remote-loading">
        <a-spin :size="24" />
        <p>正在建立 RemoteApp 会话，请稍候…</p>
      </div>
      <div v-else class="remote-desktop">
        <div class="desktop-window">
          <div class="window-titlebar">
            <span class="window-dot window-dot--red" />
            <span class="window-dot window-dot--yellow" />
            <span class="window-dot window-dot--green" />
            <span class="window-title">{{ site.title }} — 远程浏览器</span>
          </div>
          <div class="window-toolbar">
            <span class="toolbar-btn">←</span>
            <span class="toolbar-btn">→</span>
            <span class="toolbar-btn">⟳</span>
            <span class="address-bar">{{ site.url }}</span>
          </div>
          <div class="window-content">
            <div class="site-nav">
              <span class="site-logo">{{ site.title[0] }}</span>
              <span class="site-name">{{ site.title }}</span>
              <span class="site-nav-item">项目</span>
              <span class="site-nav-item">文档</span>
              <span class="site-nav-item">设置</span>
            </div>
            <div class="site-hero">
              <h3>{{ site.title }}</h3>
              <p>内部 Web 站点 · 经堡垒机 RemoteApp 远程浏览器访问</p>
              <p class="site-hero-sub">凭据不下发客户端 · 会话全程录屏审计</p>
            </div>
            <div class="site-cards">
              <div v-for="n in 3" :key="n" class="site-card">
                <span class="card-line card-line--wide" />
                <span class="card-line" />
                <span class="card-line card-line--short" />
              </div>
            </div>
          </div>
        </div>
        <div class="desktop-taskbar">
          <span class="taskbar-start">开始</span>
          <span class="taskbar-time">{{ taskbarTime }}</span>
        </div>
        <p class="remote-watermark">
          RemoteApp 渲染预览（mock）· 真实实现经 RDP/Guacamole 网关推送远程浏览器画面
        </p>
      </div>
    </template>

    <!-- 代理视图 -->
    <div v-else class="proxy-view">
      <div class="proxy-notice">
        <icon-link />
        <span>网关未就绪：当前为直连模拟，正式环境经堡垒机反向代理访问并全程审计（mock）</span>
      </div>
      <div class="proxy-frame-wrap">
        <iframe :src="site.url" class="proxy-frame" @load="proxyState = 'ready'" />
        <div v-if="proxyState === 'loading'" class="proxy-loading">
          <a-spin :size="24" />
          <p>正在通过代理加载 {{ site.url }}…</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.web-access {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/* ---------- 会话信息条 ---------- */
.session-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-card);
  margin-bottom: var(--spacing-page);
  background: var(--color-card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}

.session-info {
  color: var(--color-text-2);
  font-size: var(--font-size-sm);
}

.session-hint {
  color: var(--color-text-3);
  font-size: var(--font-size-sm);
}

.session-spacer {
  flex: 1;
}

/* ---------- 空态 ---------- */
.web-access__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-page);
  padding: 80px 0;
  color: var(--color-text-3);
}

.empty-icon {
  font-size: 48px;
  color: var(--color-text-4);
}

/* ---------- RemoteApp：连接中 ---------- */
.remote-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-page);
  flex: 1;
  min-height: 320px;
  color: var(--color-text-3);
}

/* ---------- RemoteApp：远程桌面模拟 ---------- */
.remote-desktop {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  padding: var(--spacing-xl);
  background: var(--color-brand-navy);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.desktop-window {
  width: min(880px, 100%);
  background: var(--color-card-bg);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-dropdown);
}

.window-titlebar {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-row) var(--spacing-card);
  background: var(--color-table-header);
  border-bottom: 1px solid var(--color-border);
}

.window-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-round);
}

.window-dot--red {
  background: var(--color-danger);
}

.window-dot--yellow {
  background: var(--color-warning);
}

.window-dot--green {
  background: var(--color-success);
}

.window-title {
  margin-left: var(--spacing-row);
  color: var(--color-text-2);
  font-size: var(--font-size-xs);
}

.window-toolbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-card);
  border-bottom: 1px solid var(--color-border);
}

.toolbar-btn {
  padding: 2px 8px;
  color: var(--color-text-3);
  font-size: var(--font-size-xs);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.address-bar {
  flex: 1;
  margin-left: var(--spacing-xs);
  padding: 4px var(--spacing-card);
  color: var(--color-text-3);
  font-size: var(--font-size-xs);
  font-family: var(--font-family-mono);
  background: var(--color-page-bg);
  border-radius: var(--radius-round);
}

.window-content {
  padding: var(--spacing-xl);
  min-height: 360px;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: var(--spacing-page);
  padding-bottom: var(--spacing-card);
  border-bottom: 1px solid var(--color-border);
}

.site-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--color-card-bg);
  font-size: var(--font-size-md);
  font-weight: 600;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
}

.site-name {
  color: var(--color-text-1);
  font-weight: 600;
}

.site-nav-item {
  color: var(--color-text-3);
  font-size: var(--font-size-sm);
}

.site-hero {
  padding: var(--spacing-xl) 0;
  text-align: center;
}

.site-hero h3 {
  margin: 0 0 var(--spacing-row);
  color: var(--color-text-1);
  font-size: var(--font-size-xl);
}

.site-hero p {
  margin: 0;
  color: var(--color-text-2);
  font-size: var(--font-size-sm);
}

.site-hero-sub {
  margin-top: var(--spacing-xs) !important;
  color: var(--color-text-3) !important;
  font-size: var(--font-size-xs) !important;
}

.site-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-card);
}

.site-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.card-line {
  height: 6px;
  background: var(--color-page-bg);
  border-radius: var(--radius-round);
}

.card-line--wide {
  width: 70%;
}

.card-line--short {
  width: 45%;
}

.desktop-taskbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(880px, 100%);
  margin-top: var(--spacing-card);
  padding: var(--spacing-xs) var(--spacing-card);
  color: var(--color-card-bg);
  font-size: var(--font-size-xs);
  background: var(--color-sidebar-bg);
  border-radius: var(--radius-md);
}

.remote-watermark {
  margin: var(--spacing-card) 0 0;
  color: var(--color-brand-text-soft);
  font-size: var(--font-size-xs);
}

/* ---------- 代理视图 ---------- */
.proxy-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.proxy-notice {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-row) var(--spacing-card);
  margin-bottom: var(--spacing-card);
  color: var(--color-text-2);
  font-size: var(--font-size-sm);
  background: var(--primary-1);
  border-radius: var(--radius-md);
}

.proxy-frame-wrap {
  position: relative;
  flex: 1;
  min-height: 420px;
  background: var(--color-card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.proxy-frame {
  width: 100%;
  height: 100%;
  min-height: 420px;
  border: none;
}

.proxy-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-page);
  color: var(--color-text-3);
  background: var(--color-card-bg);
}
</style>
