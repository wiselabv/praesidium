package io.github.wiselabv.praesidium.admin.asset.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 凭据实体：密码 / 私钥 / 令牌的加密保管箱。
 *
 * <p>secretEncrypted 为 AES-256-GCM 密文（见 shared/security/SecretCipher），绝不落明文；
 * secretMasked 为脱敏展示串（如 ssh-ed25519 AAAA…zQm9），列表接口只下发脱敏值。
 */
@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "secret_encrypted", nullable = false, columnDefinition = "TEXT")
    private String secretEncrypted;

    @Column(name = "secret_masked", length = 64)
    private String secretMasked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected Credential() {
    }

    private Credential(String name, String type, String secretEncrypted, String secretMasked) {
        Instant now = Instant.now();
        this.name = name;
        this.type = type;
        this.secretEncrypted = secretEncrypted;
        this.secretMasked = secretMasked;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Credential create(String name, String type, String secretEncrypted, String secretMasked) {
        return new Credential(name, type, secretEncrypted, secretMasked);
    }

    /** 修改基础信息（不换密） */
    public void update(String name, String type) {
        this.name = name;
        this.type = type;
        this.updatedAt = Instant.now();
    }

    /** 轮换敏感内容 */
    public void rotate(String secretEncrypted, String secretMasked) {
        this.secretEncrypted = secretEncrypted;
        this.secretMasked = secretMasked;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public String getSecretMasked() {
        return secretMasked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
