package io.github.wiselabv.praesidium.admin.sessions.application;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.access.domain.AccessPolicyRepository;
import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.CredentialRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectionInfo;
import io.github.wiselabv.praesidium.admin.sessions.domain.SessionRepository;
import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.security.SecretCipher;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内网会话连接信息服务：供 Rust 网关拉取解密凭据与目标地址。
 *
 * <p>这是网关信任边界内的敏感接口：调用方必须持有内网共享密钥
 * （{@code X-Internal-Key}），且返回前独立校验授权策略（不依赖网关侧校验）。
 */
@Service
public class InternalSessionService {

    private final SessionRepository sessionRepository;
    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;
    private final CredentialRepository credentialRepository;
    private final AccessPolicyRepository policyRepository;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;

    public InternalSessionService(SessionRepository sessionRepository,
                                  AssetRepository assetRepository,
                                  AssetAccountRepository accountRepository,
                                  CredentialRepository credentialRepository,
                                  AccessPolicyRepository policyRepository,
                                  SecretCipher secretCipher,
                                  ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.policyRepository = policyRepository;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
    }

    /** 拉取会话连接信息（解密凭据 + 授权校验） */
    @Transactional(readOnly = true)
    public ConnectionInfo connectionInfo(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException(ApiErrorCode.SESSION_NOT_FOUND, "会话不存在"));
        if (!"online".equals(session.getStatus())) {
            throw new BizException(ApiErrorCode.SESSION_NOT_FOUND, "会话已结束");
        }
        Asset asset = assetRepository.findById(session.getAssetId())
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        AssetAccount account = null;
        if (session.getAccountId() != null) {
            account = accountRepository.findById(session.getAccountId())
                    .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_ACCOUNT_NOT_FOUND, "资产账号不存在"));
        }

        // 授权策略校验（内网接口是敏感边界，必须独立校验）
        requireAuthorized(session, asset, account);

        // 解密凭据
        String authType = "password";
        String secret = "";
        String accountName = account == null ? "" : account.getName();
        if (account != null && account.getCredentialId() != null) {
            Credential credential = credentialRepository.findById(account.getCredentialId())
                    .orElseThrow(() -> new BizException(ApiErrorCode.CREDENTIAL_NOT_FOUND, "账号凭据不存在"));
            authType = credential.getType() == null ? "password" : credential.getType();
            secret = secretCipher.decrypt(credential.getSecretEncrypted());
            if (secret == null) {
                throw new BizException(ApiErrorCode.INTERNAL_ERROR, "凭据解密失败");
            }
        }

        int port = asset.getPort() == null ? defaultPort(asset.getProtocol()) : asset.getPort();
        return new ConnectionInfo(asset.getId(), asset.getAddress(), port,
                asset.getProtocol(), accountName,
                account == null ? null : account.getId(), authType, secret);
    }

    /** 与 Rust 网关一致的策略匹配语义：任一 active 策略放行即通过 */
    private void requireAuthorized(Session session, Asset asset, AssetAccount account) {
        List<AccessPolicy> policies = policyRepository.findByStatus("active");
        Instant now = Instant.now();
        for (AccessPolicy policy : policies) {
            if (!policy.getUserId().equals(session.getUserId())) {
                continue;
            }
            if (policy.getProtocol() != null && !policy.getProtocol().isBlank()
                    && !policy.getProtocol().equals(session.getProtocol())) {
                continue;
            }
            List<Long> assetIds = parseIds(policy.getAssetIds());
            if (!assetIds.isEmpty() && !assetIds.contains(asset.getId())) {
                continue;
            }
            List<Long> accountIds = parseIds(policy.getAccountIds());
            if (!accountIds.isEmpty()) {
                if (session.getAccountId() == null || !accountIds.contains(session.getAccountId())) {
                    continue;
                }
            }
            if (policy.getValidFrom() != null && policy.getValidFrom().isAfter(now)) {
                continue;
            }
            if (policy.getValidTo() != null && policy.getValidTo().isBefore(now)) {
                continue;
            }
            return; // 命中任一 active 策略
        }
        throw new BizException(ApiErrorCode.POLICY_DENIED, "无该资产的访问授权策略");
    }

    private int defaultPort(String protocol) {
        return "ssh".equals(protocol) ? 22 : 3389;
    }

    private List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
