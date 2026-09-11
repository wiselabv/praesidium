//! Praesidium 核心代理库。
//!
//! 二进制 `praesidium` 基于 Tokio + Axum 提供：
//! - HTTP 健康检查与内部管理接口
//! - WebSocket 终端透传（当前为占位回显，后续接入 russh SSH 代理）

pub mod config;
pub mod server;
