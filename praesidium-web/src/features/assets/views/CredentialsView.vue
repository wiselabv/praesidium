<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  createCredential,
  deleteCredential,
  listCredentials,
  revealCredential,
  updateCredential,
} from '../../../api/credentials'
import type { CredentialItem, CredentialRequest } from '../../../api/credentials'

/**
 * 凭据页（L4）。
 *
 * 资产模块之三：凭据集中管理（密码/私钥），密文加密存储，
 * 列表只回显掩码，明文经「查看」单次展示后即弃。
 * 数据源 GET/POST/PUT/DELETE /api/credentials、GET /api/credentials/{id}/secret。
 */

const TYPE_LABELS: Record<string, { label: string; color: string }> = {
  password: { label: '密码', color: 'arcoblue' },
  key: { label: '私钥', color: 'purple' },
}

/** 列表状态 */
const loading = ref(false)
const credentials = ref<CredentialItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const data = await listCredentials({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
    })
    credentials.value = data.items
    total.value = data.total
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载凭据列表失败')
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
  type: 'password',
  secret: '',
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', type: 'password', secret: '' })
  drawerVisible.value = true
}

function openEdit(credential: CredentialItem) {
  editingId.value = credential.id
  Object.assign(form, { name: credential.name, type: credential.type, secret: '' })
  drawerVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim() || !form.type) {
    Message.warning('请完整填写凭据名称与类型')
    return
  }
  if (editingId.value === null && !form.secret.trim()) {
    Message.warning('新建凭据必须填写密钥/密码内容')
    return
  }
  const payload: CredentialRequest = {
    name: form.name.trim(),
    type: form.type,
    // 修改时 secret 留空表示不轮换
    secret: form.secret.trim() || undefined,
  }
  saving.value = true
  try {
    if (editingId.value === null) {
      await createCredential(payload)
      Message.success(`凭据 ${payload.name} 已创建`)
    } else {
      await updateCredential(editingId.value, payload)
      Message.success(`凭据 ${payload.name} 已更新`)
    }
    drawerVisible.value = false
    loadList()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

/** 单次明文查看 */
const revealingId = ref<number | null>(null)

async function handleReveal(credential: CredentialItem) {
  revealingId.value = credential.id
  try {
    const result = await revealCredential(credential.id)
    Modal.info({
      title: `凭据「${credential.name}」明文（仅本次展示）`,
      content: result.secret,
      simple: false,
    })
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '查看失败，请重试')
  } finally {
    revealingId.value = null
  }
}

/** 删除凭据 */
function handleDelete(credential: CredentialItem) {
  Modal.confirm({
    title: '删除凭据',
    content: `确认删除凭据「${credential.name}」？关联账号将失去凭据引用。`,
    okText: '删除',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await deleteCredential(credential.id)
        Message.success(`凭据 ${credential.name} 已删除`)
        loadList()
      } catch (error) {
        Message.error(error instanceof Error ? error.message : '删除失败，请重试')
      }
    },
  })
}
</script>

<template>
  <div class="credentials-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <div class="page-left">
        <h2 class="page-title">凭据</h2>
        <a-input-search
          v-model="keyword"
          placeholder="搜索凭据名称"
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
          新建凭据
        </a-button>
      </div>
    </div>

    <!-- 凭据表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :data="credentials"
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
          <a-table-column title="名称">
            <template #cell="{ record }">
              <span class="credential-name">{{ record.name }}</span>
            </template>
          </a-table-column>
          <a-table-column title="类型">
            <template #cell="{ record }">
              <a-tag size="small" :color="TYPE_LABELS[record.type]?.color ?? 'gray'">
                {{ TYPE_LABELS[record.type]?.label ?? record.type }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="密文掩码" data-index="secretMasked">
            <template #cell="{ record }">
              <span class="masked-cell">{{ record.secretMasked }}</span>
            </template>
          </a-table-column>
          <a-table-column title="关联账号数" data-index="boundAccounts" :width="110" />
          <a-table-column title="操作" :width="160">
            <template #cell="{ record }">
              <a-space :wrap="true">
                <a-link :loading="revealingId === record.id" @click="handleReveal(record)">查看</a-link>
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
      :title="editingId === null ? '新建凭据' : '编辑凭据'"
      :width="420"
      :footer="false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="凭据名称" required>
          <a-input v-model="form.name" placeholder="如 web-root-密钥" />
        </a-form-item>
        <a-form-item label="类型" required>
          <a-radio-group v-model="form.type" type="button">
            <a-radio value="password">密码</a-radio>
            <a-radio value="key">私钥</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="editingId === null ? '密钥/密码内容（必填）' : '密钥/密码内容（留空表示不轮换）'">
          <a-textarea v-model="form.secret" :auto-size="{ minRows: 3, maxRows: 8 }" placeholder="密文将经 AES-256-GCM 加密落库" />
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

.table-card {
  box-shadow: var(--shadow-card);
}

/* 单元格 */
.credential-name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.masked-cell {
  color: var(--color-text-3);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
}

.cancel-btn {
  margin-left: var(--spacing-row);
}
</style>
