//! HTTP / WebSocket 服务组装。

use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    response::IntoResponse,
    routing::get,
    Json, Router,
};
use futures_util::{SinkExt, StreamExt};
use serde_json::json;
use tracing::{info, warn};

/// 组装应用路由。
pub fn app() -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/ws/terminal", get(ws_terminal))
}

async fn health() -> impl IntoResponse {
    Json(json!({
        "status": "ok",
        "service": "praesidium-core",
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

/// WebSocket 终端端点。
///
/// 当前为占位实现：把收到的输入原样回显，用于打通“浏览器 → 代理”链路。
/// TODO(阶段一): 接入 russh，建立到目标资产的 SSH 会话后双向透传。
async fn ws_terminal(ws: WebSocketUpgrade) -> impl IntoResponse {
    ws.on_upgrade(handle_terminal)
}

async fn handle_terminal(socket: WebSocket) {
    let (mut sender, mut receiver) = socket.split();

    let banner = "[praesidium] terminal placeholder: echo mode\r\n";
    if sender
        .send(Message::Text(banner.to_string().into()))
        .await
        .is_err()
    {
        return;
    }

    while let Some(result) = receiver.next().await {
        match result {
            Ok(Message::Text(text)) => {
                let input = text.as_str();
                info!(input = %input, "terminal input");
                if sender
                    .send(Message::Text(format!("{input}\r\n").into()))
                    .await
                    .is_err()
                {
                    break;
                }
            }
            Ok(Message::Binary(bytes)) => {
                info!(
                    len = bytes.len(),
                    "terminal binary input (ignored in placeholder mode)"
                );
            }
            Ok(Message::Close(_)) => break,
            Ok(_) => {}
            Err(err) => {
                warn!(%err, "terminal websocket error");
                break;
            }
        }
    }

    info!("terminal session closed");
    let _ = sender.close().await;
}
