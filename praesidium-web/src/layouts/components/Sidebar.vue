<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'

interface MenuItem {
  key: string
  label: string
  icon: string
  /** 页面是否已实现：未实现的菜单点击仅提示（后续里程碑逐个点亮） */
  ready: boolean
}

interface MenuGroup {
  title: string
  items: MenuItem[]
}

/**
 * 一级菜单静态配置（M1 阶段）。
 * 后续 M5 接入权限体系后，改为按登录用户角色动态生成。
 */
const menus: MenuGroup[] = [
  {
    title: '',
    items: [{ key: '/dashboard', label: '总览', icon: 'icon-dashboard', ready: true }],
  },
  {
    title: '资产管理',
    items: [
      { key: '/assets/inventory', label: '资产列表', icon: 'icon-apps', ready: true },
      { key: '/assets/accounts', label: '账号', icon: 'icon-user-group', ready: true },
      { key: '/assets/credentials', label: '凭据', icon: 'icon-safe', ready: true },
    ],
  },
  {
    title: '访问控制',
    items: [{ key: '/access/policies', label: '授权策略', icon: 'icon-user-add', ready: true }],
  },
  {
    title: '会话',
    items: [
      { key: '/sessions/online', label: '在线会话', icon: 'icon-live-broadcast', ready: true },
      { key: '/sessions/recordings', label: '录像回放', icon: 'icon-play-circle', ready: true },
    ],
  },
  {
    title: '审计',
    items: [
      { key: '/audit/operations', label: '操作日志', icon: 'icon-file', ready: true },
      { key: '/audit/logins', label: '登录日志', icon: 'icon-history', ready: true },
    ],
  },
  {
    title: '系统设置',
    items: [{ key: '/settings', label: '系统设置', icon: 'icon-settings', ready: true }],
  },
]

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()

function isActive(item: MenuItem): boolean {
  return route.path === item.key || route.path.startsWith(item.key + '/')
}

function handleClick(item: MenuItem) {
  if (!item.ready) {
    Message.info(`「${item.label}」将在后续里程碑接入`)
    return
  }
  router.push(item.key)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <!-- Logo 区 -->
    <div class="sidebar-logo">
      <icon-safe class="logo-icon" />
      <span v-if="!collapsed" class="logo-text">Praesidium</span>
    </div>

    <!-- 菜单区 -->
    <nav class="sidebar-nav">
      <div v-for="group in menus" :key="group.title" class="menu-group">
        <div v-if="group.title && !collapsed" class="menu-group-title">{{ group.title }}</div>
        <div
          v-for="item in group.items"
          :key="item.key"
          class="menu-item"
          :class="{ active: isActive(item) }"
          :title="collapsed ? item.label : ''"
          @click="handleClick(item)"
        >
          <component :is="item.icon" class="menu-icon" />
          <span v-if="!collapsed" class="menu-label">{{ item.label }}</span>
        </div>
      </div>
    </nav>

    <!-- 折叠按钮 -->
    <div class="sidebar-collapse" @click="collapsed = !collapsed">
      <icon-right v-if="collapsed" />
      <icon-left v-else />
      <span v-if="!collapsed" class="collapse-text">折叠</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--layout-sidebar-width);
  height: 100vh;
  background: var(--color-sidebar-bg);
  transition: width var(--duration-base) ease;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: var(--layout-sidebar-collapsed);
}

/* Logo 区 */
.sidebar-logo {
  display: flex;
  align-items: center;
  height: var(--layout-header-height);
  padding: 0 var(--spacing-page);
  gap: var(--spacing-row);
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  font-size: var(--size-logo-icon);
  color: var(--color-primary);
  flex-shrink: 0;
}

.logo-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-sidebar-text-active);
}

/* 菜单区 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--spacing-xs) 0;
}

.menu-group {
  margin-bottom: var(--spacing-xs);
}

.menu-group-title {
  padding: var(--spacing-row) var(--spacing-page);
  font-size: var(--font-size-xs);
  color: var(--color-sidebar-group-title);
  white-space: nowrap;
}

.menu-item {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--size-menu-item);
  padding: 0 var(--spacing-page);
  gap: var(--spacing-row);
  color: var(--color-sidebar-text);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  transition: background var(--duration-fast) ease, color var(--duration-fast) ease;
}

.menu-item:hover {
  color: var(--color-sidebar-text-active);
  background: var(--color-sidebar-hover-bg);
}

.menu-item.active {
  color: var(--color-sidebar-text-active);
  /* 主色 25% 透明底：由 token 推导，不硬编码色值 */
  background: color-mix(in srgb, var(--color-primary) 25%, transparent);
}

/* 选中态左侧 3px 主色竖条 */
.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.menu-icon {
  font-size: var(--size-icon-md);
  flex-shrink: 0;
}

.menu-label {
  font-size: var(--font-size-md);
}

/* 折叠按钮 */
.sidebar-collapse {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--size-menu-item);
  gap: var(--spacing-row);
  border-top: 1px solid var(--color-sidebar-divider);
  color: var(--color-sidebar-text);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  transition: color var(--duration-fast) ease;
}

.sidebar-collapse:hover {
  color: var(--color-sidebar-text-active);
}

.collapse-text {
  font-size: var(--font-size-sm);
}
</style>
