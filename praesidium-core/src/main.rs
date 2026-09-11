//! Praesidium 核心代理入口。

use praesidium_core::{config::CoreConfig, server};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let config = CoreConfig::from_env();
    let listener = tokio::net::TcpListener::bind(&config.listen_addr).await?;

    tracing::info!(addr = %config.listen_addr, "praesidium-core listening");
    axum::serve(listener, server::app()).await?;
    Ok(())
}
