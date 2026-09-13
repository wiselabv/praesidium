package io.github.wiselabv.praesidium.admin.identity.application;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoAuthorizeResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoProviderItem;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoProviderSaveRequest;
import io.github.wiselabv.praesidium.admin.identity.domain.SsoProviderRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.SsoProvider;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.security.SecretCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SSO 提供商配置应用服务：CRUD + 授权发起。
 *
 * <p>client secret 经 {@link SecretCipher} 加密落库；演示模式回调在
 * {@link AuthService} 中按第三方绑定定位用户（真实场景走 OAuth 令牌交换）。
 */
@Service
public class SsoProviderService {

    private final SsoProviderRepository ssoProviderRepository;
    private final SecretCipher secretCipher;
    private final SecureRandom secureRandom = new SecureRandom();

    public SsoProviderService(SsoProviderRepository ssoProviderRepository,
                              SecretCipher secretCipher) {
        this.ssoProviderRepository = ssoProviderRepository;
        this.secretCipher = secretCipher;
    }

    /** 全部提供商配置 */
    @Transactional(readOnly = true)
    public List<SsoProviderItem> list() {
        return ssoProviderRepository.findAll().stream().map(SsoProviderItem::from).toList();
    }

    /** 单提供商配置 */
    @Transactional(readOnly = true)
    public SsoProviderItem get(String provider) {
        return SsoProviderItem.from(requireProvider(provider));
    }

    /** 新增或更新提供商配置 */
    @Transactional
    public SsoProviderItem save(SsoProviderSaveRequest request) {
        String provider = request.provider().trim().toLowerCase();
        String secretEnc = null;
        if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
            secretEnc = secretCipher.encrypt(request.clientSecret());
        }
        SsoProvider existing = ssoProviderRepository.findByProvider(provider).orElse(null);
        if (existing == null) {
            existing = SsoProvider.create(provider, request.displayName().trim(), request.enabled(),
                    trimToNull(request.clientId()), secretEnc,
                    trimToNull(request.authorizeUrl()), trimToNull(request.tokenUrl()),
                    trimToNull(request.userinfoUrl()), trimToNull(request.scopes()),
                    trimToNull(request.extraConfig()));
        } else {
            existing.updateConfig(request.displayName().trim(), request.enabled(),
                    trimToNull(request.clientId()), secretEnc,
                    trimToNull(request.authorizeUrl()), trimToNull(request.tokenUrl()),
                    trimToNull(request.userinfoUrl()), trimToNull(request.scopes()),
                    trimToNull(request.extraConfig()));
        }
        ssoProviderRepository.save(existing);
        return SsoProviderItem.from(existing);
    }

    /** 删除提供商配置 */
    @Transactional
    public void delete(String provider) {
        SsoProvider existing = requireProvider(provider);
        ssoProviderRepository.delete(existing);
    }

    /** 发起授权：生成演示授权 URL（回调地址指向本服务 /api/auth/sso/{provider}/callback） */
    @Transactional(readOnly = true)
    public SsoAuthorizeResponse authorize(String provider) {
        SsoProvider config = requireProvider(provider);
        if (!config.isEnabled()) {
            throw new BizException(ApiErrorCode.AUTH_SSO_FAILED, "该登录方式未启用");
        }
        byte[] stateBytes = new byte[16];
        secureRandom.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        String baseUrl = config.getAuthorizeUrl() != null ? config.getAuthorizeUrl()
                : "https://sso.demo.praesidium.local/authorize";
        String authorizeUrl = baseUrl
                + (baseUrl.contains("?") ? "&" : "?")
                + "client_id=" + (config.getClientId() != null ? config.getClientId() : "demo")
                + "&redirect_uri=/api/auth/sso/" + provider + "/callback"
                + "&state=" + state
                + (config.getScopes() != null && !config.getScopes().isBlank()
                        ? "&scope=" + config.getScopes().trim() : "");
        return new SsoAuthorizeResponse(provider, config.getDisplayName(), authorizeUrl, state);
    }

    private SsoProvider requireProvider(String provider) {
        return ssoProviderRepository.findByProvider(provider)
                .orElseThrow(() -> new BizException(ApiErrorCode.SETTINGS_SECTION_NOT_FOUND,
                        "SSO 提供商不存在: " + provider));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
