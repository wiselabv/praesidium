<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { disconnectSession, listSessions } from '../../../api/sessions'
import type { SessionItem } from '../../../api/sessions'

/**
 * 在线会话页（L4）。
 *
 * 会话模块之一：当前正在进行的运维会话实时监控。
 * 实时监控（共享屏幕）依赖 Rust 网关的会话代理能力，为后续里程碑；先提供列表与强制断开。
 * 数据源 GET /api/sessions?status=online、POST /api/sessions/{id}/disconnect。
 */

const PROTOCOL_COLORS: Record<string, string> = {
  SSH: 'green',
  RDP: 'arcoblue',
  VNC: 'purple',
  MySQL: 'orange',
  PostgreSQL: 'cyan',
  Redis: 'magenta',
  HTTP: 'gold',
  HTTPS: 'arcoblue',
}

/** 列表状态 */
const loading = ref(false)
const sessions = ref<SessionItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await listSessions({
      page: page.value,
      size: pageSize.value,
      status: 'online',
      keyword: keyword.value.trim() || undefined,
    })
    sessions.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载在线会话失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handlePageChange(next: number) {
  page.value = next
  loadList()
}

function handlePageSizeChange(next: number) {
  pageSize.value = next
  page.value = 1
  loadList()
}

onMounted(loadList)

/** 时长格式化：秒 → x 小时 y 分钟 */
function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest > 0 ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

/** 实时监控（占位，依赖网关会话代理，后续里程碑接入） */
function handleMonitor(session: SessionItem) {
  Message.info(`「${session.asset}」的实时监控将在网关会话代理接入后开放`)
}

/** 强制断开（真实调后端，会话落 ended 并生成审计记录） */
function handleTerminate(session: SessionItem) {
  Modal.confirm({
    title: '强制断开',
    content: `确认强制断开 ${session.user} 在 ${session.asset}（${session.account}）的 ${session.protocol} 会话？该会话将立即终止并生成审计记录。`,
    okText: '断开',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await disconnectSession(session.id)
        Message.success(`会话 #${session.id} 已强制断开`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '断开失败，请重试')
      }
    },
  })
}
</script>

<template>
  <div class="sessions-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">在线会话</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索用户 / 资产"
          allow-clear
          class="page-search"
          @search="handleSearch"
        />
      </div>
      <div class="page-actions">
        <a-button @click="loadList">
          <template #icon><icon-refresh /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计条：在线数 -->
    <div class="stat-bar">
      <div class="stat-item">
        <span class="stat-value">{{ total }}</span>
        <span class="stat-label">当前在线会话</span>
      </div>
    </div>

    <!-- 会话表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="sessions"
        :loading="loading"
        :pagination="{
          current: page,
          pageSize,
          total,
          showTotal: true,
          showPageSize: true,
          showJumper: true,
        }"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #columns>
          <!-- 除操作列外均不设固定宽：列宽随屏幕宽度自适应分配 -->
          <a-table-column title="会话 ID" data-index="id">
            <template #cell="{ record }">
              <span class="session-id">#{{ record.id }}</span>
            </template>
          </a-table-column>
          <a-table-column title="用户" data-index="user" />
          <a-table-column title="资产" data-index="asset" />
          <a-table-column title="账号" data-index="account" />
          <a-table-column title="协议">
            <template #cell="{ record }">
              <a-tag size="small" :color="PROTOCOL_COLORS[record.protocol as string] ?? 'gray'">
                {{ record.protocol }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="来源 IP" data-index="sourceIp" />
          <a-table-column title="开始时间">
            <template #cell="{ record }">
              <span class="time-cell">{{ formatTime(record.startedAt) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="时长">
            <template #cell="{ record }">
              <span class="duration-cell">{{ formatDuration(record.durationSeconds) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="160">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link @click="handleMonitor(record)">监控</a-link>
                <a-link status="danger" @click="handleTerminate(record)">断开</a-link>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>
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

/* 页头左侧：标题 + 搜索并排 */
.page-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
}

.page-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
}

.page-search {
  width: 240px;
}

/* 统计条 */
.stat-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
  margin-bottom: var(--spacing-card);
  padding: var(--spacing-card);
  background: var(--color-card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}

.table-card {
  box-shadow: var(--shadow-card);
}

/* 会话 ID */
.session-id {
  color: var(--color-text-2);
  font-family: var(--font-family-mono);
}

/* 时长 / 时间 */
.duration-cell,
.time-cell {
  color: var(--color-text-2);
}
</style>
