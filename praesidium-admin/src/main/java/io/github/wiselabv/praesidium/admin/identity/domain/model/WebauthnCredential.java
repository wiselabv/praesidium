package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * WebAuthn / Passkey 设备实体：演示注册保存凭据元数据（真实场景由网关完成签名校验）。
 */
@Entity
@Table(name = "webauthn_credentials")
public class WebauthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "credential_id", nullable = false, unique = true, length = 255)
    private String credentialId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "sign_count", nullable = false)
    private Long signCount;

    @Column(length = 64)
    private String name;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /** JPA 要求 */
    protected WebauthnCredential() {
    }

    private WebauthnCredential(Long userId, String credentialId, String publicKey, String name) {
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.signCount = 0L;
        this.name = name;
        this.addedAt = Instant.now();
    }

    /** 注册一把安全密钥 */
    public static WebauthnCredential register(Long userId, String credentialId,
                                              String publicKey, String name) {
        return new WebauthnCredential(userId, credentialId, publicKey, name);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Long getSignCount() {
        return signCount;
    }

    public String getName() {
        return name;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
