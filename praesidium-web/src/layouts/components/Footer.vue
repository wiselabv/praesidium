<script setup lang="ts">
/** 底部栏：极简处理，融入页面背景，不抢视觉重心 */
</script>

<template>
  <footer class="footer">
    <span>© 2026 Praesidium · Apache-2.0</span>
    <span>v0.1.0</span>
  </footer>
</template>

<style scoped>
.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--layout-footer-height);
  padding: 0 var(--spacing-page);
  font-size: var(--font-size-xs);
  color: var(--color-text-4);
  flex-shrink: 0;
}
</style>
