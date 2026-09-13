package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** 邮件验证码开关请求 */
public record EmailMfaToggleRequest(boolean enabled) {
}
