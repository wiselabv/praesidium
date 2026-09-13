package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户聚合根：身份上下文的核心实体。
 *
 * <p>务实折中：实体直接携带 JPA 映射注解（jakarta.persistence 是规范而非框架实现），
 * 避免首版为「纯领域对象 + 基础设施映射类」付出双份成本；
 * 字段一律私有、无 setter，状态变更只能通过行为方法，保持聚合不变量。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 哈希，绝不下发到任何响应 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(length = 128)
    private String email;

    /** 是否启用两步验证（TOTP） */
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    /** TOTP 密钥（Base32），未启用 MFA 时为 null */
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected User() {
    }

    private User(String username, String passwordHash, String displayName, String email,
                 UserStatus status, Instant now) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 创建新用户（初始未启用 MFA） */
    public static User create(String username, String passwordHash, String displayName, String email) {
        return new User(username, passwordHash, displayName, email, UserStatus.ACTIVE, Instant.now());
    }

    /** 修改密码：同步刷新哈希与更新时间 */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    /** 更新基础资料（displayName/email 在 users 表，其余扩展字段在 user_profiles） */
    public void updateProfile(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
        this.updatedAt = Instant.now();
    }

    /** 启用两步验证并绑定 TOTP 密钥 */
    public void enableMfa(String secret) {
        this.mfaEnabled = true;
        this.totpSecret = secret;
        this.updatedAt = Instant.now();
    }

    /** 停用两步验证 */
    public void disableMfa() {
        this.mfaEnabled = false;
        this.totpSecret = null;
        this.updatedAt = Instant.now();
    }

    /** 禁用账号 */
    public void disable() {
        this.status = UserStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
