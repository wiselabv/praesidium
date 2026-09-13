package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 恢复码登录请求：用户名 + 一次性恢复码（跳过 MFA） */
public record RecoverLoginRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String recoveryCode) {
}
