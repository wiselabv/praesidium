//! 审计事件发布：薄抽象层（Broker 可替换），默认实现为 RabbitMQ（lapin）。
//!
//! 事件流向：Rust 会话事件 → 交换机 `praesidium.audit`（topic）
//!   - routing key `session.*`：Java 消费落库（sessions/audit_logs）
//!   - routing key `session.command`：Python 消费做 AI 异常检测

use anyhow::{anyhow, Result};
use async_trait::async_trait;
use chrono::Utc;
use lapin::{
    options::{BasicPublishOptions, ExchangeDeclareOptions},
    types::FieldTable,
    BasicProperties, Connection, ConnectionProperties, ExchangeKind,
};
use serde::Serialize;

/// 事件发布抽象：替换 Broker 时仅需换实现。
#[async_trait]
pub trait EventPublisher: Send + Sync {
    /// 发布事件到指定 routing key（topic 交换）。
    async fn publish(&self, routing_key: &str, payload: &[u8]) -> Result<()>;
}

/// RabbitMQ 实现（lapin），交换类型 topic，自动声明。
pub struct RabbitPublisher {
    conn: Connection,
    exchange: String,
}

impl RabbitPublisher {
    pub async fn connect(amqp_url: &str, exchange: &str) -> Result<Self> {
        let conn = Connection::connect(amqp_url, ConnectionProperties::default())
            .await
            .map_err(|err| anyhow!("RabbitMQ 连接失败: {err}"))?;
        let channel = conn
            .create_channel()
            .await
            .map_err(|err| anyhow!("RabbitMQ 通道创建失败: {err}"))?;
        channel
            .exchange_declare(
                exchange,
                ExchangeKind::Topic,
                ExchangeDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                FieldTable::default(),
            )
            .await
            .map_err(|err| anyhow!("交换机声明失败: {err}"))?;
        Ok(Self {
            conn,
            exchange: exchange.to_string(),
        })
    }
}

#[async_trait]
impl EventPublisher for RabbitPublisher {
    async fn publish(&self, routing_key: &str, payload: &[u8]) -> Result<()> {
        let channel = self
            .conn
            .create_channel()
            .await
            .map_err(|err| anyhow!("RabbitMQ 通道创建失败: {err}"))?;
        channel
            .basic_publish(
                &self.exchange,
                routing_key,
                BasicPublishOptions::default(),
                payload,
                BasicProperties::default()
                    .with_content_type("application/json".into())
                    .with_delivery_mode(2), // persistent
            )
            .await
            .map_err(|err| anyhow!("RabbitMQ 发布失败: {err}"))?;
        Ok(())
    }
}

/// 统一事件信封。
#[derive(Debug, Clone, Serialize)]
pub struct AuditEnvelope<T: Serialize> {
    pub event: &'static str,
    pub occurred_at: String,
    #[serde(flatten)]
    pub data: T,
}

impl<T: Serialize> AuditEnvelope<T> {
    pub fn new(event: &'static str, data: T) -> Self {
        Self {
            event,
            occurred_at: Utc::now().to_rfc3339(),
            data,
        }
    }

    pub fn to_json(&self) -> Result<Vec<u8>> {
        serde_json::to_vec(self).map_err(|err| anyhow!("事件序列化失败: {err}"))
    }
}

/// 会话开始事件（Java 落库：会话状态/审计日志）。
#[derive(Debug, Clone, Serialize)]
pub struct SessionStartedData {
    pub session_id: i64,
    pub user_id: i64,
    pub asset_id: i64,
    pub account: String,
    pub protocol: String,
}

/// 会话结束事件。
#[derive(Debug, Clone, Serialize)]
pub struct SessionEndedData {
    pub session_id: i64,
    pub user_id: i64,
    pub reason: String,
}

/// 命令执行事件（Java 落审计 + Python AI 检测）。
#[derive(Debug, Clone, Serialize)]
pub struct CommandData {
    pub session_id: i64,
    pub user_id: i64,
    pub asset_id: i64,
    pub account: String,
    pub command: String,
}

/// 录像切片索引事件（Java 落库录像信息）。
#[derive(Debug, Clone, Serialize)]
pub struct RecordingIndexData {
    pub session_id: i64,
    pub object_key: String,
    pub size_bytes: u64,
}

/// 常用 routing key 常量。
pub mod routing {
    pub const SESSION_STARTED: &str = "session.started";
    pub const SESSION_ENDED: &str = "session.ended";
    pub const SESSION_COMMAND: &str = "session.command";
    pub const RECORDING_INDEX: &str = "session.recording.index";
}

/// 便捷封装：发布会话开始事件。
pub async fn emit_session_started(
    publisher: &dyn EventPublisher,
    data: SessionStartedData,
) -> Result<()> {
    publisher
        .publish(
            routing::SESSION_STARTED,
            &AuditEnvelope::new("session.started", data).to_json()?,
        )
        .await
}

/// 便捷封装：发布会话结束事件。
pub async fn emit_session_ended(
    publisher: &dyn EventPublisher,
    data: SessionEndedData,
) -> Result<()> {
    publisher
        .publish(
            routing::SESSION_ENDED,
            &AuditEnvelope::new("session.ended", data).to_json()?,
        )
        .await
}

/// 便捷封装：发布命令事件。
pub async fn emit_command(publisher: &dyn EventPublisher, data: CommandData) -> Result<()> {
    publisher
        .publish(
            routing::SESSION_COMMAND,
            &AuditEnvelope::new("session.command", data).to_json()?,
        )
        .await
}

/// 便捷封装：发布录像索引事件。
pub async fn emit_recording_index(
    publisher: &dyn EventPublisher,
    data: RecordingIndexData,
) -> Result<()> {
    publisher
        .publish(
            routing::RECORDING_INDEX,
            &AuditEnvelope::new("session.recording.index", data).to_json()?,
        )
        .await
}
