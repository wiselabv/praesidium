package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** 安全设置摘要：MFA 各通道状态（个人安全页展示） */
public record SecuritySummaryResponse(
        boolean mfaEnabled,
        String smsPhone,
        boolean smsEnabled,
        boolean emailMfaEnabled,
        long webauthnCount,
        long recoveryCodesLeft) {
}
