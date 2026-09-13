<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { listLoginLogs } from '../../../api/logs'
import type { LoginLogItem } from '../../../api/logs'

/**
 * 登录日志页（L4）。
 *
 * 审计模块之二：登录行为审计（成功/失败/登录方式/来源），支撑异常登录研判。
 * 登录风险引擎（异地登录检测、IP 信誉库）为后续里程碑；先提供日志检索与详情。
 * 数据源 GET /api/logs/login（登录埋点由后端事件监听自动落库）。
 */

/** 登录方式（后端埋点值）→ 展示名与 tag 色 */
const METHOD_LABELS: Record<string, { label: string; color: string }> = {
  password: { label: '密码登录', color: 'gray' },
  mfa: { label: '密码 + MFA', color: 'green' },
  code: { label: '验证码登录', color: 'arcoblue' },
  domain: { label: '域账号登录', color: 'cyan' },
  recovery: { label: '恢复码登录', color: 'orange' },
  sso: { label: 'SSO 登录', color: 'purple' },
}

/** 列表状态 */
const loading = ref(false)
const logs = ref<LoginLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterResult = ref<'all' | 'success' | 'failed'>('all')

async function loadList() {
  loading.value = true
  try {
    const data = await listLoginLogs({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      result: filterResult.value === 'all' ? undefined : filterResult.value,
    })
    logs.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载登录日志失败')
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

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

/** 详情弹窗 */
const detailVisible = ref(false)
const viewing = ref<LoginLogItem | null>(null)

function openDetail(log: LoginLogItem) {
  viewing.value = log
  detailVisible.value = true
}

/** 导出检索结果（占位，导出文件接口后续里程碑接入） */
function handleExport() {
  Message.info(`导出当前 ${total.value} 条登录日志的导出功能将在文件服务接入后开放`)
}
</script>

<template>
  <div class="loginlogs-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">登录日志</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索用户 / 来源 IP"
          allow-clear
          class="page-search"
          @search="handleSearch"
        />
      </div>
      <div class="page-actions">
        <a-button @click="handleExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </div>
    </div>

    <!-- 筛选栏：结果 -->
    <div class="filter-bar">
      <a-radio-group v-model="filterResult" type="button" size="small" @change="handleSearch">
        <a-radio value="all">全部结果</a-radio>
        <a-radio value="success">成功</a-radio>
        <a-radio value="failed">失败</a-radio>
      </a-radio-group>
      <span class="filter-count">共 {{ total }} 条</span>
    </div>

    <!-- 登录日志表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="logs"
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
          <a-table-column title="时间">
            <template #cell="{ record }">
              <span class="time-cell">{{ formatTime(record.time) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="用户" data-index="user" />
          <a-table-column title="来源 IP" data-index="sourceIp" />
          <a-table-column title="登录方式">
            <template #cell="{ record }">
              <a-tag size="small" :color="METHOD_LABELS[record.method]?.color ?? 'gray'">
                {{ METHOD_LABELS[record.method]?.label ?? record.method }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="结果">
            <template #cell="{ record }">
              <a-tag size="small" :color="record.result === 'success' ? 'green' : 'red'">
                {{ record.result === 'success' ? '成功' : '失败' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="失败原因">
            <template #cell="{ record }">
              <span :class="record.result === 'failed' ? 'reason-failed' : 'reason-none'">
                {{ record.reason ?? '—' }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="80">
            <template #cell="{ record }">
              <a-link @click="openDetail(record)">详情</a-link>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:visible="detailVisible" title="登录日志详情" :footer="false">
      <a-descriptions v-if="viewing" :column="1" bordered size="medium">
        <a-descriptions-item label="用户">{{ viewing.user }}</a-descriptions-item>
        <a-descriptions-item label="时间">{{ formatTime(viewing.time) }}</a-descriptions-item>
        <a-descriptions-item label="来源 IP">{{ viewing.sourceIp }}</a-descriptions-item>
        <a-descriptions-item label="登录方式">
          {{ METHOD_LABELS[viewing.method]?.label ?? viewing.method }}
        </a-descriptions-item>
        <a-descriptions-item label="结果">
          {{ viewing.result === 'success' ? '成功' : '失败' }}
        </a-descriptions-item>
        <a-descriptions-item label="失败原因">{{ viewing.reason ?? '—' }}</a-descriptions-item>
        <a-descriptions-item label="客户端标识">{{ viewing.client ?? '—' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
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

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
  margin-bottom: var(--spacing-row);
}

.filter-count {
  margin-left: auto;
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}

.table-card {
  box-shadow: var(--shadow-card);
}

/* 单元格 */
.time-cell {
  color: var(--color-text-2);
}

.reason-failed {
  color: var(--color-danger);
}

.reason-none {
  color: var(--color-text-4);
}
</style>
