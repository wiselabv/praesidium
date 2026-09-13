//! 配置：从环境变量读取，前缀 `PRAESIDIUM_`。

use std::env;

/// 核心代理配置。
#[derive(Debug, Clone)]
pub struct CoreConfig {
    /// HTTP / WebSocket 监听地址
    pub listen_addr: String,
    /// 网关令牌共享密钥（与 Java `PRAESIDIUM_JWT_SECRET` 一致）
    pub jwt_secret: String,
    /// RabbitMQ 地址（审计事件发布）
    pub amqp_url: String,
    /// 审计事件交换机（topic）
    pub audit_exchange: String,
    /// MinIO 连接参数（会话录像存储）
    pub minio_endpoint: String,
    pub minio_access_key: String,
    pub minio_secret_key: String,
    pub minio_bucket: String,
    /// Java 管理服务内网地址（拉取连接信息）
    pub java_base: String,
    /// 内网共享密钥（随请求头 `X-Internal-Key` 发送）
    pub internal_key: String,
    /// Java gRPC 策略流地址
    pub policy_grpc_addr: String,
}

impl CoreConfig {
    /// 从环境变量加载配置（默认值面向本地开发）。
    pub fn from_env() -> Self {
        Self {
            listen_addr: env::var("PRAESIDIUM_CORE_ADDR")
                .unwrap_or_else(|_| "127.0.0.1:8081".to_string()),
            jwt_secret: env::var("PRAESIDIUM_JWT_SECRET").unwrap_or_else(|_| {
                "praesidium-dev-only-secret-change-me-in-production-0123456789".to_string()
            }),
            // AMQP 凭据为真实凭据，经环境变量注入，默认值不含密码（本地无中间件时降级空操作）
            amqp_url: env::var("PRAESIDIUM_AMQP_URL")
                .unwrap_or_else(|_| "amqp://127.0.0.1:5672/%2F".to_string()),
            audit_exchange: env::var("PRAESIDIUM_AUDIT_EXCHANGE")
                .unwrap_or_else(|_| "praesidium.audit".to_string()),
            minio_endpoint: env::var("PRAESIDIUM_MINIO_ENDPOINT")
                .unwrap_or_else(|_| "http://192.168.10.4:9000".to_string()),
            minio_access_key: env::var("PRAESIDIUM_MINIO_ACCESS_KEY")
                .unwrap_or_else(|_| "minio".to_string()),
            // 密钥为真实凭据，不落代码默认值；开发环境经 PRAESIDIUM_MINIO_SECRET_KEY 注入
            minio_secret_key: env::var("PRAESIDIUM_MINIO_SECRET_KEY").unwrap_or_default(),
            minio_bucket: env::var("PRAESIDIUM_MINIO_BUCKET")
                .unwrap_or_else(|_| "praesidium-recordings".to_string()),
            java_base: env::var("PRAESIDIUM_JAVA_BASE")
                .unwrap_or_else(|_| "http://127.0.0.1:8080".to_string()),
            internal_key: env::var("PRAESIDIUM_INTERNAL_KEY")
                .unwrap_or_else(|_| "praesidium-dev-internal-key".to_string()),
            policy_grpc_addr: env::var("PRAESIDIUM_POLICY_GRPC_ADDR")
                .unwrap_or_else(|_| "http://127.0.0.1:9090".to_string()),
        }
    }
}
