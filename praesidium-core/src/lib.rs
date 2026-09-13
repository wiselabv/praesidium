//! Praesidium 核心代理库。
//!
//! 二进制 `praesidium` 基于 Tokio + Axum 提供：
//! - HTTP 健康检查
//! - WebSocket 终端透传（JWT 验签 → 拉连接信息 → 策略校验 → russh SSH 代理）
//! - 审计事件发布（RabbitMQ）+ 会话录像切片（MinIO）
//! - gRPC 客户端接收授权策略流（Java 推送）

pub mod audit;
pub mod auth;
pub mod config;
pub mod conninfo;
pub mod gateway;
pub mod policy;
pub mod recorder;
pub mod server;
