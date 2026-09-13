-- 业务演示种子：assets 表为空时播种，让各页面与总览有真实数据可查
-- 敏感占位：凭据 secret_encrypted 为演示占位串（真实写入经应用层 AES 加密）

INSERT INTO credentials (name, type, secret_encrypted, secret_masked, created_at, updated_at) VALUES
    ('web-root-密钥', 'key', 'enc:demo:placeholder-1', 'ssh-ed25519 AAAA…zQm9 生产私钥', now() - interval '60 days', now() - interval '11 days'),
    ('web-deploy-密码', 'password', 'enc:demo:placeholder-2', 'Deploy@2026#Web', now() - interval '30 days', now() - interval '5 days'),
    ('pg-root-密钥', 'key', 'enc:demo:placeholder-3', 'ssh-ed25519 AAAA…xK7p 数据库私钥', now() - interval '45 days', now() - interval '20 days'),
    ('pg-svc-密码', 'password', 'enc:demo:placeholder-4', 'Svc@Pg#2026', now() - interval '40 days', now() - interval '9 days'),
    ('win-admin-密码', 'password', 'enc:demo:placeholder-5', 'Win@Admin#2026', now() - interval '25 days', now() - interval '3 days');

INSERT INTO assets (name, type, address, protocol, port, group_path, description, status, created_by, created_at, updated_at) VALUES
    ('web-prod-01', 'host', '10.0.1.21', 'SSH', 22, '生产环境/Web', 'Web 生产主机 01', 'active', 1, now() - interval '120 days', now() - interval '10 days'),
    ('web-prod-02', 'host', '10.0.1.22', 'SSH', 22, '生产环境/Web', 'Web 生产主机 02', 'active', 1, now() - interval '120 days', now() - interval '8 days'),
    ('pg-master', 'db', '10.0.3.10', 'PostgreSQL', 5432, '生产环境/数据库', 'PostgreSQL 主库', 'active', 1, now() - interval '100 days', now() - interval '6 days'),
    ('mysql-prod', 'db', '10.0.3.11', 'MySQL', 3306, '生产环境/数据库', 'MySQL 业务库', 'active', 1, now() - interval '100 days', now() - interval '15 days'),
    ('redis-cache-01', 'db', '10.0.3.20', 'Redis', 6379, '生产环境/缓存', '缓存集群节点 01', 'active', 1, now() - interval '90 days', now() - interval '2 days'),
    ('win-jump-01', 'host', '10.0.2.15', 'RDP', 3389, '办公区/Windows', 'Windows 跳板机', 'active', 1, now() - interval '80 days', now() - interval '4 days'),
    ('bastion-02', 'host', '10.0.0.2', 'SSH', 22, '基础设施/堡垒机', '堡垒机备机', 'active', 1, now() - interval '150 days', now() - interval '30 days'),
    ('db-slave-01', 'db', '10.0.3.12', 'PostgreSQL', 5432, '生产环境/数据库', 'PostgreSQL 从库', 'active', 1, now() - interval '95 days', now() - interval '12 days'),
    ('build-svr', 'host', '10.0.5.8', 'SSH', 22, '研发区/CI', 'CI 构建服务器', 'active', 1, now() - interval '70 days', now() - interval '1 days'),
    ('gitlab', 'web', 'https://gitlab.internal.example.com', 'HTTPS', NULL, '研发区/工具', 'GitLab 代码托管', 'active', 1, now() - interval '60 days', now() - interval '7 days'),
    ('jenkins', 'web', 'http://ci.internal.example.com:8080', 'HTTP', NULL, '研发区/CI', 'Jenkins 持续集成', 'active', 1, now() - interval '60 days', now() - interval '7 days'),
    ('k8s-master-01', 'host', '10.0.4.10', 'SSH', 22, '生产环境/容器', 'Kubernetes 主节点', 'active', 1, now() - interval '50 days', now() - interval '3 days');

