package io.github.wiselabv.praesidium.admin.audit.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 登录日志实体：成功与失败的登录尝试留痕（认证埋点写入）。
 *
 * <p>method：password（密码/MFA）、code（验证码）、sso（第三方）、domain（域账号）；
 * result：success / failed。
 */
@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 失败登录可能无 userId（账号不存在） */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(length = 64)
    private String location;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false, length = 16)
    private String result;

    @Column(length = 128)
    private String reason;

    @Column(length = 64)
    private String client;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA 要求 */
    protected LoginLog() {
    }

    private LoginLog(Long userId, String username, String sourceIp, String location,
                     String method, String result, String reason, String client) {
        this.userId = userId;
        this.username = username;
        this.sourceIp = sourceIp;
        this.location = location;
        this.method = method;
        this.result = result;
        this.reason = reason;
        this.client = client;
        this.createdAt = Instant.now();
    }

    /** 记录一次登录尝试（成功 / 失败） */
    public static LoginLog record(Long userId, String username, String sourceIp, String location,
                                  String method, String result, String reason, String client) {
        return new LoginLog(userId, username, sourceIp, location, method, result, reason, client);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getLocation() {
        return location;
    }

    public String getMethod() {
        return method;
    }

    public String getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public String getClient() {
        return client;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
