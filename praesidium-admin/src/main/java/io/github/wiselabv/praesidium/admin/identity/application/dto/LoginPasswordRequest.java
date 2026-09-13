package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 密码登录请求 */
public record LoginPasswordRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String password) {
}
