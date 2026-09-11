# Praesidium

**从零自研的开源堡垒机（Bastion Host / PAM）—— 功能完整、无商业限制、永久免费。**

> **项目状态：早期开发中（Early Development）。**
> API、模块结构与配置项可能随时变化，当前版本**不建议用于生产环境**。

## 这是什么

Praesidium（拉丁语：驻军、卫戍要塞）是一个完全开源的运维审计系统（堡垒机），目标只有一个：
提供**功能完整**、没有"社区版 / 企业版"功能切割、没有商业许可限制的替代方案。

- **不设付费墙**：所有功能都在这个仓库里，没有"联系销售"才能解锁的能力；
- **不绑公司规模**：Apache-2.0 许可证，不限制使用者的人数、营收与商用场景；
- **审计不可篡改**：会话录像与命令日志按 append-only 设计存储，管理界面只读，写入前计算哈希并记录。

## 架构

多语言分工，各用所长；核心会话链路保持单一语言（Rust），不与外围服务做同步跨语言调用。

| 模块 | 语言 / 技术 | 职责 |
| --- | --- | --- |
| `praesidium-core` | Rust（Tokio / Axum / russh） | SSH / RDP / VNC 代理、命令审计、会话录制、实时水印、策略执行点、WebSocket / WebRTC 网关 |
| `praesidium-admin` | Java（Spring Boot） | 管理面：用户 / 资产 / 授权管理、RBAC / ABAC、审批工作流、SSO / LDAP / AD、审计查询、报表 |
| `praesidium-python` | Python（FastAPI / Celery） | 自动化运维（Ansible）、定时巡检、异常行为检测、风险评分、告警通知 |
| `praesidium-web` | Vue 3 + TypeScript | 管理后台、浏览器终端（xterm.js）、会话回放、监控大屏 |

通信边界（核心原则）：

```text
Vue 前端
  ├── 管理操作 ── HTTP ──────────> praesidium-admin (Java)
  └── 终端连接 ── WebSocket/WebRTC ──> praesidium-core (Rust) ──> 目标资产
                                          │
                                          │ 审计事件（异步）
                                          ▼
                                      消息队列 ──> praesidium-python (自动化 / AI)
```

- 管理操作走 Java，终端流量**不经过** Java，避免性能瓶颈与单点故障；
- 审计事件由 Rust 异步上报消息队列，Java 落库、Python 消费做 AI 分析；
- Java 通过 gRPC 向 Rust 下发策略（命令 ACL、会话控制），Rust 是策略执行点。

> **代码风格与分层规范见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**：
> 后端 DDD 分层（限界上下文 + api/application/domain/infrastructure），前端套娃组件 + 统一自由设置（Schema 驱动）。结构即文档——忘了代码，看目录就知道系统在做什么。

## 目录结构

```text
praesidium/
├── praesidium-core/     # Rust 核心代理
├── praesidium-admin/    # Java 管理服务
├── praesidium-python/   # Python 自动化与 AI 服务
└── praesidium-web/      # Vue 前端
```

## 路线图

- [ ] **阶段一（MVP）**：SSH 代理 + 浏览器终端 + 会话录制（asciinema 格式）+ 基础 RBAC
- [ ] **阶段二（审计增强）**：命令级 ACL、高危命令实时拦截、实时水印、审计日志结构化存储
- [ ] **阶段三（协议扩展）**：RDP / VNC（IronRDP + WebRTC）、数据库协议代理、文件传输审计
- [ ] **阶段四（企业能力）**：LDAP / AD 集成、MFA、审批工作流、HA 部署

## 快速开始

> 各模块仍在骨架阶段，以下命令用于本地开发调试。

```bash
# Rust 核心代理（当前提供 /health 与 WebSocket 占位透传）
# 注：本机需先装 Visual Studio（C 工具链）才能编译，详见 docs/ARCHITECTURE.md §6
cd praesidium-core
cargo run          # 默认监听 127.0.0.1:8081

# Vue 前端
cd praesidium-web
npm install
npm run dev        # 默认 http://localhost:5173

# Python 自动化与 AI 服务
cd praesidium-python
uv sync
uv run praesidium-python   # 默认 http://127.0.0.1:8000

# Java 管理服务（需要 JDK 25 + Maven，或直接用 IDE 打开）
cd praesidium-admin
mvn spring-boot:run        # 默认 http://localhost:8080
```

## 免责声明

- 本项目为**个人开源项目**，不提供任何**合规保证、法律担保或技术支持承诺**；
- 使用者需自行评估是否符合其所在地区的法律法规要求（如等保、网络安全法等），并**自行承担使用风险**；
- 本项目**完全免费，不接受付费支持请求**。如有问题欢迎提 Issue，但**不保证响应时间**；
- 项目提供可配置的审计日志留存与命令拦截能力，但"是否符合某项法规 / 标准"的判定应由使用者与评估机构完成，本项目不作任何结论性声明。

## 许可证

[Apache License 2.0](LICENSE) © 2026 wiselabv

选择 Apache-2.0 的原因：许可证宽松（MIT 级别的自由度），同时包含明确的专利授权与免责条款，对使用者和贡献者都更安全。
