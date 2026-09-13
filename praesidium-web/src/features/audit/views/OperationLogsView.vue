<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { listOperationLogs } from '../../../api/logs'
import type { OperationLogItem } from '../../../api/logs'

/**
 * 操作日志页（L4）。
 *
 * 审计模块之一：运维操作审计（命令级/SQL 级），堡垒机合规审计的核心。
 * 命令过滤拦截与高危指令告警依赖网关拦截器，为后续里程碑；先提供日志检索与详情。
 * 数据源 GET /api/logs/operations（后端按 risk/result/keyword 服务端筛选分页）。
 */

type RiskLevel = 'high' | 'medium' | 'low'

const RISK_LABELS: Record<string, { label: string; color: string }> = {
  high: { label: '高危', color: 'red' },
  medium: { label: '中危', color: 'orange' },
  low: { label: '低危', color: 'green' },
}

const RESULT_LABELS: Record<string, { label: string; color: string }> = {
  success: { label: '成功', color: 'green' },
  blocked: { label: '已拦截', color: 'red' },
  failed: { label: '失败', color: 'orange' },
}

/** 列表状态（服务端分页） */
const loading = ref(false)
const logs = ref<OperationLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

/** 筛选状态 */
const filterRisk = ref<'all' | RiskLevel>('all')
const filterResult = ref<'all' | 'success' | 'blocked' | 'failed'>('all')
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await listOperationLogs({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      risk: filterRisk.value === 'all' ? undefined : filterRisk.value,
      result: filterResult.value === 'all' ? undefined : filterResult.value,
    })
    logs.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

/** 任一筛选条件变化 → 回到第 1 页重新拉取 */
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

/** 详情弹窗 */
const detailVisible = ref(false)
const viewing = ref<OperationLogItem | null>(null)

function openDetail(log: OperationLogItem) {
  viewing.value = log
  detailVisible.value = true
}

/** 导出检索结果（占位，导出文件接口后续里程碑接入） */
function handleExport() {
  Message.info(`导出当前 ${total.value} 条操作日志的导出功能将在文件服务接入后开放`)
}
</script>

<template>
  <div class="oplogs-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">操作日志</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索用户 / 资产 / 操作内容"
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

    <!-- 筛选栏：风险等级 + 结果 -->
    <div class="filter-bar">
      <a-radio-group v-model="filterRisk" type="button" size="small" @change="handleSearch">
        <a-radio value="all">全部风险</a-radio>
        <a-radio value="high">高危</a-radio>
        <a-radio value="medium">中危</a-radio>
        <a-radio value="low">低危</a-radio>
      </a-radio-group>
      <a-radio-group v-model="filterResult" type="button" size="small" @change="handleSearch">
        <a-radio value="all">全部结果</a-radio>
        <a-radio value="success">成功</a-radio>
        <a-radio value="blocked">已拦截</a-radio>
        <a-radio value="failed">失败</a-radio>
      </a-radio-group>
      <span class="filter-count">共 {{ total }} 条</span>
    </div>

    <!-- 操作日志表格 -->
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
          <!-- 除操作列外均不设固定宽：列宽随屏幕宽度自适应分配 -->
          <a-table-column title="时间" data-index="time" />
          <a-table-column title="用户" data-index="user" />
          <a-table-column title="资产" data-index="asset" />
          <a-table-column title="账号" data-index="account" />
          <a-table-column title="操作内容" data-index="action">
            <template #cell="{ record }">
              <span class="action-cell">{{ record.action }}</span>
            </template>
          </a-table-column>
          <a-table-column title="结果">
            <template #cell="{ record }">
              <a-tag size="small" :color="RESULT_LABELS[record.result]?.color ?? 'gray'">
                {{ RESULT_LABELS[record.result]?.label ?? record.result }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="风险">
            <template #cell="{ record }">
              <a-tag size="small" :color="RISK_LABELS[record.risk]?.color ?? 'gray'">
                {{ RISK_LABELS[record.risk]?.label ?? record.risk }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="90">
            <template #cell="{ record }">
              <a-link @click="openDetail(record)">详情</a-link>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 操作详情弹窗 -->
    <a-modal v-model:visible="detailVisible" title="操作详情" :footer="false" :width="520">
      <template v-if="viewing">
        <div class="detail-row">
          <span class="detail-label">操作时间</span>
          <span class="detail-value">{{ viewing.time }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">操作用户</span>
          <span class="detail-value">{{ viewing.user }}（{{ viewing.account }}）</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">目标资产</span>
          <span class="detail-value">{{ viewing.asset }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">来源 IP</span>
          <span class="detail-value">{{ viewing.sourceIp }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">风险等级</span>
          <a-tag size="small" :color="RISK_LABELS[viewing.risk]?.color ?? 'gray'">
            {{ RISK_LABELS[viewing.risk]?.label ?? viewing.risk }}
          </a-tag>
        </div>
        <div class="detail-block">
          <span class="detail-label">完整记录</span>
          <pre class="detail-code">{{ viewing.detail }}</pre>
        </div>
      </template>
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

/* 操作内容 */
.action-cell {
  color: var(--color-text-2);
}

/* 详情弹窗 */
.detail-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-row) 0;
}

.detail-label {
  width: 72px;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}

.detail-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-1);
}

.detail-block {
  padding: var(--spacing-row) 0;
}

.detail-code {
  margin: var(--spacing-row) 0 0;
  padding: var(--spacing-card);
  background: var(--color-table-header);
  border-radius: var(--radius-md);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  color: var(--color-text-2);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
