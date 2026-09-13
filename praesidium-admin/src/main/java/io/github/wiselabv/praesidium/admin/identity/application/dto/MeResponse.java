package io.github.wiselabv.praesidium.admin.identity.application.dto;

import io.github.wiselabv.praesidium.admin.identity.domain.model.User;

/** 当前用户信息 */
public record MeResponse(
        Long id,
        String username,
        String displayName,
        String email,
        boolean mfaEnabled) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), user.isMfaEnabled());
    }
}
