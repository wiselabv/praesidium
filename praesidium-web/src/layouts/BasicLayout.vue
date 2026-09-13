<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
</script>

<template>
  <div class="basic-layout">
    <Sidebar />
    <div class="basic-main">
      <Header />
      <main class="basic-content">
        <router-view />
      </main>
      <Footer />
    </div>
  </div>
</template>

<style scoped>
.basic-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.basic-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.basic-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-page);
}
</style>
