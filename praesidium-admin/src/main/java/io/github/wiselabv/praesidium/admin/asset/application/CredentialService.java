package io.github.wiselabv.praesidium.admin.asset.application;

import java.util.List;
import java.util.Map;

import io.github.wiselabv.praesidium.admin.asset.application.dto.CredentialItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.CredentialRequest;
import io.github.wiselabv.praesidium.admin.asset.application.dto.SecretRevealResponse;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.CredentialRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import io.github.wiselabv.praesidium.admin.shared.security.SecretCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 凭据应用服务：加密落库、脱敏下发、单次明文查看。
 */
@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final AssetAccountRepository accountRepository;
    private final SecretCipher secretCipher;

    public CredentialService(CredentialRepository credentialRepository,
                             AssetAccountRepository accountRepository,
                             SecretCipher secretCipher) {
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
        this.secretCipher = secretCipher;
    }

    @Transactional(readOnly = true)
    public PageResponse<CredentialItem> list(String keyword, int page, int size) {
        List<Credential> credentials = credentialRepository.findPage(keyword, (page - 1) * size, size);
        long total = credentialRepository.countPage(keyword);
        Map<Long, Long> bound = accountRepository.countGroupByCredential(
                credentials.stream().map(Credential::getId).toList());
        List<CredentialItem> items = credentials.stream()
                .map(c -> CredentialItem.from(c, bound.getOrDefault(c.getId(), 0L)))
                .toList();
        return PageResponse.of(items, total, page, size);
    }

    @Transactional
    public CredentialItem create(CredentialRequest request) {
        String secret = requireSecret(request);
        String type = request.type().trim();
        Credential credential = Credential.create(request.name().trim(), type,
                secretCipher.encrypt(secret), maskByType(type, secret));
        credentialRepository.save(credential);
        return CredentialItem.from(credential, 0);
    }

    @Transactional
    public CredentialItem update(Long id, CredentialRequest request) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.CREDENTIAL_NOT_FOUND, "凭据不存在"));
        String type = request.type().trim();
        credential.update(request.name().trim(), type);
        if (request.secret() != null && !request.secret().isBlank()) {
            credential.rotate(secretCipher.encrypt(request.secret()), maskByType(type, request.secret()));
        }
        credentialRepository.save(credential);
        long bound = accountRepository.countByCredentialId(id);
        return CredentialItem.from(credential, bound);
    }

    @Transactional
    public void delete(Long id) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.CREDENTIAL_NOT_FOUND, "凭据不存在"));
        long bound = accountRepository.countByCredentialId(id);
        if (bound > 0) {
            throw new BizException(ApiErrorCode.CREDENTIAL_IN_USE,
                    "凭据仍被 " + bound + " 个账号引用，请先解除绑定");
        }
        credentialRepository.delete(credential);
    }

    /** 单次明文查看：解密下发，前端展示后即弃 */
    public SecretRevealResponse reveal(Long id) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.CREDENTIAL_NOT_FOUND, "凭据不存在"));
        String secret = secretCipher.decrypt(credential.getSecretEncrypted());
        if (secret == null) {
            throw new BizException(ApiErrorCode.INTERNAL_ERROR,
                    "凭据解密失败（占位数据或加密密钥变更），请通过编辑重置凭据内容");
        }
        return new SecretRevealResponse(secret);
    }

    private String requireSecret(CredentialRequest request) {
        if (request.secret() == null || request.secret().isBlank()) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "凭据内容不能为空");
        }
        return request.secret();
    }

    /** 脱敏展示：私钥保留头部指纹，密码/令牌固定掩码 */
    private String maskByType(String type, String secret) {
        if ("key".equals(type)) {
            return secretCipher.mask(secret);
        }
        return "••••••••";
    }
}
