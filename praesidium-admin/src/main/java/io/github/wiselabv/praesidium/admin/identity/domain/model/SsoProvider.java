package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * SSO 提供商实体：OAuth / OIDC / SAML / CAS 等外部身份源的客户端配置。
 *
 * <p>client_secret 经应用层 AES 加密后落库（clientSecretEnc），
 * extra_config 以 JSONB 存各提供商的扩展参数（如 SAML metadata、钉钉 corpId）。
 */
@Entity
@Table(name = "sso_providers")
public class SsoProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String provider;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "client_secret_enc", columnDefinition = "TEXT")
    private String clientSecretEnc;

    @Column(name = "authorize_url", length = 255)
    private String authorizeUrl;

    @Column(name = "token_url", length = 255)
    private String tokenUrl;

    @Column(name = "userinfo_url", length = 255)
    private String userinfoUrl;

    @Column(length = 255)
    private String scopes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private String extraConfig;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected SsoProvider() {
    }

    private SsoProvider(String provider, String displayName, boolean enabled, String clientId,
                        String clientSecretEnc, String authorizeUrl, String tokenUrl,
                        String userinfoUrl, String scopes, String extraConfig) {
        this.provider = provider;
        this.displayName = displayName;
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecretEnc = clientSecretEnc;
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.userinfoUrl = userinfoUrl;
        this.scopes = scopes;
        this.extraConfig = extraConfig;
        this.updatedAt = Instant.now();
    }

    /** 新建提供商配置 */
    public static SsoProvider create(String provider, String displayName, boolean enabled,
                                     String clientId, String clientSecretEnc, String authorizeUrl,
                                     String tokenUrl, String userinfoUrl, String scopes,
                                     String extraConfig) {
        return new SsoProvider(provider, displayName, enabled, clientId, clientSecretEnc,
                authorizeUrl, tokenUrl, userinfoUrl, scopes, extraConfig);
    }

    /** 整体替换配置（clientSecret 为 null 时保留原值） */
    public void updateConfig(String displayName, boolean enabled, String clientId,
                             String clientSecretEnc, String authorizeUrl, String tokenUrl,
                             String userinfoUrl, String scopes, String extraConfig) {
        this.displayName = displayName;
        this.enabled = enabled;
        this.clientId = clientId;
        if (clientSecretEnc != null) {
            this.clientSecretEnc = clientSecretEnc;
        }
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.userinfoUrl = userinfoUrl;
        this.scopes = scopes;
        this.extraConfig = extraConfig;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecretEnc() {
        return clientSecretEnc;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getUserinfoUrl() {
        return userinfoUrl;
    }

    public String getScopes() {
        return scopes;
    }

    public String getExtraConfig() {
        return extraConfig;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
