<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  approvePolicy,
  createPolicy,
  deletePolicy,
  listPolicies,
  revokePolicy,
  updatePolicy,
} from '../../../api/policies'
import type { PolicyItem } from '../../../api/policies'
import { listUsers } from '../../../api/users'
import type { UserOption } from '../../../api/users'
import { listAssets } from '../../../api/assets'
import type { AssetItem } from '../../../api/assets'
import { listAssetAccounts } from '../../../api/accounts'
import type { AssetAccountItem } from '../../../api/accounts'

/**
 * 授权策略列表页（L4）。
 *
 * 访问控制核心：定义「谁（用户）→ 访问什么（资产/账号）→ 通过什么协议 → 在什么时间」的授权规则。
 * 策略审批流（申请-审批-下发）已接入后端：新建后为 pending，审批通过后 active，可随时撤销。
 * 数据源 /api/access-policies + /api/users + /api/assets + /api/asset-accounts。
 */

/** 后端状态生命周期：pending（待审批）→ active（生效）/ revoked（已撤销） */
const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  pending: { label: '待审批', color: 'orange' },
  active: { label: '生效中', color: 'green' },
  revoked: { label: '已撤销', color: 'gray' },
}

/** 列表状态（服务端分页） */
const loading = ref(false)
const policies = ref<PolicyItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filterStatus = ref<'all' | 'pending' | 'active' | 'revoked'>('all')
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await listPolicies({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      status: filterStatus.value === 'all' ? undefined : filterStatus.value,
    })
    policies.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载授权策略失败')
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

/** 下拉数据源（用户 / 资产 / 账号，演示规模一次性拉取） */
const userOptions = ref<UserOption[]>([])
const assetOptions = ref<AssetItem[]>([])
const accountOptions = ref<AssetAccountItem[]>([])

async function loadOptions() {
  try {
    const [users, assets, accounts] = await Promise.all([
      listUsers(),
      listAssets({ page: 1, size: 200 }),
      listAssetAccounts({ page: 1, size: 200 }),
    ])
    userOptions.value = users
    assetOptions.value = assets.items
    accountOptions.value = accounts.items
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载表单选项失败')
  }
}

const PROTOCOL_OPTIONS = ['SSH', 'RDP', 'VNC', 'MySQL', 'PostgreSQL', 'Redis', 'HTTP', 'HTTPS']

/** 新建/编辑抽屉 */
const drawerVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  name: '',
  userId: undefined as number | undefined,
  assetIds: [] as number[],
  accountIds: [] as number[],
  protocol: 'SSH',
  /** 有效期（range-picker 数组 v-model，value-format 输出 YYYY-MM-DD 字符串） */
  validRange: [] as string[],
  description: '',
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    userId: undefined,
    assetIds: [],
    accountIds: [],
    protocol: 'SSH',
    validRange: [],
    description: '',
  })
  drawerVisible.value = true
}

function openEdit(policy: PolicyItem) {
  editingId.value = policy.id
  Object.assign(form, {
    name: policy.name,
    userId: policy.userId,
    assetIds: [...policy.assetIds],
    accountIds: [...policy.accountIds],
    protocol: policy.protocol ?? 'SSH',
    validRange: [policy.validFrom?.slice(0, 10) ?? '', policy.validTo?.slice(0, 10) ?? ''],
    description: policy.description ?? '',
  })
  drawerVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim() || form.userId === undefined || form.assetIds.length === 0) {
    Message.warning('请完整填写策略名称、授权用户与资产范围')
    return
  }
  const payload = {
    name: form.name.trim(),
    userId: form.userId,
    assetIds: form.assetIds,
    accountIds: form.accountIds,
    protocol: form.protocol,
    validFrom: form.validRange[0] || null,
    validTo: form.validRange[1] || null,
    description: form.description.trim() || null,
  }
  submitting.value = true
  try {
    if (editingId.value === null) {
      await createPolicy(payload)
      Message.success(`策略 ${payload.name} 已创建，等待审批`)
    } else {
      await updatePolicy(editingId.value, payload)
      Message.success(`策略 ${payload.name} 已更新`)
    }
    drawerVisible.value = false
    loadList()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存策略失败')
  } finally {
    submitting.value = false
  }
}

/** 审批通过（pending → active） */
function handleApprove(policy: PolicyItem) {
  Modal.confirm({
    title: '审批策略',
    content: `确认审批通过授权策略「${policy.name}」？通过后 ${policy.userName} 将获得对应资产的访问权限。`,
    okText: '审批通过',
    onOk: async () => {
      try {
        await approvePolicy(policy.id)
        Message.success(`策略 ${policy.name} 已审批通过`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '审批失败')
      }
    },
  })
}

