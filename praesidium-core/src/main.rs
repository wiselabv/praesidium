//! Praesidium 核心代理入口。

use std::sync::Arc;

use praesidium_core::{
    audit::RabbitPublisher, auth::TokenVerifier, config::CoreConfig, policy,
    server::{self, AppState},
};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let config = CoreConfig::from_env();

    // RabbitMQ 审计事件发布（失败不阻断启动：本地无中间件时仍可提供 mock 降级）
    let publisher: Arc<dyn praesidium_core::audit::EventPublisher> =
        match RabbitPublisher::connect(&config.amqp_url, &config.audit_exchange).await {
            Ok(publi) => {
                tracing::info!("RabbitMQ 审计事件通道已就绪");
                Arc::new(publi)
            }
            Err(err) => {
                tracing::warn!(%err, "RabbitMQ 不可用，审计事件发布降级为空操作");
                Arc::new(NoopPublisher)
            }
        };

    // 授权策略表 + gRPC 策略流（后台任务，断线自动重连）
    let policies = Arc::new(policy::PolicyStore::new());
    tokio::spawn(policy::run_policy_stream(
        config.policy_grpc_addr.clone(),
        Arc::clone(&policies),
    ));

    let state = Arc::new(AppState {
        verifier: TokenVerifier::new(&config.jwt_secret),
        publisher,
        policies,
        config: config.clone(),
    });

    let listener = tokio::net::TcpListener::bind(&config.listen_addr).await?;
    tracing::info!(addr = %config.listen_addr, "praesidium-core listening");
    tracing::info!(
        minio_endpoint = %config.minio_endpoint,
        minio_access_key = %config.minio_access_key,
        minio_secret_len = config.minio_secret_key.len(),
        "MinIO 录像存储配置"
    );
    axum::serve(listener, server::app(state)).await?;
    Ok(())
}

/// 中间件不可用时的空操作发布器（保证链路降级可运行）。
struct NoopPublisher;

#[async_trait::async_trait]
impl praesidium_core::audit::EventPublisher for NoopPublisher {
    async fn publish(&self, _routing_key: &str, _payload: &[u8]) -> anyhow::Result<()> {
        Ok(())
    }
}
