package io.github.wiselabv.praesidium.admin.identity.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserProfile;

/** 个人信息响应：users 基础字段 + user_profiles 扩展字段 */
public record ProfileResponse(
        Long id,
        String username,
        String displayName,
        String email,
        boolean mfaEnabled,
        String status,
        String avatar,
        String phone,
        String department,
        String title,
        String bio,
        Instant lastLoginAt,
        String lastLoginIp,
        Instant createdAt) {

    public static ProfileResponse from(User user, UserProfile profile) {
        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.isMfaEnabled(),
                user.getStatus().name(),
                profile != null ? profile.getAvatar() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getDepartment() : null,
                profile != null ? profile.getTitle() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getLastLoginAt() : null,
                profile != null ? profile.getLastLoginIp() : null,
                user.getCreatedAt());
    }
}
