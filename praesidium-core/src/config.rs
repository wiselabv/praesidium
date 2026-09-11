//! 配置：从环境变量读取，前缀 `PRAESIDIUM_`。

use std::env;

/// 核心代理配置。
#[derive(Debug, Clone)]
pub struct CoreConfig {
    /// HTTP / WebSocket 监听地址。
    pub listen_addr: String,
}

impl CoreConfig {
    /// 从环境变量加载配置（默认值面向本地开发）。
    pub fn from_env() -> Self {
        Self {
            listen_addr: env::var("PRAESIDIUM_CORE_ADDR")
                .unwrap_or_else(|_| "127.0.0.1:8081".to_string()),
        }
    }
}
