package io.github.wiselabv.praesidium.admin.identity.domain.event;

import io.github.wiselabv.praesidium.admin.shared.domain.DomainEvent;

/**
 * 用户已登录（含 MFA 通过）事件。
 *
 * <p>由 application 层在登录成功后发布；audit 上下文订阅本事件落登录日志，
 * 身份上下文订阅本事件刷新最近登录信息，两者互不感知。
 */
public class UserLoggedIn extends DomainEvent {

    private final Long userId;
    private final String username;
    private final String sourceIp;
    private final String method;
    private final String client;

    public UserLoggedIn(Long userId, String username, String sourceIp, String method, String client) {
        this.userId = userId;
        this.username = username;
        this.sourceIp = sourceIp;
        this.method = method;
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

    public String getClient() {
        return client;
    }
}
