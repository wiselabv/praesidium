-- 认证扩展演示种子：个人信息 / 第三方绑定 / SSO 配置 / 安全设置 / 安全密钥
-- 依赖 SeedUsersRunner 播种的 admin（id=1）与 demo（id=2）

INSERT INTO user_profiles (user_id, avatar, phone, department, title, bio, last_login_at, last_login_ip, updated_at) VALUES
    (1, NULL, '138****8888', '运维部', '运维工程师', '负责生产环境与堡垒机管理。', now() - interval '2 hours', '192.168.1.15', now() - interval '2 hours'),
    (2, NULL, NULL, '研发部', '后端开发', '演示账号，用于验证两阶段登录。', now() - interval '1 day', '10.0.0.45', now() - interval '1 day');

INSERT INTO user_third_party (user_id, provider, provider_user_id, nickname, avatar_url, bound_at) VALUES
    (1, 'github', 'demo-github-8842', 'admin@github', NULL, now() - interval '90 days'),
    (1, 'dingtalk', 'demo-dingtalk-5521', 'admin@dingtalk', NULL, now() - interval '60 days');

INSERT INTO sso_providers (provider, display_name, enabled, client_id, client_secret_enc, authorize_url, token_url, userinfo_url, scopes, extra_config, updated_at) VALUES
    ('github', 'GitHub', TRUE, 'demo-github-client', 'enc:demo:placeholder-sso-github', 'https://github.com/login/oauth/authorize', 'https://github.com/login/oauth/access_token', 'https://api.github.com/user', 'read:user user:email', NULL, now() - interval '30 days'),
    ('oidc', 'OIDC 通用', FALSE, NULL, NULL, NULL, NULL, NULL, 'openid profile email', NULL, now() - interval '30 days'),
    ('ldap', 'LDAP / AD 域', FALSE, NULL, NULL, NULL, NULL, NULL, NULL, '{"serverUrl":"ldap://ad.internal.example.com:389","baseDn":"DC=example,DC=com","domain":"PRAESIDIUM"}', now() - interval '30 days'),
    ('dingtalk', '钉钉扫码', FALSE, 'demo-dingtalk-app', 'enc:demo:placeholder-sso-dingtalk', 'https://login.dingtalk.com/oauth2/auth', NULL, NULL, 'snsapi_login', NULL, now() - interval '30 days');

INSERT INTO user_security_settings (user_id, sms_phone, sms_enabled, email_mfa_enabled, updated_at) VALUES
    (1, '13800000000', TRUE, FALSE, now() - interval '30 days'),
    (2, NULL, FALSE, TRUE, now() - interval '10 days');

INSERT INTO webauthn_credentials (user_id, credential_id, public_key, sign_count, name, added_at) VALUES
    (1, 'demo-webauthn-windows-hello-001', 'demo-public-key-windows-hello', 128, 'Windows Hello', now() - interval '45 days'),
    (1, 'demo-webauthn-yubikey-5c-002', 'demo-public-key-yubikey-5c', 42, 'YubiKey 5C', now() - interval '20 days');
