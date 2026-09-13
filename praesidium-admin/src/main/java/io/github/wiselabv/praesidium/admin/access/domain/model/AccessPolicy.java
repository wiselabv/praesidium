package io.github.wiselabv.praesidium.admin.access.domain.model;

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
 * 授权策略实体：把「用户 → 资产/账号 → 协议」的访问许可写成可审批策略。
 *
 * <p>assetIds / accountIds 为 JSONB 数组（JSON 字符串形式存取，由应用层序列化）；
 * status 生命周期：pending（待审批）→ active（生效）/ revoked（已撤销）。
 */
@Entity
@Table(name = "access_policies")
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** JSON 数组字符串，如 "[1,2]" */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "asset_ids", nullable = false)
    private String assetIds;

    /** JSON 数组字符串，如 "[5,6]" */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "account_ids", nullable = false)
    private String accountIds;

    @Column(length = 16)
    private String protocol;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected AccessPolicy() {
    }

    private AccessPolicy(String name, Long userId, String assetIds, String accountIds,
                         String protocol, Instant validFrom, Instant validTo, String description) {
        Instant now = Instant.now();
        this.name = name;
        this.userId = userId;
        this.assetIds = assetIds;
        this.accountIds = accountIds;
        this.protocol = protocol;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.description = description;
        this.status = "pending";
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 新建策略（状态 pending，待审批） */
    public static AccessPolicy create(String name, Long userId, String assetIds, String accountIds,
                                      String protocol, Instant validFrom, Instant validTo, String description) {
        return new AccessPolicy(name, userId, assetIds, accountIds, protocol, validFrom, validTo, description);
    }

    /** 修改策略内容（保持审批状态不变） */
    public void update(String name, Long userId, String assetIds, String accountIds,
                       String protocol, Instant validFrom, Instant validTo, String description) {
        this.name = name;
        this.userId = userId;
        this.assetIds = assetIds;
        this.accountIds = accountIds;
        this.protocol = protocol;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    /** 审批通过 */
    public void approve() {
        this.status = "active";
        this.updatedAt = Instant.now();
    }

    /** 撤销授权 */
    public void revoke() {
        this.status = "revoked";
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAssetIds() {
        return assetIds;
    }

    public String getAccountIds() {
        return accountIds;
    }

    public String getProtocol() {
        return protocol;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
