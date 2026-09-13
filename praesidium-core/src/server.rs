//! HTTP / WebSocket 服务组装：终端透传全流程。
//!
//! WS 消息协议（JSON 帧，与前端 XtermTerminal 对齐）：
//! - 客户端 → 服务端：`{ type: 'auth', token }` / `{ type: 'input', data }` / `{ type: 'resize', cols, rows }`
//! - 服务端 → 客户端：终端原始输出（文本帧直接写入 xterm）；错误以 `[praesidium] ...` 文本回显。

use std::sync::Arc;
use std::time::Duration;

use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    response::IntoResponse,
    routing::get,
    Json, Router,
};
use futures_util::{SinkExt, StreamExt};
use serde::Deserialize;
use serde_json::json;
use tracing::{info, warn};

use crate::audit::{self, EventPublisher, SessionEndedData, SessionStartedData};
use crate::auth::TokenVerifier;
use crate::config::CoreConfig;
use crate::conninfo::fetch_connection_info;
use crate::gateway::SshSession;
use crate::policy::PolicyStore;
use crate::recorder::Recorder;

/// 服务共享状态。
pub struct AppState {
    pub config: CoreConfig,
    pub verifier: TokenVerifier,
    pub publisher: Arc<dyn EventPublisher>,
    pub policies: Arc<PolicyStore>,
}

/// 组装应用路由。
pub fn app(state: Arc<AppState>) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/ws/terminal", get(ws_terminal))
        .with_state(state)
}

