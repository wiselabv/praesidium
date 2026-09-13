<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useRouter } from 'vue-router'
import SessionTrendChart from '../components/SessionTrendChart.vue'
import ProtocolPieChart from '../components/ProtocolPieChart.vue'
import {
  fetchLoginTrend,
  fetchOnlineSessions,
  fetchOverview,
  fetchProtocols,
  fetchRecentOperations,
  fetchSessionTrend,
} from '../../../api/dashboard'
import type { DashboardOverview, NameCount, TrendPoint } from '../../../api/dashboard'
import type { OperationLogItem } from '../../../api/logs'
import type { SessionItem } from '../../../api/sessions'

const router = useRouter()

/**
 * 总览仪表盘（L4）。
 *
 * 结构：6 指标卡 → 图表区（会话趋势 + 登录趋势 + 协议分布环形）
 * → 动态区（最近操作 + 在线会话）。
 * 数据源 /api/dashboard/*（7 个统计接口），onMounted 并行加载。
 */

/** 指标卡数据（overview 接口驱动） */
interface Metric {
  label: string
  value: number
  hint: string
  tone: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  icon: string
}

const overview = ref<DashboardOverview | null>(null)
const loading = ref(false)

const metrics = computed<Metric[]>(() => {
  const data = overview.value
  if (!data) return []
  return [
    { label: '资产总数', value: data.totalAssets, hint: '纳管资产', tone: 'primary', icon: 'icon-desktop' },
    { label: '用户总数', value: data.totalUsers, hint: '本地账号', tone: 'info', icon: 'icon-user-group' },
    { label: '在线会话', value: data.onlineSessions, hint: '当前在线', tone: 'success', icon: 'icon-live-broadcast' },
    { label: '今日会话', value: data.todaySessions, hint: '当日发起', tone: 'info', icon: 'icon-file' },
    { label: '今日登录', value: data.todayLogins, hint: '成功登录', tone: 'success', icon: 'icon-check-circle' },
    { label: '今日失败登录', value: data.todayFailedLogins, hint: '安全关注', tone: 'danger', icon: 'icon-exclamation-circle' },
  ]
})

const TONE_COLORS: Record<Metric['tone'], string> = {
  primary: 'var(--color-primary)',
  success: 'var(--color-success)',
  warning: 'var(--color-warning)',
  danger: 'var(--color-danger)',
  info: 'var(--color-info)',
}

/** 会话趋势（近 7 天） */
const trendLabels = ref<string[]>([])
const trendSeries = ref<{ name: string; values: number[]; tone: 'primary' | 'danger' }[]>([])

function mapTrend(points: TrendPoint[]): { labels: string[]; values: number[] } {
  return {
    labels: points.map((point) => point.date.slice(5)),
    values: points.map((point) => point.count),
  }
}

/** 协议分布（NameCount → 环形图 { name, value }） */
const protocolDist = ref<NameCount[]>([])

const protocolSlices = computed(() =>
  protocolDist.value.map((item) => ({ name: item.name, value: item.count })),
)

/** 最近操作 */
const recentOperations = ref<OperationLogItem[]>([])

/** 在线会话 */
const onlineSessions = ref<SessionItem[]>([])

const RISK_LABELS: Record<string, { label: string; color: string }> = {
  high: { label: '高危', color: 'red' },
  medium: { label: '中危', color: 'orange' },
  low: { label: '低危', color: 'green' },
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest > 0 ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}

