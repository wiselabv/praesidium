<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  createAsset,
  deleteAsset,
  listAssets,
  testAsset,
  updateAsset,
} from '../../../api/assets'
import type { AssetItem, AssetRequest } from '../../../api/assets'

/**
 * 资产列表页（L4）。
 *
 * 资产管理核心：资产 CRUD、连通性测试（TCP 探测 3 秒超时）、
 * 资产下账号清单（跳转账号页按资产过滤）。
 * 数据源 GET/POST/PUT/DELETE /api/assets，分页由后端 PageResponse 驱动。
 */

const TYPE_LABELS: Record<string, { label: string; color: string }> = {
  host: { label: '主机', color: 'arcoblue' },
  db: { label: '数据库', color: 'purple' },
  web: { label: 'Web 站点', color: 'orange' },
}

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  active: { label: '正常', color: 'green' },
  disabled: { label: '停用', color: 'gray' },
}

/** 列表状态 */
const loading = ref(false)
const assets = ref<AssetItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterType = ref<'all' | string>('all')

async function loadList() {
  loading.value = true
  try {
    const data = await listAssets({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      type: filterType.value === 'all' ? undefined : filterType.value,
    })
    assets.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载资产列表失败')
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

/** 新建/编辑抽屉 */
const drawerVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  type: 'host',
  address: '',
  protocol: 'SSH',
  port: undefined as number | undefined,
  groupPath: '',
  description: '',
})

const TYPE_OPTIONS = [
  { value: 'host', label: '主机' },
  { value: 'db', label: '数据库' },
  { value: 'web', label: 'Web 站点' },
]
const PROTOCOL_OPTIONS = ['SSH', 'RDP', 'VNC', 'MySQL', 'PostgreSQL', 'Oracle', 'Redis', 'HTTP', 'HTTPS']

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: 'host',
    address: '',
    protocol: 'SSH',
    port: undefined,
    groupPath: '',
    description: '',
  })
  drawerVisible.value = true
}

function openEdit(asset: AssetItem) {
  editingId.value = asset.id
  Object.assign(form, {
    name: asset.name,
    type: asset.type,
    address: asset.address,
    protocol: asset.protocol,
    port: asset.port ?? undefined,
    groupPath: asset.groupPath ?? '',
    description: asset.description ?? '',
  })
  drawerVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim() || !form.address.trim() || !form.protocol) {
    Message.warning('请完整填写名称、地址与协议')
    return
  }
  const payload: AssetRequest = {
    name: form.name.trim(),
    type: form.type,
    address: form.address.trim(),
    protocol: form.protocol,
    port: form.port ?? null,
    groupPath: form.groupPath.trim() || null,
    description: form.description.trim() || null,
  }
  saving.value = true
  try {
    if (editingId.value === null) {
      await createAsset(payload)
      Message.success(`资产 ${payload.name} 已创建`)
    } else {
      await updateAsset(editingId.value, payload)
      Message.success(`资产 ${payload.name} 已更新`)
    }
    drawerVisible.value = false
    loadList()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

/** 连通性测试（TCP 探测） */
const testingId = ref<number | null>(null)

async function handleTest(asset: AssetItem) {
  testingId.value = asset.id
  try {
    const result = await testAsset(asset.id)
    if (result.reachable) {
      Message.success(`${asset.name}（${asset.address}:${asset.port ?? ''}）可达，延迟 ${result.latencyMs}ms`)
    } else {
      Message.warning(`无法连通 ${asset.name}：${result.message}`)
    }
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '连通性测试失败')
  } finally {
    testingId.value = null
  }
}

/** 删除资产 */
function handleDelete(asset: AssetItem) {
  Modal.confirm({
    title: '删除资产',
    content: `确认删除资产「${asset.name}」？关联的账号与授权策略将一并失效。`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await deleteAsset(asset.id)
        Message.success(`资产 ${asset.name} 已删除`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '删除失败，请重试')
      }
    },
  })
}
</script>

<template>
  <div class="assets-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">资产列表</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索名称 / 地址 / 分组"
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
        <a-button type="primary" @click="openCreate">
          <template #icon><icon-plus /></template>
          新建资产
        </a-button>
      </div>
    </div>

    <!-- 筛选栏：类型 -->
    <div class="filter-bar">
      <a-radio-group v-model="filterType" type="button" size="small" @change="handleSearch">
        <a-radio value="all">全部类型</a-radio>
        <a-radio value="host">主机</a-radio>
        <a-radio value="db">数据库</a-radio>
        <a-radio value="web">Web 站点</a-radio>
      </a-radio-group>
      <span class="filter-count">共 {{ total }} 个</span>
    </div>

    <!-- 资产表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="assets"
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
          <a-table-column title="名称" data-index="name">
            <template #cell="{ record }">
              <span class="asset-name">{{ record.name }}</span>
            </template>
          </a-table-column>
          <a-table-column title="类型">
            <template #cell="{ record }">
              <a-tag size="small" :color="TYPE_LABELS[record.type]?.color ?? 'gray'">
                {{ TYPE_LABELS[record.type]?.label ?? record.type }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="地址" data-index="address" />
          <a-table-column title="协议">
            <template #cell="{ record }">
              <span class="protocol-cell">{{ record.protocol }}</span>
            </template>
          </a-table-column>
          <a-table-column title="端口">
            <template #cell="{ record }">
              <span class="port-cell">{{ record.port ?? '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="分组" data-index="groupPath">
            <template #cell="{ record }">
              <span class="group-cell">{{ record.groupPath || '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="状态">
            <template #cell="{ record }">
              <a-tag size="small" :color="STATUS_LABELS[record.status]?.color ?? 'gray'">
                {{ STATUS_LABELS[record.status]?.label ?? record.status }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="账号数" data-index="accountCount" :width="80" />
          <a-table-column title="操作" :width="220">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link :loading="testingId === record.id" @click="handleTest(record)">测试</a-link>
                <a-link @click="openEdit(record)">编辑</a-link>
                <a-link status="danger" @click="handleDelete(record)">删除</a-link>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:visible="drawerVisible"
      :title="editingId === null ? '新建资产' : '编辑资产'"
      :width="420"
      :footer="false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="资产名称" required>
          <a-input v-model="form.name" placeholder="如 web-prod-01" />
        </a-form-item>
        <a-form-item label="资产类型" required>
          <a-radio-group v-model="form.type" type="button">
            <a-radio v-for="option in TYPE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="地址" required>
          <a-input v-model="form.address" placeholder="IP 或域名（Web 站点填完整 URL）" />
        </a-form-item>
        <a-form-item label="协议" required>
          <a-select v-model="form.protocol">
            <a-option v-for="protocol in PROTOCOL_OPTIONS" :key="protocol" :value="protocol">
              {{ protocol }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="端口">
          <a-input-number v-model="form.port" :min="1" :max="65535" placeholder="协议默认端口" class="full-width" />
        </a-form-item>
        <a-form-item label="分组">
          <a-input v-model="form.groupPath" placeholder="如 生产环境/Web" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="saving" @click="handleSubmit">保存</a-button>
          <a-button class="cancel-btn" @click="drawerVisible = false">取消</a-button>
        </a-form-item>
      </a-form>
    </a-drawer>
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
.asset-name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.protocol-cell {
  color: var(--color-primary);
}

.port-cell,
.group-cell {
  color: var(--color-text-2);
}

.full-width {
  width: 100%;
}

.cancel-btn {
  margin-left: var(--spacing-row);
}
</style>