async fn health() -> impl IntoResponse {
    Json(json!({
        "status": "ok",
        "service": "praesidium-core",
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

/// WebSocket 终端端点。
async fn ws_terminal(
    ws: WebSocketUpgrade,
    axum::extract::State(state): axum::extract::State<Arc<AppState>>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| handle_terminal(socket, state))
}

/// 客户端入站帧。
#[derive(Debug, Deserialize)]
#[serde(tag = "type", rename_all = "lowercase")]
enum ClientFrame {
    Auth { token: String },
    Input { data: String },
    Resize { cols: u32, rows: u32 },
}

async fn handle_terminal(socket: WebSocket, state: Arc<AppState>) {
    if let Err(err) = run_terminal(socket, &state).await {
        warn!(%err, "终端会话异常结束");
    }
}

async fn run_terminal(socket: WebSocket, state: &AppState) -> anyhow::Result<()> {
    let (mut sender, mut receiver) = socket.split();

    // 1. 等待 auth 帧（10s 超时）
    let token = tokio::time::timeout(Duration::from_secs(10), async {
        loop {
            match receiver.next().await {
                Some(Ok(Message::Text(text))) => {
                    if let Ok(ClientFrame::Auth { token }) = serde_json::from_str::<ClientFrame>(&text) {
                        return token;
                    }
                }
                Some(Ok(_)) => continue,
                _ => return String::new(),
            }
        }
    })
    .await
    .map_err(|_| anyhow::anyhow!("等待认证帧超时"))?;

    if token.is_empty() {
        let _ = sender
            .send(Message::Text("[praesidium] 认证失败：缺少网关令牌\r\n".into()))
            .await;
        return Ok(());
    }

    // 2. 验签网关令牌
    let claims = match state.verifier.verify(&token) {
        Ok(claims) => claims,
        Err(err) => {
            let _ = sender
                .send(Message::Text(format!("[praesidium] 认证失败：{err}\r\n").into()))
                .await;
            return Ok(());
        }
    };
    info!(
        session_id = claims.session_id,
        user_id = claims.user_id,
        "终端连接认证通过"
    );

    // 3. 从 Java 拉取连接信息（目标资产 + 解密凭据）
    let conn = match fetch_connection_info(
        &state.config.java_base,
        &state.config.internal_key,
        claims.session_id,
    )
    .await
    {
        Ok(conn) => conn,
        Err(err) => {
            let _ = sender
                .send(Message::Text(format!("[praesidium] 连接信息获取失败：{err}\r\n").into()))
                .await;
            return Ok(());
        }
    };

    // 4. 本地策略校验
    if !state.policies.authorize(
        claims.user_id,
        conn.asset_id,
        conn.account_id,
        &conn.protocol,
    ) {
        let _ = sender
            .send(Message::Text(
                "[praesidium] 授权拒绝：当前用户无该资产/账号的访问策略\r\n".into(),
            ))
            .await;
        let _ = audit::emit_session_ended(
            state.publisher.as_ref(),
            SessionEndedData {
                session_id: claims.session_id,
                user_id: claims.user_id,
                reason: "policy_denied".into(),
            },
        )
        .await;
        return Ok(());
    }

    // 5. 建立 SSH 会话
    let auth = match conn.to_auth() {
        Ok(auth) => auth,
        Err(err) => {
            let _ = sender
                .send(Message::Text(format!("[praesidium] 凭据无效：{err}\r\n").into()))
                .await;
            return Ok(());
        }
    };
    let mut ssh = match SshSession::connect(&conn.host, conn.port, &conn.account, &auth, 120, 30).await {
        Ok(ssh) => ssh,
        Err(err) => {
            let _ = sender
                .send(Message::Text(format!("[praesidium] SSH 连接失败：{err}\r\n").into()))
                .await;
            let _ = audit::emit_session_ended(
                state.publisher.as_ref(),
                SessionEndedData {
                    session_id: claims.session_id,
                    user_id: claims.user_id,
                    reason: "ssh_failed".into(),
                },
            )
            .await;
            return Ok(());
        }
    };

    let _ = audit::emit_session_started(
        state.publisher.as_ref(),
        SessionStartedData {
            session_id: claims.session_id,
            user_id: claims.user_id,
            asset_id: conn.asset_id,
            account: conn.account.clone(),
            protocol: conn.protocol.clone(),
        },
    )
    .await;
    info!(session_id = claims.session_id, "SSH 会话已建立，开始透传");

    // 6. 录像记录器
    let mut recorder = Recorder::new(
        claims.session_id,
        &state.config.minio_endpoint,
        &state.config.minio_access_key,
        &state.config.minio_secret_key,
        &state.config.minio_bucket,
        Arc::clone(&state.publisher),
    )
    .map_err(|err| anyhow::anyhow!("录像初始化失败: {err}"))?;

    // 命令行缓冲（用于审计事件提取）
    let mut line_buf: Vec<u8> = Vec::with_capacity(512);

    // 7. 双向透传循环
    let reason = loop {
        tokio::select! {
            frame = receiver.next() => {
                match frame {
                    Some(Ok(Message::Text(text))) => {
                        match serde_json::from_str::<ClientFrame>(&text) {
                            Ok(ClientFrame::Input { data }) => {
                                let bytes = data.into_bytes();
                                collect_command_lines(&bytes, &mut line_buf, &state, &claims, &conn);
                                if let Err(err) = ssh.send(&bytes).await {
                                    warn!(%err, "SSH 写入失败");
                                    break "ssh_write_error";
                                }
                            }
                            Ok(ClientFrame::Resize { cols, rows }) => {
                                if let Err(err) = ssh.resize(cols, rows).await {
                                    warn!(%err, "PTY 调整失败");
                                }
                            }
                            Ok(ClientFrame::Auth { .. }) => {} // 重复 auth 忽略
                            Err(_) => {} // 未知帧忽略
                        }
                    }
                    Some(Ok(Message::Binary(_))) | Some(Ok(Message::Ping(_))) | Some(Ok(Message::Pong(_))) => {}
                    Some(Ok(Message::Close(_))) | Some(Err(_)) | None => break "client_closed",
                }
            }
            out = ssh.rx.recv() => {
                match out {
                    Some(data) => {
                        recorder.feed(&data);
                        if sender.send(Message::Text(String::from_utf8_lossy(&data).into_owned().into())).await.is_err() {
                            break "client_closed";
                        }
                    }
                    None => break "ssh_closed",
                }
            }
        }
    };

    // 8. 收尾：结束事件 + 录像落盘 + 关闭会话
    info!(session_id = claims.session_id, reason, "终端会话结束");
    recorder.flush();
    let _ = ssh.close().await;
    let _ = audit::emit_session_ended(
        state.publisher.as_ref(),
        SessionEndedData {
            session_id: claims.session_id,
            user_id: claims.user_id,
            reason: reason.to_string(),
        },
    )
    .await;
    let _ = sender.close().await;
    Ok(())
}

/// 从输入字节流提取命令行（遇回车提交），发布命令审计事件。
fn collect_command_lines(
    bytes: &[u8],
    line_buf: &mut Vec<u8>,
    state: &AppState,
    claims: &crate::auth::GatewayClaims,
    conn: &crate::conninfo::ConnectionInfo,
) {
    for &b in bytes {
        match b {
            b'\r' | b'\n' => {
                if let Some(command) = clean_command(line_buf) {
                    let data = audit::CommandData {
                        session_id: claims.session_id,
                        user_id: claims.user_id,
                        asset_id: conn.asset_id,
                        account: conn.account.clone(),
                        command,
                    };
                    // 事件发布不阻塞输入流（fire-and-forget）
                    let publisher = Arc::clone(&state.publisher);
                    tokio::spawn(async move {
                        if let Err(err) = audit::emit_command(publisher.as_ref(), data).await {
                            warn!(%err, "命令事件发布失败");
                        }
                    });
                }
                line_buf.clear();
            }
            0x7f => {
                line_buf.pop();
            }
            0x03 => line_buf.clear(), // Ctrl+C 丢弃当前行
            other => {
                if line_buf.len() < 512 {
                    line_buf.push(other);
                }
            }
        }
    }
}

/// 清洗命令行：剥离 ESC 转义序列、去控制字符与 ANSI 转义，截断到 256 字符。
fn clean_command(line: &[u8]) -> Option<String> {
    // 先剥离 ESC 序列（含 xterm.js bracketed paste 标记 \x1b[200~ / \x1b[201~），
    // 否则 ESC 被控制字符过滤后其标记残片（如 "[200~"）会混入命令文本。
    let stripped = strip_escape_sequences(line);
    let text = String::from_utf8_lossy(&stripped);
    let cleaned: String = text
        .chars()
        .filter(|c| !c.is_control() || c.is_whitespace())
        .collect();
    let trimmed = cleaned.trim();
    if trimmed.is_empty() {
        None
    } else {
        Some(trimmed.chars().take(256).collect())
    }
}

/// 剥离字节流中的 ESC 转义序列：CSI（ESC [ ... 0x40..=0x7e）与 SS3（ESC O + 1 字节）。
fn strip_escape_sequences(bytes: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == 0x1b && i + 1 < bytes.len() {
            match bytes[i + 1] {
                b'[' => {
                    // CSI：参数字节 0x20..=0x3f、中间字节 0x20..=0x2f，终结字节 0x40..=0x7e
                    i += 2;
                    while i < bytes.len() && !(0x40..=0x7e).contains(&bytes[i]) {
                        i += 1;
                    }
                    if i < bytes.len() {
                        i += 1; // 吞掉终结字节
                    }
                    continue;
                }
                b'O' => {
                    // SS3：ESC O 后跟单个最终字节（如方向键 ESC OA）
                    i += 3.min(bytes.len() - i);
                    continue;
                }
                _ => {}
            }
        }
        out.push(bytes[i]);
        i += 1;
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn extracts_command_lines() {
        let mut buf = Vec::new();
        let input = b"ls -la\rcd /tmp\x7f\x7f\x7f\r";
        let commands: Vec<String> = input
            .iter()
            .filter_map(|&b| match b {
                b'\r' | b'\n' => {
                    let cmd = clean_command(&buf);
                    buf.clear();
                    cmd
                }
                0x7f => {
                    buf.pop();
                    None
                }
                other => {
                    buf.push(other);
                    None
                }
            })
            .collect();
        assert_eq!(commands, vec!["ls -la".to_string(), "cd /".to_string()]);
    }

    #[test]
    fn strips_bracketed_paste_markers() {
        // xterm.js 粘贴时包裹 \x1b[200~ ... \x1b[201~ 标记
        let line = b"\x1b[200~rm -rf /tmp/test\x1b[201~";
        assert_eq!(clean_command(line), Some("rm -rf /tmp/test".to_string()));
    }

    #[test]
    fn strips_cursor_key_sequences() {
        // 方向键 ESC OA 不应残留任何字符
        let line = b"who\x1b[Dam\x1b[Ci";
        assert_eq!(clean_command(line), Some("whoami".to_string()));
    }

    #[test]
    fn drops_control_only_lines() {
        assert_eq!(clean_command(b"\x1b[200~\x1b[201~"), None);
        assert_eq!(clean_command(b"\x1b"), None);
    }
}