async function loadAll() {
  loading.value = true
  try {
    const [overviewData, sessionTrendData, loginTrendData, protocols, operations, sessions] =
      await Promise.all([
        fetchOverview(),
        fetchSessionTrend(7),
        fetchLoginTrend(7),
        fetchProtocols(),
        fetchRecentOperations(10),
        fetchOnlineSessions(5),
      ])
    overview.value = overviewData
    const mapped = mapTrend(sessionTrendData)
    trendLabels.value = mapped.labels
    trendSeries.value = [
      { name: '会话量', values: mapped.values, tone: 'primary' },
      {
        name: '失败登录',
        values: loginTrendData.map((point) => point.failed),
        tone: 'danger',
      },
    ]
    protocolDist.value = protocols
    recentOperations.value = operations
    onlineSessions.value = sessions
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载总览数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

/** 快速入口 */
function handleStartSession() {
  router.push('/terminal')
}

function handleCreateHost() {
  router.push('/assets/inventory')
}

function handleGoPolicies() {
  router.push('/access/policies')
}
</script>

<template>
  <div class="dashboard">
    <!-- 页标题区：大标题 + 快速入口操作组 -->
    <div class="page-header">
      <h2 class="page-title">总览</h2>
      <div class="page-actions">
        <a-button @click="handleStartSession">
          <template #icon><icon-live-broadcast /></template>
          发起会话
        </a-button>
        <a-button @click="handleGoPolicies">
          <template #icon><icon-user-add /></template>
          授权策略
        </a-button>
        <a-button type="primary" @click="handleCreateHost">
          <template #icon><icon-plus /></template>
          新建资产
        </a-button>
      </div>
    </div>

    <a-spin :loading="loading" class="dashboard-spin">
      <!-- 指标卡（24 栅格，4 格/个，窄屏自动降档） -->
      <a-row :gutter="12" class="metric-row">
        <a-col v-for="metric in metrics" :key="metric.label" :xs="12" :md="8" :xl="4">
          <a-card :bordered="false" class="metric-card">
            <div class="metric-top">
              <span class="metric-icon" :style="{ color: TONE_COLORS[metric.tone] }">
                <component :is="metric.icon" />
              </span>
              <span class="metric-label">{{ metric.label }}</span>
            </div>
            <div class="metric-value">{{ metric.value }}</div>
            <div class="metric-trend">{{ metric.hint }}</div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 图表区：会话/登录趋势（14 格）+ 协议分布（10 格） -->
      <a-row :gutter="12" class="detail-row">
        <a-col :xs="24" :xl="14">
          <a-card :bordered="false" class="panel-card" title="会话与登录趋势（近 7 天）">
            <SessionTrendChart :labels="trendLabels" :series="trendSeries" />
          </a-card>
        </a-col>
        <a-col :xs="24" :xl="10">
          <a-card :bordered="false" class="panel-card" title="资产协议分布">
            <ProtocolPieChart :slices="protocolSlices" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 动态区：最近操作 + 在线会话 -->
      <a-row :gutter="12" class="detail-row">
        <a-col :xs="24" :lg="12" :xl="12">
          <a-card :bordered="false" class="panel-card" title="最近操作">
            <div class="session-item" v-for="log in recentOperations" :key="log.id">
              <span class="session-time">{{ formatTime(log.time) }}</span>
              <span class="session-user">{{ log.user }}</span>
              <span class="session-asset">{{ log.asset }} · {{ log.action }}</span>
              <a-tag size="small" :color="RISK_LABELS[log.risk]?.color ?? 'gray'">
                {{ RISK_LABELS[log.risk]?.label ?? log.risk }}
              </a-tag>
            </div>
            <a-empty v-if="recentOperations.length === 0" description="暂无操作记录" />
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="12" :xl="12">
          <a-card :bordered="false" class="panel-card" title="在线会话">
            <div class="session-item" v-for="session in onlineSessions" :key="session.id">
              <span class="session-time">{{ formatTime(session.startedAt) }}</span>
              <span class="session-user">{{ session.user }}</span>
              <span class="session-asset">{{ session.asset }}（{{ session.account }}）</span>
              <a-tag size="small" color="green">{{ session.protocol }}</a-tag>
              <span class="session-duration">{{ formatDuration(session.durationSeconds) }}</span>
            </div>
            <a-empty v-if="onlineSessions.length === 0" description="暂无在线会话" />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<style scoped>
/* 页标题区（全站标准页面结构第 1 层） */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-card);
}

.page-title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

/* 快速入口操作组 */
.page-actions {
  display: flex;
  gap: var(--spacing-row);
}

.metric-row,
.detail-row {
  margin-bottom: var(--spacing-card);
}

.metric-card,
.panel-card {
  box-shadow: var(--shadow-card);
}

/* 指标卡：图标 + 标签一行，数值大字号，提示按功能色 */
.metric-top {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  background: var(--primary-1);
  font-size: var(--size-icon-md);
}

.metric-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}

.metric-value {
  margin-top: var(--spacing-row);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
  line-height: var(--line-height-tight);
}

.metric-trend {
  margin-top: var(--spacing-row);
  font-size: var(--font-size-xs);
  color: var(--color-text-4);
}

/* 最近操作 / 在线会话列表行 */
.session-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-row) 0;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
}

.session-item:last-child {
  border-bottom: none;
}

.session-time {
  color: var(--color-text-3);
}

.session-user {
  color: var(--color-primary);
}

.session-asset {
  flex: 1;
  color: var(--color-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-duration {
  color: var(--color-text-3);
  font-size: var(--font-size-xs);
  flex-shrink: 0;
}

.dashboard-spin {
  display: block;
}
</style>
