package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** TOTP 绑定阶段一响应：新生成的密钥与 otpauth URI（供渲染二维码） */
public record MfaSetupResponse(String secret, String otpAuthUri) {
}
