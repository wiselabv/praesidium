package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 第三方登录绑定实体：同一用户同一提供商只能绑定一次。
 *
 * <p>provider 取值：github / oidc / saml / cas / dingtalk / feishu / wecom。
 */
@Entity
@Table(name = "user_third_party")
public class UserThirdParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String provider;

    @Column(name = "provider_user_id", length = 128)
    private String providerUserId;

    @Column(length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    /** JPA 要求 */
    protected UserThirdParty() {
    }

    private UserThirdParty(Long userId, String provider, String providerUserId,
                           String nickname, String avatarUrl) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.boundAt = Instant.now();
    }

    /** 绑定一个第三方账号 */
    public static UserThirdParty bind(Long userId, String provider, String providerUserId,
                                      String nickname, String avatarUrl) {
        return new UserThirdParty(userId, provider, providerUserId, nickname, avatarUrl);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getBoundAt() {
        return boundAt;
    }
}
