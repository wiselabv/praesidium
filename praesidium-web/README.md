# praesidium-web

Praesidium 前端：管理后台 + 浏览器终端（Vue 3 + TypeScript + Vite）。

- 管理操作走 `/api` → Java 管理服务（Vite 代理，见 `vite.config.ts`）
- 终端连接走 `/ws` → Rust 核心代理（WebSocket）
- 终端渲染基于 xterm.js；UI 组件库使用 Arco Design Vue（按需引入）

## 开发

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # 类型检查 + 产物构建
```
