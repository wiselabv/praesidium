package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户 MFA 补充设置实体：短信 / 邮件验证码的绑定状态（共享主键 user_id）。
 */
@Entity
@Table(name = "user_security_settings")
public class UserSecuritySettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sms_phone", length = 20)
    private String smsPhone;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @Column(name = "email_mfa_enabled", nullable = false)
    private boolean emailMfaEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected UserSecuritySettings() {
    }

    private UserSecuritySettings(Long userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    /** 为指定用户创建默认设置（全部未绑定） */
    public static UserSecuritySettings create(Long userId) {
        return new UserSecuritySettings(userId);
    }

    /** 绑定短信验证手机号并启用 */
    public void bindSms(String phone) {
        this.smsPhone = phone;
        this.smsEnabled = true;
        this.updatedAt = Instant.now();
    }

    /** 解绑短信验证 */
    public void unbindSms() {
        this.smsPhone = null;
        this.smsEnabled = false;
        this.updatedAt = Instant.now();
    }

    /** 启用 / 停用邮件验证码 */
    public void toggleEmailMfa(boolean enabled) {
        this.emailMfaEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getSmsPhone() {
        return smsPhone;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isEmailMfaEnabled() {
        return emailMfaEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
