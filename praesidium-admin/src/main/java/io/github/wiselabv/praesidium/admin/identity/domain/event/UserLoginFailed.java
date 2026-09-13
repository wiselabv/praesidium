package io.github.wiselabv.praesidium.admin.identity.domain.event;

import io.github.wiselabv.praesidium.admin.shared.domain.DomainEvent;

/**
 * 用户登录失败事件（密码错 / MFA 码错 / 账号禁用）。
 *
 * <p>audit 上下文订阅本事件落失败登录日志（防暴力破解审计的基础数据）。
 */
public class UserLoginFailed extends DomainEvent {

    private final Long userId;
    private final String username;
    private final String sourceIp;
    private final String method;
    private final String reason;
    private final String client;

    public UserLoginFailed(Long userId, String username, String sourceIp,
                           String method, String reason, String client) {
        this.userId = userId;
        this.username = username;
        this.sourceIp = sourceIp;
        this.method = method;
        this.reason = reason;
        this.client = client;
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

    public String getMethod() {
        return method;
    }

    public String getReason() {
        return reason;
    }

    public String getClient() {
        return client;
    }
}
