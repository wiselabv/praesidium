package io.github.wiselabv.praesidium.admin.audit.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 操作审计日志实体：堡垒机内高危/普通操作的留痕。
 *
 * <p>result：success（成功）、blocked（被拦截）、failed（失败）；
 * risk：low / medium / high。数据来源为会话网关上报，本服务只做落库与查询。
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "command_detail", columnDefinition = "TEXT")
    private String commandDetail;

    @Column(nullable = false, length = 16)
    private String result;

    @Column(nullable = false, length = 16)
    private String risk;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA 要求 */
    protected AuditLog() {
    }

    private AuditLog(Long userId, Long assetId, Long accountId, String action,
                     String commandDetail, String result, String risk, String sourceIp) {
        this.userId = userId;
        this.assetId = assetId;
        this.accountId = accountId;
        this.action = action;
        this.commandDetail = commandDetail;
        this.result = result;
        this.risk = risk;
        this.sourceIp = sourceIp;
        this.createdAt = Instant.now();
    }

    /** 记录一条操作审计（网关上报 / 系统埋点共用） */
    public static AuditLog record(Long userId, Long assetId, Long accountId, String action,
                                  String commandDetail, String result, String risk, String sourceIp) {
        return new AuditLog(userId, assetId, accountId, action, commandDetail, result, risk, sourceIp);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAction() {
        return action;
    }

    public String getCommandDetail() {
        return commandDetail;
    }

    public String getResult() {
        return result;
    }

    public String getRisk() {
        return risk;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