INSERT INTO asset_accounts (asset_id, name, type, privileged, source, credential_id, enabled, created_at, updated_at) VALUES
    (1, 'root', 'root', TRUE, 'discovery', 1, TRUE, now() - interval '118 days', now() - interval '10 days'),
    (1, 'deploy', 'service', FALSE, 'manual', 2, TRUE, now() - interval '100 days', now() - interval '8 days'),
    (2, 'root', 'root', TRUE, 'discovery', 1, TRUE, now() - interval '118 days', now() - interval '10 days'),
    (2, 'deploy', 'service', FALSE, 'manual', 2, TRUE, now() - interval '100 days', now() - interval '8 days'),
    (3, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '95 days', now() - interval '6 days'),
    (3, 'postgres', 'service', FALSE, 'manual', 4, TRUE, now() - interval '90 days', now() - interval '5 days'),
    (4, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '95 days', now() - interval '10 days'),
    (5, 'default', 'service', FALSE, 'manual', NULL, TRUE, now() - interval '85 days', now() - interval '2 days'),
    (6, 'Administrator', 'admin', TRUE, 'discovery', 5, TRUE, now() - interval '75 days', now() - interval '4 days'),
    (7, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '140 days', now() - interval '30 days'),
    (8, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '90 days', now() - interval '12 days'),
    (9, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '65 days', now() - interval '1 days'),
    (10, 'root', 'root', TRUE, 'discovery', 3, TRUE, now() - interval '55 days', now() - interval '7 days'),
    (12, 'ops', 'normal', FALSE, 'manual', NULL, FALSE, now() - interval '45 days', now() - interval '3 days');

INSERT INTO access_policies (name, user_id, asset_ids, account_ids, protocol, valid_from, valid_to, status, description, created_at, updated_at) VALUES
    ('web-prod 临时授权', 1, '[1,2]', '[2]', 'SSH', now() - interval '1 hour', now() + interval '4 hours', 'pending', 'root 账号 · 临时授权 4h', now() - interval '1 hour', now() - interval '1 hour'),
    ('build-svr 部署授权', 1, '[9]', '[13]', 'SSH', now() - interval '1 day', now() + interval '90 days', 'pending', '部署账号 · 长期', now() - interval '1 day', now() - interval '1 day'),
    ('pg-master 只读授权', 1, '[3]', '[6]', 'PostgreSQL', now() - interval '2 hours', now() + interval '7 days', 'pending', '只读账号 · 7 天', now() - interval '2 hours', now() - interval '2 hours'),
    ('数据库运维常规授权', 1, '[3,4,5,8]', '[5,7,8]', NULL, now() - interval '30 days', now() + interval '335 days', 'active', 'dba 组数据库运维', now() - interval '30 days', now() - interval '30 days');

INSERT INTO sessions (user_id, asset_id, account_id, protocol, status, source_ip, started_at, ended_at, recording_path, recording_size_bytes) VALUES
    (1, 1, 2, 'SSH', 'online', '192.168.1.15', now() - interval '36 minutes', NULL, NULL, NULL),
    (1, 3, 6, 'PostgreSQL', 'online', '10.0.0.45', now() - interval '12 minutes', NULL, NULL, NULL),
    (1, 7, 10, 'SSH', 'online', '172.16.3.9', now() - interval '56 minutes', NULL, NULL, NULL),
    (1, 6, 9, 'RDP', 'online', '10.0.0.2', now() - interval '3 minutes', NULL, NULL, NULL),
    (1, 9, 13, 'SSH', 'online', '192.168.1.88', now() - interval '5 minutes', NULL, NULL, NULL),
    (1, 1, 1, 'SSH', 'ended', '192.168.1.15', now() - interval '2 hours', now() - interval '1 hour 35 minutes', 'rec/10240.cast', 18874368),
    (1, 3, 5, 'PostgreSQL', 'ended', '192.168.1.15', now() - interval '5 hours', now() - interval '4 hours 40 minutes', 'rec/10236.cast', 5242880),
    (1, 6, 9, 'RDP', 'ended', '192.168.1.15', now() - interval '1 day', now() - interval '1 day' + interval '50 minutes', 'rec/10230.cast', 268435456),
    (1, 9, 13, 'SSH', 'ended', '192.168.1.88', now() - interval '2 days', now() - interval '2 days' + interval '1 hour', 'rec/10218.cast', 10485760);

