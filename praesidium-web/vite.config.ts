import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 管理面：转发到 Java 管理服务
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 终端面：转发到 Rust 核心代理（WebSocket）
      '/ws': {
        target: 'ws://127.0.0.1:8081',
        ws: true,
      },
    },
  },
})
