//! SSH 网关：russh 客户端连接目标资产，暴露双向字节流（终端透传）。
//!
//! 通道所有权归内部 spawn 任务所有，外部通过命令通道（输入/调整/关闭）与输出流交互。

use anyhow::{anyhow, bail, Result};
use russh::client;
use russh::keys::{decode_secret_key, PublicKeyOrCertificate};
use russh::ChannelMsg;
use std::sync::Arc;
use tokio::io::AsyncWriteExt;
use tokio::sync::mpsc;

/// 宽松主机密钥策略：接受任意主机密钥（内部网络资产，生产可配置严格校验）。
struct SshHandler;

impl client::Handler for SshHandler {
    type Error = russh::Error;

    async fn check_server_key(
        &mut self,
        _server_public_key: &PublicKeyOrCertificate,
    ) -> Result<bool, Self::Error> {
        Ok(true)
    }
}

/// 目标资产认证方式（凭据由 Java 内网接口解密后下发）。
#[derive(Debug, Clone)]
pub enum CredentialAuth {
    Password(String),
    PrivateKey(String),
}

/// 会话控制命令（外部 → 内部任务）。
enum SessionCmd {
    Input(Vec<u8>),
    Resize(u32, u32),
    Close,
}

/// 已建立的 SSH 会话。
pub struct SshSession {
    cmd_tx: mpsc::Sender<SessionCmd>,
    /// 远端输出（终端回显）流
    pub rx: mpsc::Receiver<Vec<u8>>,
}

impl SshSession {
    /// 连接目标并打开交互 shell，内部任务负责双向转发。
    pub async fn connect(
        host: &str,
        port: u16,
        username: &str,
        auth: &CredentialAuth,
        cols: u32,
        rows: u32,
    ) -> Result<Self> {
        let config = Arc::new(client::Config::default());
        let mut handle = client::connect(config, (host, port), SshHandler)
            .await
            .map_err(|err| anyhow!("SSH 连接失败 {host}:{port}: {err}"))?;

        match auth {
            CredentialAuth::Password(password) => {
                let auth_result = handle
                    .authenticate_password(username, password)
                    .await
                    .map_err(|err| anyhow!("SSH 密码认证失败: {err}"))?;
                if !auth_result.success() {
                    bail!("SSH 密码认证被拒绝: {auth_result:?}");
                }
            }
            CredentialAuth::PrivateKey(pem) => {
                let key = decode_secret_key(pem, None)
                    .map_err(|err| anyhow!("SSH 私钥解析失败: {err}"))?;
                let key_with_alg = russh::keys::PrivateKeyWithHashAlg::new(Arc::new(key), None);
                let auth_result = handle
                    .authenticate_publickey(username, key_with_alg)
                    .await
                    .map_err(|err| anyhow!("SSH 密钥认证失败: {err}"))?;
                if !auth_result.success() {
                    bail!("SSH 密钥认证被拒绝: {auth_result:?}");
                }
            }
        }

        let mut channel = handle
            .channel_open_session()
            .await
            .map_err(|err| anyhow!("打开会话通道失败: {err}"))?;

        channel
            .request_pty(false, "xterm", cols, rows, 0, 0, &[])
            .await
            .map_err(|err| anyhow!("请求 PTY 失败: {err}"))?;
        channel
            .request_shell(false)
            .await
            .map_err(|err| anyhow!("请求 shell 失败: {err}"))?;

        let (cmd_tx, mut cmd_rx) = mpsc::channel::<SessionCmd>(256);
        let (out_tx, out_rx) = mpsc::channel::<Vec<u8>>(256);

        // 内部任务：单点持有 channel/handle，双向 select 转发
        tokio::spawn(async move {
            loop {
                tokio::select! {
                    cmd = cmd_rx.recv() => match cmd {
                        Some(SessionCmd::Input(data)) => {
                            if send_channel(&mut channel, &data).await.is_err() {
                                break;
                            }
                        }
                        Some(SessionCmd::Resize(cols, rows)) => {
                            if channel.window_change(cols, rows, 0, 0).await.is_err() {
                                break;
                            }
                        }
                        Some(SessionCmd::Close) | None => break,
                    },
                    msg = channel.wait() => match msg {
                        Some(ChannelMsg::Data { data }) => {
                            if out_tx.send(data.to_vec()).await.is_err() {
                                break;
                            }
                        }
                        Some(_) => continue, // 忽略控制消息
                        None => break,
                    },
                }
            }
            let _ = channel.close().await;
            let _ = handle
                .disconnect(russh::Disconnect::ByApplication, "", "English")
                .await;
        });

        Ok(Self { cmd_tx, rx: out_rx })
    }

    /// 终端输入写入远端。
    pub async fn send(&self, data: &[u8]) -> Result<()> {
        self.cmd_tx
            .send(SessionCmd::Input(data.to_vec()))
            .await
            .map_err(|_| anyhow!("SSH 会话已关闭"))
    }

    /// 终端尺寸变化同步到 PTY。
    pub async fn resize(&self, cols: u32, rows: u32) -> Result<()> {
        self.cmd_tx
            .send(SessionCmd::Resize(cols, rows))
            .await
            .map_err(|_| anyhow!("SSH 会话已关闭"))
    }

    /// 关闭会话。
    pub async fn close(&self) -> Result<()> {
        let _ = self.cmd_tx.send(SessionCmd::Close).await;
        Ok(())
    }
}

/// 向 SSH 通道写入数据（writer 用完即弃）。
async fn send_channel(channel: &russh::Channel<client::Msg>, data: &[u8]) -> Result<()> {
    let mut writer = channel.make_writer_ext(None);
    writer
        .write_all(data)
        .await
        .map_err(|err| anyhow!("SSH 写入失败: {err}"))?;
    writer
        .flush()
        .await
        .map_err(|err| anyhow!("SSH 刷新失败: {err}"))?;
    Ok(())
}
