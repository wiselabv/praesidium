package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 验证码登录请求：用户名 + 验证码（演示模式验证码固定 123456） */
public record LoginCodeRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String code) {
}
