//! 授权策略：gRPC 长连接接收 Java 推送（全量 + 增量），维护内存策略表并支持建连校验。

use std::collections::HashMap;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, RwLock};
use std::time::Duration;

use anyhow::{anyhow, Result};
use chrono::Local;
use tonic::transport::Channel;

pub mod pb {
    tonic::include_proto!("praesidium.policy.v1");
}

/// 内存中的有效策略。
#[derive(Debug, Clone)]
pub struct StoredPolicy {
    pub id: i64,
    pub name: String,
    pub user_id: i64,
    pub asset_ids: Vec<i64>,
    pub account_ids: Vec<i64>,
    pub protocol: String,
    pub valid_from: String,
    pub valid_to: String,
    pub status: String,
}

impl From<pb::Policy> for StoredPolicy {
    fn from(p: pb::Policy) -> Self {
        Self {
            id: p.id,
            name: p.name,
            user_id: p.user_id,
            asset_ids: p.asset_ids,
            account_ids: p.account_ids,
            protocol: p.protocol,
            valid_from: p.valid_from,
            valid_to: p.valid_to,
            status: p.status,
        }
    }
}

/// 策略表：读多写少，RwLock + HashMap。
pub struct PolicyStore {
    policies: RwLock<HashMap<i64, StoredPolicy>>,
    version: AtomicI64,
}

impl PolicyStore {
    pub fn new() -> Self {
        Self {
            policies: RwLock::new(HashMap::new()),
            version: AtomicI64::new(0),
        }
    }

    pub fn version(&self) -> i64 {
        self.version.load(Ordering::Acquire)
    }

    /// 应用一个策略事件（upsert / remove）。
    pub fn apply(&self, action: &str, policy: Option<pb::Policy>, version: i64) {
        let mut guard = self.policies.write().expect("策略表锁中毒");
        match action {
            "upsert" => {
                if let Some(p) = policy {
                    guard.insert(p.id, StoredPolicy::from(p));
                }
            }
            "remove" => {
                if let Some(p) = policy {
                    guard.remove(&p.id);
                }
            }
            _ => {}
        }
        self.version.store(version, Ordering::Release);
    }

    /// 建连授权校验：存在任一 active 策略放行即通过。
    pub fn authorize(
        &self,
        user_id: i64,
        asset_id: i64,
        account_id: Option<i64>,
        protocol: &str,
    ) -> bool {
        let today = Local::now().date_naive().to_string();
        let guard = self.policies.read().expect("策略表锁中毒");
        guard.values().any(|p| {
            if p.status != "active" || p.user_id != user_id {
                return false;
            }
            if !p.asset_ids.is_empty() && !p.asset_ids.contains(&asset_id) {
                return false;
            }
            if !p.account_ids.is_empty() {
                match account_id {
                    Some(id) if !p.account_ids.contains(&id) => return false,
                    None => return false,
                    _ => {}
                }
            }
            if !p.protocol.is_empty() && p.protocol != protocol {
                return false;
            }
            if !p.valid_from.is_empty() && p.valid_from > today {
                return false;
            }
            if !p.valid_to.is_empty() && p.valid_to < today {
                return false;
            }
            true
        })
    }

    pub fn count(&self) -> usize {
        self.policies.read().expect("策略表锁中毒").len()
    }
}

/// 策略流后台任务：建立 gRPC 长连接，断线重连（指数退避，上限 30s）。
pub async fn run_policy_stream(
    addr: String,
    store: Arc<PolicyStore>,
) {
    let mut backoff_secs = 1u64;
    loop {
        match connect_and_stream(&addr, Arc::clone(&store)).await {
            Ok(()) => {
                backoff_secs = 1;
                tracing::warn!("策略流断开，1s 后重连");
            }
            Err(err) => {
                tracing::warn!(%err, "策略流连接失败，{backoff_secs}s 后重试");
                tokio::time::sleep(Duration::from_secs(backoff_secs)).await;
                backoff_secs = (backoff_secs * 2).min(30);
            }
        }
    }
}

