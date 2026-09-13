package io.github.wiselabv.praesidium.admin.identity.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 个人信息实体：users 只承载认证凭据，个人资料独立 1:1 扩展。
 *
 * <p>主键即 user_id（共享主键，无自增），资料缺失时按空资料展示。
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 255)
    private String avatar;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String department;

    @Column(length = 64)
    private String title;

    @Column(length = 512)
    private String bio;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected UserProfile() {
    }

    private UserProfile(Long userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    /** 为指定用户创建空白资料 */
    public static UserProfile create(Long userId) {
        return new UserProfile(userId);
    }

    /** 更新个人资料字段 */
    public void update(String avatar, String phone, String department, String title, String bio) {
        this.avatar = avatar;
        this.phone = phone;
        this.department = department;
        this.title = title;
        this.bio = bio;
        this.updatedAt = Instant.now();
    }

    /** 登录成功后刷新最近登录信息（登录埋点补充） */
    public void touchLogin(String ip) {
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ip;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getPhone() {
        return phone;
    }

    public String getDepartment() {
        return department;
    }

    public String getTitle() {
        return title;
    }

    public String getBio() {
        return bio;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
