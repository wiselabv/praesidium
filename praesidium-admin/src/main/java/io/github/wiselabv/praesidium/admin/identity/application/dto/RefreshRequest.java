package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 刷新令牌请求 */
public record RefreshRequest(
        @NotBlank(message = "不能为空") String refreshToken) {
}