async fn connect_and_stream(addr: &str, store: Arc<PolicyStore>) -> Result<()> {
    let channel = Channel::from_shared(addr.to_string())
        .map_err(|err| anyhow!("gRPC 地址无效: {err}"))?
        .connect()
        .await
        .map_err(|err| anyhow!("gRPC 连接失败: {err}"))?;

    let mut client = pb::policy_service_client::PolicyServiceClient::new(channel);
    let since_version = store.version();
    let response = client
        .stream_policies(pb::StreamRequest { since_version })
        .await
        .map_err(|err| anyhow!("策略流开启失败: {err}"))?;
    let mut stream = response.into_inner();

    tracing::info!(since_version, "策略流已建立");
    while let Some(event) = stream
        .message()
        .await
        .map_err(|err| anyhow!("策略流中断: {err}"))?
    {
        store.apply(&event.action, event.policy, event.version);
        tracing::info!(
            action = %event.action,
            version = event.version,
            count = store.count(),
            "策略已更新"
        );
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn policy(id: i64, user_id: i64, asset_ids: Vec<i64>, account_ids: Vec<i64>) -> StoredPolicy {
        StoredPolicy {
            id,
            name: format!("p{id}"),
            user_id,
            asset_ids,
            account_ids,
            protocol: String::new(),
            valid_from: String::new(),
            valid_to: String::new(),
            status: "active".into(),
        }
    }

    #[test]
    fn authorize_rules() {
        let store = PolicyStore::new();
        store.apply("upsert", Some(pb::Policy {
            id: 1,
            name: "p1".into(),
            user_id: 1,
            asset_ids: vec![10],
            account_ids: vec![],
            protocol: String::new(),
            valid_from: String::new(),
            valid_to: String::new(),
            status: "active".into(),
        }), 1);
        store.apply("upsert", Some(pb::Policy {
            id: 2,
            name: "p2".into(),
            user_id: 2,
            asset_ids: vec![],
            account_ids: vec![5],
            protocol: String::new(),
            valid_from: String::new(),
            valid_to: String::new(),
            status: "active".into(),
        }), 2);

        // 用户 1 → 资产 10 放行
        assert!(store.authorize(1, 10, None, "SSH"));
        // 用户 1 → 资产 11 拒绝
        assert!(!store.authorize(1, 11, None, "SSH"));
        // 用户 2 → 任意资产 + 账号 5 放行
        assert!(store.authorize(2, 99, Some(5), "SSH"));
        // 用户 2 → 无账号绑定拒绝
        assert!(!store.authorize(2, 99, None, "SSH"));
        // 用户 3 无策略拒绝
        assert!(!store.authorize(3, 10, None, "SSH"));
    }

    #[test]
    fn remove_and_revoked() {
        let store = PolicyStore::new();
        store.apply("upsert", Some(pb::Policy {
            id: 1, name: "p1".into(), user_id: 1, asset_ids: vec![], account_ids: vec![],
            protocol: String::new(), valid_from: String::new(), valid_to: String::new(),
            status: "active".into(),
        }), 1);
        assert!(store.authorize(1, 1, None, "SSH"));
        // 撤销（改状态）
        store.apply("upsert", Some(pb::Policy {
            id: 1, name: "p1".into(), user_id: 1, asset_ids: vec![], account_ids: vec![],
            protocol: String::new(), valid_from: String::new(), valid_to: String::new(),
            status: "revoked".into(),
        }), 2);
        assert!(!store.authorize(1, 1, None, "SSH"));
        // 删除
        store.apply("remove", Some(pb::Policy {
            id: 1, name: String::new(), user_id: 0, asset_ids: vec![], account_ids: vec![],
            protocol: String::new(), valid_from: String::new(), valid_to: String::new(),
            status: String::new(),
        }), 3);
        assert_eq!(store.count(), 0);
    }
}
