package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 保存 SSO 提供商配置请求（clientSecret 为空表示保留原值） */
public record SsoProviderSaveRequest(
        @NotBlank(message = "不能为空") @Size(max = 16) String provider,
        @NotBlank(message = "不能为空") @Size(max = 64) String displayName,
        boolean enabled,
        @Size(max = 128) String clientId,
        String clientSecret,
        @Size(max = 255) String authorizeUrl,
        @Size(max = 255) String tokenUrl,
        @Size(max = 255) String userinfoUrl,
        @Size(max = 255) String scopes,
        String extraConfig) {
}
