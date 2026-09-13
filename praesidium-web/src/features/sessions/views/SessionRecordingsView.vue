<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { listRecordings, listRecordingObjects } from '../../../api/sessions'
import type { RecordingItem } from '../../../api/sessions'
import RecordingPlayer from '../components/RecordingPlayer.vue'

/**
 * 录像回放页（L4）。
 *
 * 会话模块之二：已结束会话的操作录像（堡垒机合规审计的核心证据）。
 * 回放：拉取 MinIO 预签名切片 URL，按 0.cast → 1.cast … 顺序渲染进 xterm 只读终端；
 * 下载：顺序拉取全部切片合并为单个 .cast 文件触发浏览器保存。
 * 数据源 GET /api/sessions/recordings（后端只回已生成录像的 ended 会话）。
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
const recordings = ref<RecordingItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await listRecordings({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
    })
    recordings.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载录像列表失败')
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

/** 文件大小格式化 */
function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

/** 回放弹窗状态 */
const playerVisible = ref(false)
const playerRecording = ref<RecordingItem | null>(null)
const downloading = ref(false)

/** 回放：弹出播放器，逐片拉取预签名 URL 渲染 */
function handlePlay(recording: RecordingItem) {
  playerRecording.value = recording
  playerVisible.value = true
}

/** 下载：顺序拉取全部切片合并为单个 .cast 文件 */
async function handleDownload(recording: RecordingItem) {
  downloading.value = true
  try {
    const objects = await listRecordingObjects(recording.id)
    if (objects.length === 0) {
      Message.warning('该会话暂无录像切片')
      return
    }
    const chunks: BlobPart[] = []
    for (const object of objects) {
      const res = await fetch(object.url)
      if (!res.ok) throw new Error(`切片 ${object.key} 拉取失败（HTTP ${res.status}）`)
      chunks.push(await res.blob())
    }
    const blob = new Blob(chunks, { type: 'application/octet-stream' })
    const anchor = document.createElement('a')
    anchor.href = URL.createObjectURL(blob)
    anchor.download = `session-${recording.sessionId}-recording.cast`
    anchor.click()
    URL.revokeObjectURL(anchor.href)
    Message.success(`已下载 ${objects.length} 个切片（${formatSize(blob.size)}）`)
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '录像下载失败')
  } finally {
    downloading.value = false
  }
}

/** 删除录像（高危操作，二次确认） */
function handleDelete(recording: RecordingItem) {
  Modal.confirm({
    title: '删除录像',
    content: `确认删除录像 #${recording.id}（会话 #${recording.sessionId}）？录像删除后不可恢复，建议确认已满足审计保留要求。`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    onOk: () => {
      // TODO(会话): 后端录像删除接口接入后调 DELETE /api/sessions/recordings/{id}
      Message.info(`录像 #${recording.id} 的删除将在录像管理接口接入后开放`)
    },
  })
}
</script>

<template>
  <div class="recordings-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">录像回放</h2>
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

    <!-- 录像表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="recordings"
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
          <a-table-column title="录像 ID" data-index="id">
            <template #cell="{ record }">
              <span class="recording-id">#{{ record.id }}</span>
            </template>
          </a-table-column>
          <a-table-column title="会话 ID" data-index="sessionId" />
          <a-table-column title="用户" data-index="user" />
          <a-table-column title="资产" data-index="asset" />
          <a-table-column title="协议">
            <template #cell="{ record }">
              <a-tag size="small" :color="PROTOCOL_COLORS[record.protocol as string] ?? 'gray'">
                {{ record.protocol }}
              </a-tag>
            </template>
          </a-table-column>
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
          <a-table-column title="大小">
            <template #cell="{ record }">
              <span class="size-cell">{{ formatSize(record.sizeBytes) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="200">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link @click="handlePlay(record)">回放</a-link>
                <a-link :loading="downloading" @click="handleDownload(record)">下载</a-link>
                <a-link status="danger" @click="handleDelete(record)">删除</a-link>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 回放弹窗 -->
    <a-modal
      v-model:visible="playerVisible"
      :title="playerRecording ? `录像回放 #${playerRecording.id} · ${playerRecording.user} @ ${playerRecording.asset}` : '录像回放'"
      :width="960"
      :footer="false"
      :mask-closable="false"
      unmount-on-close
    >
      <RecordingPlayer v-if="playerVisible && playerRecording" :recording="playerRecording" />
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

.table-card {
  box-shadow: var(--shadow-card);
}

/* 录像 ID */
.recording-id {
  color: var(--color-text-2);
  font-family: var(--font-family-mono);
}

/* 时长 / 时间 / 大小 */
.duration-cell,
.time-cell,
.size-cell {
  color: var(--color-text-2);
}
</style>
