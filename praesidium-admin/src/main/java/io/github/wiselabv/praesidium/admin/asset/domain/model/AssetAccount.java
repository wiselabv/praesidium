package io.github.wiselabv.praesidium.admin.asset.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 资产账号实体：资产上的登录账号（如 root / deploy）。
 *
 * <p>type：root（超级用户）、admin（管理员）、service（服务账号）、normal（普通账号）；
 * source：manual（手动录入）、discovery（自动收集）。
 */
@Entity
@Table(name = "asset_accounts")
public class AssetAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private boolean privileged;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "credential_id")
    private Long credentialId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected AssetAccount() {
    }

    private AssetAccount(Long assetId, String name, String type, boolean privileged,
                         String source, Long credentialId, boolean enabled) {
        Instant now = Instant.now();
        this.assetId = assetId;
        this.name = name;
        this.type = type;
        this.privileged = privileged;
        this.source = source == null || source.isBlank() ? "manual" : source;
        this.credentialId = credentialId;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AssetAccount create(Long assetId, String name, String type, boolean privileged,
                                      String source, Long credentialId, boolean enabled) {
        return new AssetAccount(assetId, name, type, privileged, source, credentialId, enabled);
    }

    /** 修改账号信息（资产归属不可变） */
    public void update(String name, String type, boolean privileged, String source,
                       Long credentialId, boolean enabled) {
        this.name = name;
        this.type = type;
        this.privileged = privileged;
        this.source = source == null || source.isBlank() ? "manual" : source;
        this.credentialId = credentialId;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public String getSource() {
        return source;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
