-- V5：修复种子凭据——secret_encrypted 由占位值替换为真实 AES-256-GCM 密文（默认开发密钥），
-- secret_masked 移除明文密码泄露（密码类固定掩码，私钥类纯指纹）。
-- 明文内容仅作演示：Deploy@2026#Web / Svc@Pg#2026 / Win@Admin#2026 / 演示私钥占位文本。

UPDATE credentials SET
    secret_encrypted = 'enc:v1:fhNA7hKJyWyM9cv5:G53RDbXOtonkgTI6xqAPHVfH6Gv+KWkjrg7xkUSLYhrv9ay6X7wRI2IrkVyf',
    secret_masked    = 'ssh-ed25519 AAAA…zQm9'
WHERE name = 'web-root-密钥';

UPDATE credentials SET
    secret_encrypted = 'enc:v1:OBwl/nKc7za+9E7v:7ZCmc1G5H684oHCsHzMdJQh6+o8oECuqWLpwYkUYaw==',
    secret_masked    = '••••••••'
WHERE name = 'web-deploy-密码';

UPDATE credentials SET
    secret_encrypted = 'enc:v1:8Z7moGdp4CUrS5QR:hHG2CrVdHJgmdhUeoXQKq8M2KNAnrdb7wQthrZXTq47PNOGk2ryJqQs5wNo=',
    secret_masked    = 'ssh-ed25519 AAAA…xK7p'
WHERE name = 'pg-root-密钥';

UPDATE credentials SET
    secret_encrypted = 'enc:v1:2h4jsC8DKlkGd77T:LAxpNWRnab5kax4OOFTMIlNtY6W/lB+BLGRA',
    secret_masked    = '••••••••'
WHERE name = 'pg-svc-密码';

UPDATE credentials SET
    secret_encrypted = 'enc:v1:0RHaX9HHddkGzK47:iPwVn8og6R+HWfv6N/FvQo9xeSdJyhMmm7Y2FwBN',
    secret_masked    = '••••••••'
WHERE name = 'win-admin-密码';

-- 站点配置管理员邮箱与种子用户对齐
UPDATE settings SET config = jsonb_set(config, '{adminEmail}', '"admin@praesidium.local"')
WHERE section = 'basic';
