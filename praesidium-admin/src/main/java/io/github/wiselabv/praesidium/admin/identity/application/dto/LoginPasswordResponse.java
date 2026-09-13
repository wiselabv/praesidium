package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** 密码登录响应：next=mfa 表示需进入 MFA 阶段；next=done 表示直接登录完成 */
public record LoginPasswordResponse(
        String next,
        /* 仅 next=mfa 时下发 */
        String sessionToken,
        /* 仅 next=done（未启用 MFA）时下发 */
        TokenResponse tokens) {
}