/** 撤销（active → revoked） */
function handleRevoke(policy: PolicyItem) {
  Modal.confirm({
    title: '撤销策略',
    content: `确认撤销授权策略「${policy.name}」？撤销后 ${policy.userName} 将立即失去对应资产的访问权限。`,
    okText: '撤销',
    okButtonProps: { status: 'warning' },
    onOk: async () => {
      try {
        await revokePolicy(policy.id)
        Message.success(`策略 ${policy.name} 已撤销`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '撤销失败')
      }
    },
  })
}

function handleDelete(policy: PolicyItem) {
  Modal.confirm({
    title: '删除策略',
    content: `确认删除授权策略「${policy.name}」？该操作不可恢复。`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await deletePolicy(policy.id)
        Message.success(`策略 ${policy.name} 已删除`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '删除失败')
      }
    },
  })
}

function formatDate(iso: string | null): string {
  return iso ? iso.slice(0, 10) : '长期'
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>

<template>
  <div class="policies-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">授权策略</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索策略名称 / 授权用户"
          allow-clear
          class="page-search"
          @search="handleSearch"
        />
      </div>
      <div class="page-actions">
        <a-button type="primary" @click="openCreate">
          <template #icon><icon-plus /></template>
          新建策略
        </a-button>
      </div>
    </div>

    <!-- 筛选栏：状态 -->
    <div class="filter-bar">
      <a-radio-group v-model="filterStatus" type="button" size="small" @change="handleSearch">
        <a-radio value="all">全部状态</a-radio>
        <a-radio value="pending">待审批</a-radio>
        <a-radio value="active">生效中</a-radio>
        <a-radio value="revoked">已撤销</a-radio>
      </a-radio-group>
      <span class="filter-count">共 {{ total }} 条</span>
    </div>

    <!-- 策略表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="policies"
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
          <a-table-column title="策略名称" data-index="name">
            <template #cell="{ record }">
              <span class="policy-name">
                <icon-idcard class="policy-icon" />
                {{ record.name }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="授权用户" data-index="userName" />
          <a-table-column title="资产范围" data-index="assetNames" />
          <a-table-column title="账号" data-index="accountNames" />
          <a-table-column title="协议">
            <template #cell="{ record }">
              <span>{{ record.protocol ?? '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="有效期">
            <template #cell="{ record }">
              <span class="valid-cell">
                {{ formatDate(record.validFrom) }} ~ {{ formatDate(record.validTo) }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="状态">
            <template #cell="{ record }">
              <a-tag size="small" :color="STATUS_LABELS[record.status]?.color ?? 'gray'">
                {{ STATUS_LABELS[record.status]?.label ?? record.status }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="210">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link @click="openEdit(record)">编辑</a-link>
                <a-link v-if="record.status === 'pending'" @click="handleApprove(record)">审批</a-link>
                <a-link v-else-if="record.status === 'active'" @click="handleRevoke(record)">撤销</a-link>
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
      :title="editingId === null ? '新建策略' : '编辑策略'"
      :width="420"
      :footer="false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="策略名称" required>
          <a-input v-model="form.name" placeholder="如 运维-生产Web-SSH" />
        </a-form-item>
        <a-form-item label="授权用户" required>
          <a-select v-model="form.userId" placeholder="选择用户">
            <a-option v-for="u in userOptions" :key="u.id" :value="u.id">
              {{ u.displayName || u.username }}（{{ u.username }}）
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="资产范围" required>
          <a-select v-model="form.assetIds" placeholder="选择资产（可多选）" multiple>
            <a-option v-for="a in assetOptions" :key="a.id" :value="a.id">
              {{ a.name }}（{{ a.address }}）
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="授权账号">
          <a-select v-model="form.accountIds" placeholder="选填，可后续绑定" multiple allow-clear>
            <a-option v-for="a in accountOptions" :key="a.id" :value="a.id">
              {{ a.assetName }} / {{ a.name }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="协议" required>
          <a-select v-model="form.protocol">
            <a-option v-for="p in PROTOCOL_OPTIONS" :key="p" :value="p">{{ p }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="有效期">
          <a-range-picker v-model="form.validRange" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model="form.description" placeholder="选填" :auto-size="{ minRows: 2, maxRows: 4 }" />
        </a-form-item>
        <a-form-item>
          <a-space class="drawer-actions">
            <a-button @click="drawerVisible = false">取消</a-button>
            <a-button type="primary" :loading="submitting" @click="handleSubmit">保存</a-button>
          </a-space>
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

/* 策略名 + 图标 */
.policy-name {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-text-1);
  font-weight: var(--font-weight-medium);
}

.policy-icon {
  font-size: var(--size-icon-md);
  color: var(--color-primary);
}

/* 有效期 */
.valid-cell {
  color: var(--color-text-2);
}

.drawer-actions {
  justify-content: flex-end;
  width: 100%;
}
</style>