INSERT INTO audit_logs (user_id, asset_id, account_id, action, command_detail, result, risk, source_ip, created_at) VALUES
    (1, 8, 11, 'sql.execute', 'SELECT count(*) FROM pg_stat_activity;', 'success', 'medium', '10.0.0.2', now() - interval '15 minutes'),
    (1, 1, 2, 'file.download', 'scp /etc/nginx/nginx.conf', 'blocked', 'high', '192.168.1.15', now() - interval '30 minutes'),
    (1, 7, 10, 'shell.exec', 'crontab -l && systemctl list-units --type=service', 'success', 'medium', '192.168.1.15', now() - interval '1 hour'),
    (1, 1, 1, 'shell.exec', 'sudo rm -rf /var/log/nginx/*', 'blocked', 'high', '192.168.1.15', now() - interval '2 hours'),
    (1, 9, 13, 'shell.exec', 'docker system prune -af', 'success', 'high', '192.168.1.88', now() - interval '3 hours'),
    (1, 3, 5, 'sql.execute', 'UPDATE users SET status = 0 WHERE id = 42', 'blocked', 'high', '192.168.1.15', now() - interval '5 hours'),
    (1, 2, 3, 'shell.exec', 'tail -f /var/log/app/error.log', 'success', 'low', '192.168.1.15', now() - interval '8 hours'),
    (1, 6, 9, 'shell.exec', 'powershell Get-EventLog -LogName Security', 'success', 'medium', '192.168.1.15', now() - interval '1 day');

INSERT INTO login_logs (user_id, username, source_ip, location, method, result, reason, client, created_at) VALUES
    (1, 'admin', '192.168.1.15', '上海', 'password', 'success', NULL, 'Chrome/Windows', now() - interval '2 hours'),
    (1, 'ops-admin', '10.0.0.45', '北京', 'password', 'success', NULL, 'Chrome/macOS', now() - interval '4 hours'),
    (1, 'dba-li', '172.16.3.9', '深圳', 'code', 'success', NULL, 'Firefox/Linux', now() - interval '6 hours'),
    (1, 'sec-audit', '10.0.0.2', '上海', 'sso', 'success', NULL, 'Edge/Windows', now() - interval '1 day'),
    (1, 'dev-zhang', '192.168.1.88', '杭州', 'password', 'success', NULL, 'Chrome/Windows', now() - interval '1 day'),
    (NULL, 'root', '203.0.113.7', '未知', 'password', 'failed', '用户名或密码错误', 'python-requests', now() - interval '3 hours'),
    (NULL, 'admin', '198.51.100.23', '未知', 'password', 'failed', '用户名或密码错误', 'curl/8.0', now() - interval '5 hours'),
    (NULL, 'oracle', '203.0.113.99', '未知', 'password', 'failed', '账号不存在', 'nmap', now() - interval '1 day');

INSERT INTO settings (section, config) VALUES
    ('basic', '{"siteName":"Praesidium 堡垒机","adminEmail":"admin@example.com","sessionTimeout":30,"timezone":"Asia/Shanghai","loginBanner":"本系统仅限授权人员使用，所有操作将被审计记录。"}'),
    ('security', '{"passwordMinLength":12,"requireComplexity":true,"passwordExpireDays":90,"lockThreshold":5,"lockMinutes":15,"idleTimeout":15,"ipWhitelist":""}'),
    ('notify', '{"emailEnabled":true,"smtpHost":"smtp.example.com","smtpPort":465,"smtpUser":"alert@example.com","smtpSsl":true,"webhookEnabled":false,"webhookUrl":""}'),
    ('auth', '{"mfaRequired":true,"ssoGithub":true,"ssoLdap":false,"ssoOidc":false,"captchaOnFailed":true}'),
    ('storage', '{"recordingRetentionDays":180,"logRetentionDays":365,"recordingPath":"/data/praesidium/recordings","alertRetentionDays":90}');
