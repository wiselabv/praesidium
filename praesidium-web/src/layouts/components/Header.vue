<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 面包屑：首页 + 当前页标题（自动根据路由 meta 生成） */
const crumbs = computed(() => {
  const current = (route.meta.title as string | undefined) ?? '未命名页面'
  return route.path.startsWith('/dashboard') ? [current] : ['首页', current]
})

/** 用户展示名：优先显示名，回退用户名 */
const userName = computed(() => (auth.user ? (auth.user.displayName ?? auth.user.username) : '未登录'))
const userInitial = computed(() =>
  userName.value === '未登录' ? 'U' : userName.value.slice(0, 1).toUpperCase(),
)

/* 全屏切换 */
const isFullscreen = ref(false)

function syncFullscreen() {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

async function toggleFullscreen() {
  if (document.fullscreenElement) {
    await document.exitFullscreen()
  } else {
    await document.documentElement.requestFullscreen()
  }
}

onMounted(() => document.addEventListener('fullscreenchange', syncFullscreen))
onUnmounted(() => document.removeEventListener('fullscreenchange', syncFullscreen))

/* 全局操作（占位，后续里程碑接入） */
function handleSearch() {
  Message.info('全局搜索将在后续里程碑接入')
}

function handleNotification() {
  Message.info('告警中心将在后续里程碑接入')
}

function handleTerminal() {
  router.push('/terminal')
}

function handleSettings() {
  router.push('/settings')
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

function handleProfile() {
  router.push('/profile')
}
</script>

<template>
  <header class="header">
    <!-- 面包屑 -->
    <a-breadcrumb class="header-crumbs">
      <a-breadcrumb-item
        v-for="(item, index) in crumbs"
        :key="index"
        :class="{ 'crumb-current': index === crumbs.length - 1 }"
      >
        {{ item }}
      </a-breadcrumb-item>
    </a-breadcrumb>

    <!-- 全局功能区 -->
    <div class="header-actions">
      <a-input-search
        class="header-search"
        placeholder="全局搜索（资源 / 用户 / 会话）"
        allow-clear
        @search="handleSearch"
      />
      <div class="header-icon" @click="handleTerminal"><icon-code-square /></div>
      <div class="header-icon" @click="handleSettings"><icon-settings /></div>
      <!-- Arco Badge：dot 红点需 count>0 才会渲染（count 未传时 Number(undefined)=NaN）。
           后续 M4 告警中心接入后，count 绑定真实未读数，超过 99 自动显示 99+ -->
      <a-badge :dot="true" :count="1">
        <div class="header-icon" @click="handleNotification"><icon-notification /></div>
      </a-badge>
      <div class="header-icon" @click="toggleFullscreen">
        <icon-fullscreen-exit v-if="isFullscreen" />
        <icon-fullscreen v-else />
      </div>
      <a-dropdown position="br">
        <div class="user-trigger">
          <!-- 头像尺寸与 --size-avatar token 一致（组件 prop 无法消费 CSS 变量） -->
          <a-avatar :size="28" :style="{ backgroundColor: 'var(--color-primary)' }">
            {{ userInitial }}
          </a-avatar>
          <span class="user-name">{{ userName }}</span>
          <icon-down class="user-arrow" />
        </div>
        <template #content>
          <a-doption @click="handleProfile">
            <template #icon><icon-user /></template>
            个人中心
          </a-doption>
          <a-doption @click="handleLogout">
            <template #icon><icon-poweroff /></template>
            退出登录
          </a-doption>
        </template>
      </a-dropdown>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--layout-header-height);
  padding: 0 var(--spacing-page);
  background: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.header-crumbs {
  font-size: var(--font-size-sm);
}

.crumb-current {
  color: var(--color-text-2);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-page);
}

.header-search {
  width: 220px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--size-header-icon);
  height: var(--size-header-icon);
  border-radius: var(--radius-md);
  color: var(--color-text-2);
  font-size: var(--size-icon-md);
  cursor: pointer;
  transition: background var(--duration-fast) ease, color var(--duration-fast) ease;
}

.header-icon:hover {
  background: var(--color-table-header);
  color: var(--color-primary);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  cursor: pointer;
}

.user-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-1);
}

.user-arrow {
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}
</style>
