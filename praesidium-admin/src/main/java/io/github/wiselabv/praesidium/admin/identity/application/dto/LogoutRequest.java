package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 登出请求 */
public record LogoutRequest(
        @NotBlank(message = "不能为空") String refreshToken) {
}
