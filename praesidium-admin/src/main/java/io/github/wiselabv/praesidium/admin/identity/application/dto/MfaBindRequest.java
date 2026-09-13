package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** TOTP 绑定阶段二请求：setup 返回的密钥 + 当前动态码 */
public record MfaBindRequest(
        @NotBlank(message = "不能为空") String secret,
        @NotBlank(message = "不能为空") String code) {
}
