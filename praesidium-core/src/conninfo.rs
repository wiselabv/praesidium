//! 连接信息拉取：向 Java 管理服务内网接口获取目标资产与解密凭据。

use anyhow::{anyhow, Result};
use serde::Deserialize;

use crate::gateway::CredentialAuth;

/// 内网接口返回的连接信息（Java 侧解密凭据后下发，仅在网关内使用）。
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectionInfo {
    pub asset_id: i64,
    pub host: String,
    pub port: u16,
    pub protocol: String,
    pub account: String,
    pub account_id: Option<i64>,
    /// password | key
    pub auth_type: String,
    pub secret: String,
}

impl ConnectionInfo {
    pub fn to_auth(&self) -> Result<CredentialAuth> {
        match self.auth_type.as_str() {
            "password" => Ok(CredentialAuth::Password(self.secret.clone())),
            "key" => Ok(CredentialAuth::PrivateKey(self.secret.clone())),
            other => Err(anyhow!("未知凭据类型: {other}")),
        }
    }
}

/// 统一响应信封（与 Java ApiResponse 对齐）。
#[derive(Debug, Deserialize)]
struct ApiEnvelope<T> {
    code: i32,
    #[allow(dead_code)]
    message: String,
    data: Option<T>,
}

/// 拉取会话连接信息：`GET {java_base}/api/internal/sessions/{session_id}/connection`
/// 需携带内网共享密钥头 `X-Internal-Key`。
pub async fn fetch_connection_info(
    java_base: &str,
    internal_key: &str,
    session_id: i64,
) -> Result<ConnectionInfo> {
    let client = reqwest::Client::builder()
        .danger_accept_invalid_certs(true)
        .build()
        .map_err(|err| anyhow!("HTTP 客户端构建失败: {err}"))?;
    let url = format!("{java_base}/api/internal/sessions/{session_id}/connection");
    let envelope = client
        .get(&url)
        .header("X-Internal-Key", internal_key)
        .send()
        .await
        .map_err(|err| anyhow!("内网接口请求失败 {url}: {err}"))?
        .json::<ApiEnvelope<ConnectionInfo>>()
        .await
        .map_err(|err| anyhow!("内网接口响应解析失败: {err}"))?;

    if envelope.code != 0 {
        return Err(anyhow!("连接信息获取失败: {}", envelope.message));
    }
    envelope
        .data
        .ok_or_else(|| anyhow!("连接信息为空"))
}
