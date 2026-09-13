package io.github.wiselabv.praesidium.admin.identity.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.identity.domain.model.SsoProvider;

/** SSO 提供商配置条目（clientSecret 只回显是否配置，不回显明文） */
public record SsoProviderItem(
        Long id,
        String provider,
        String displayName,
        boolean enabled,
        String clientId,
        boolean secretConfigured,
        String authorizeUrl,
        String tokenUrl,
        String userinfoUrl,
        String scopes,
        String extraConfig,
        Instant updatedAt) {

    public static SsoProviderItem from(SsoProvider provider) {
        return new SsoProviderItem(
                provider.getId(),
                provider.getProvider(),
                provider.getDisplayName(),
                provider.isEnabled(),
                provider.getClientId(),
                provider.getClientSecretEnc() != null && !provider.getClientSecretEnc().isBlank(),
                provider.getAuthorizeUrl(),
                provider.getTokenUrl(),
                provider.getUserinfoUrl(),
                provider.getScopes(),
                provider.getExtraConfig(),
                provider.getUpdatedAt());
    }
}
