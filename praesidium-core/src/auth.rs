//! 网关令牌验签：Java 管理服务签发（HS256，共享密钥），Rust 验签后获得会话上下文。

use anyhow::{anyhow, Result};
use jsonwebtoken::{decode, DecodingKey, Validation};
use serde::{Deserialize, Serialize};

/// 验签后的会话上下文。
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct GatewayClaims {
    /// 会话 ID（sessions 表主键，Java 侧签发）
    pub session_id: i64,
    /// 发起会话的用户 ID
    pub user_id: i64,
    /// 标准声明
    #[serde(default)]
    pub sub: String,
    #[serde(default)]
    pub exp: usize,
}

/// JWT 验签器（HS256）。
pub struct TokenVerifier {
    key: DecodingKey,
}

impl TokenVerifier {
    pub fn new(secret: &str) -> Self {
        Self {
            key: DecodingKey::from_secret(secret.as_bytes()),
        }
    }

    /// 校验签名与过期时间，返回会话上下文。
    pub fn verify(&self, token: &str) -> Result<GatewayClaims> {
        let mut validation = Validation::default();
        validation.validate_exp = true;
        let data = decode::<GatewayClaims>(token, &self.key, &validation)
            .map_err(|err| anyhow!("网关令牌无效: {err}"))?;
        Ok(data.claims)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use jsonwebtoken::{encode, EncodingKey, Header};

    #[test]
    fn roundtrip_verify() {
        let secret = "test-secret-0123456789";
        let claims = GatewayClaims {
            session_id: 42,
            user_id: 1,
            sub: "1".into(),
            exp: (chrono::Utc::now().timestamp() + 60) as usize,
        };
        let token = encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret(secret.as_bytes()),
        )
        .unwrap();
        let verified = TokenVerifier::new(secret).verify(&token).unwrap();
        assert_eq!(verified.session_id, 42);
        assert_eq!(verified.user_id, 1);
    }

    #[test]
    fn rejects_wrong_secret() {
        let claims = GatewayClaims {
            session_id: 1,
            user_id: 1,
            sub: "1".into(),
            exp: (chrono::Utc::now().timestamp() + 60) as usize,
        };
        let token = encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret("secret-a".as_bytes()),
        )
        .unwrap();
        assert!(TokenVerifier::new("secret-b").verify(&token).is_err());
    }
}
