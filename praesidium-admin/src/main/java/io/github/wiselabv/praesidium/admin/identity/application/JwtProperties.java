package io.github.wiselabv.praesidium.admin.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 与刷新令牌配置（绑定 {@code praesidium.security.jwt.*}）。
 *
 * <p>secret 缺省为仅限本地开发的演示密钥，生产环境必须通过
 * {@code PRAESIDIUM_JWT_SECRET} 环境变量覆盖（HS256 要求 ≥ 32 字节）。
 */
@ConfigurationProperties(prefix = "praesidium.security.jwt")
public class JwtProperties {

    /** 签名密钥（HS256 共享密钥，Rust 网关持同一密钥验签） */
    private String secret = "praesidium-dev-only-secret-change-me-in-production-0123456789";

    /** 访问令牌有效期（分钟） */
    private long accessTtlMinutes = 15;

    /** 登录会话令牌（密码通过后待 MFA 阶段）有效期（分钟） */
    private long sessionTtlMinutes = 5;

    /** 网关令牌有效期（分钟）：浏览器持令牌连 Rust 网关建 SSH 会话 */
    private long gatewayTtlMinutes = 480;

    /** 刷新令牌有效期（天） */
    private long refreshTtlDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(long accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(long sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public long getGatewayTtlMinutes() {
        return gatewayTtlMinutes;
    }

    public void setGatewayTtlMinutes(long gatewayTtlMinutes) {
        this.gatewayTtlMinutes = gatewayTtlMinutes;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }
}
