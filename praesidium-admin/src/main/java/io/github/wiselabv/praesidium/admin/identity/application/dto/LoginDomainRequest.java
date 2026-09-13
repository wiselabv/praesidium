package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 域账号登录请求：username 形如 PRAESIDIUM\admin 或 admin@domain（演示：凭据回落本地校验） */
public record LoginDomainRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String password) {
}
