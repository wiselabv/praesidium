package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** 令牌对 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        /** 访问令牌有效期（秒），供前端计算续期时机 */
        long expiresIn,
        MeResponse user) {
}
