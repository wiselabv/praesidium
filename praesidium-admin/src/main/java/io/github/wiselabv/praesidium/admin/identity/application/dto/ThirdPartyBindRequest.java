package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 绑定第三方账号请求（演示：直接完成绑定，真实场景先跳转提供商授权） */
public record ThirdPartyBindRequest(
        @NotBlank(message = "不能为空") String provider) {
}
