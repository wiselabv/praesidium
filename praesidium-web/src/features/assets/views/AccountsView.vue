<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import {
  deleteAssetAccount,
  listAssetAccounts,
  updateAssetAccount,
} from '../../../api/accounts'
import type { AssetAccountItem, AssetAccountRequest } from '../../../api/accounts'
import { listCredentials } from '../../../api/credentials'
import type { CredentialItem } from '../../../api/credentials'
import { listAssets } from '../../../api/assets'
import type { AssetItem } from '../../../api/assets'

/**
 * 资产账号页（L4）。
 *
 * 资产模块之二：全局账号清单（分页）+ 资产维度过滤 + 修改/删除。
 * 数据源 GET/PUT/DELETE /api/asset-accounts；资产过滤经 query 参数
 * （由资产列表「账号」入口带 assetId 跳转）。
 */

const TYPE_LABELS: Record<string, { label: string; color: string }> = {
  root: { label: 'root', color: 'red' },
  admin: { label: '管理员', color: 'orange' },
  service: { label: '服务账号', color: 'arcoblue' },
  normal: { label: '普通账号', color: 'green' },
}

const route = useRoute()

/** 列表状态 */
const loading = ref(false)
const accounts = ref<AssetAccountItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
/** 资产过滤（来自 /assets/inventory 的「账号」入口） */
const filterAssetId = ref<number | undefined>(
  typeof route.query.assetId === 'string' ? Number(route.query.assetId) : undefined,
)
/** 资产下拉数据源 */
const assetOptions = ref<AssetItem[]>([])
const selectedAssetId = ref<number | undefined>(filterAssetId.value)

async function loadAssets() {
  try {
    const data = await listAssets({ page: 1, size: 200 })
    assetOptions.value = data.items
  } catch {
    // 资产选项加载失败不阻塞主列表
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await listAssetAccounts({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
    })
    // 资产维度过滤：数据量小，前端按 assetId 过滤（演示规模）
    accounts.value = selectedAssetId.value
      ? data.items.filter((item) => item.assetId === selectedAssetId.value)
      : data.items
    total.value = selectedAssetId.value ? accounts.value.length : data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载账号列表失败')
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

onMounted(() => {
  loadAssets()
  loadList()
})

/** 编辑抽屉 */
const drawerVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  type: 'service',
  privileged: false,
  source: 'manual',
  credentialId: undefined as number | undefined,
  enabled: true,
})

const TYPE_OPTIONS = [
  { value: 'root', label: 'root' },
  { value: 'admin', label: '管理员' },
  { value: 'service', label: '服务账号' },
  { value: 'normal', label: '普通账号' },
]
/** 凭据下拉（可为空：账号不关联凭据） */
const credentialOptions = ref<CredentialItem[]>([])

async function loadCredentials() {
  try {
    const data = await listCredentials({ page: 1, size: 200 })
    credentialOptions.value = data.items
  } catch {
    // 凭据选项加载失败不阻塞编辑
  }
}

function openEdit(account: AssetAccountItem) {
  editingId.value = account.id
  Object.assign(form, {
    name: account.name,
    type: account.type,
    privileged: account.privileged,
    source: account.source ?? 'manual',
    credentialId: account.credentialId ?? undefined,
    enabled: account.enabled,
  })
  loadCredentials()
  drawerVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim() || !form.type) {
    Message.warning('请完整填写账号名与类型')
    return
  }
  const payload: AssetAccountRequest = {
    name: form.name.trim(),
    type: form.type,
    privileged: form.privileged,
    source: form.source,
    credentialId: form.credentialId ?? null,
    enabled: form.enabled,
  }
  saving.value = true
  try {
    if (editingId.value !== null) {
      await updateAssetAccount(editingId.value, payload)
      Message.success(`账号 ${payload.name} 已更新`)
    }
    drawerVisible.value = false
    loadList()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

/** 删除账号 */
function handleDelete(account: AssetAccountItem) {
  Modal.confirm({
    title: '删除账号',
    content: `确认删除 ${account.assetName} 上的账号「${account.name}」？`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await deleteAssetAccount(account.id)
        Message.success(`账号 ${account.name} 已删除`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '删除失败，请重试')
      }
    },
  })
}
</script>

<template>
  <div class="accounts-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">账号</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索账号名 / 资产"
          allow-clear
          class="page-search"
          @search="handleSearch"
        />
        <a-select
          v-model="selectedAssetId"
          placeholder="按资产过滤"
          allow-clear
          class="asset-filter"
          @change="handleSearch"
        >
          <a-option v-for="asset in assetOptions" :key="asset.id" :value="asset.id">
            {{ asset.name }}
          </a-option>
        </a-select>
      </div>
      <div class="page-actions">
        <a-button @click="loadList">
          <template #icon><icon-refresh /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 账号表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="accounts"
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
          <a-table-column title="账号名">
            <template #cell="{ record }">
              <span class="account-name">{{ record.name }}</span>
            </template>
          </a-table-column>
          <a-table-column title="资产" data-index="assetName" />
          <a-table-column title="类型">
            <template #cell="{ record }">
              <a-tag size="small" :color="TYPE_LABELS[record.type]?.color ?? 'gray'">
                {{ TYPE_LABELS[record.type]?.label ?? record.type }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="特权">
            <template #cell="{ record }">
              <a-tag size="small" :color="record.privileged ? 'red' : 'gray'">
                {{ record.privileged ? '是' : '否' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="来源">
            <template #cell="{ record }">
              <span class="source-cell">
                {{ record.source === 'discovery' ? '自动发现' : '手动录入' }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="关联凭据">
            <template #cell="{ record }">
              <span class="credential-cell">{{ record.credentialName ?? '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="状态">
            <template #cell="{ record }">
              <a-tag size="small" :color="record.enabled ? 'green' : 'gray'">
                {{ record.enabled ? '启用' : '停用' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="140">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link @click="openEdit(record)">编辑</a-link>
                <a-link status="danger" @click="handleDelete(record)">删除</a-link>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑抽屉 -->
    <a-drawer
      v-model:visible="drawerVisible"
      title="编辑账号"
      :width="420"
      :footer="false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="账号名" required>
          <a-input v-model="form.name" placeholder="如 root / deploy" />
        </a-form-item>
        <a-form-item label="账号类型" required>
          <a-select v-model="form.type">
            <a-option v-for="option in TYPE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="特权账号">
          <a-switch v-model="form.privileged" />
          <span class="switch-tip">root / 管理员等高风险账号</span>
        </a-form-item>
        <a-form-item label="来源">
          <a-radio-group v-model="form.source" type="button">
            <a-radio value="manual">手动录入</a-radio>
            <a-radio value="discovery">自动发现</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="关联凭据">
          <a-select v-model="form.credentialId" allow-clear placeholder="不关联">
            <a-option
              v-for="credential in credentialOptions"
              :key="credential.id"
              :value="credential.id"
            >
              {{ credential.name }}（{{ credential.type === 'password' ? '密码' : '密钥' }}）
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model="form.enabled" />
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

/* 页头左侧：标题 + 搜索 + 资产过滤并排 */
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

.asset-filter {
  width: 200px;
}

.table-card {
  box-shadow: var(--shadow-card);
}

/* 单元格 */
.account-name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.source-cell,
.credential-cell {
  color: var(--color-text-2);
}

.switch-tip {
  margin-left: var(--spacing-row);
  font-size: var(--font-size-sm);
  color: var(--color-text-3);
}

.cancel-btn {
  margin-left: var(--spacing-row);
}
</style>
