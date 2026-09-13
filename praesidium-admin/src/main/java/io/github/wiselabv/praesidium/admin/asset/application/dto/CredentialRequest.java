package io.github.wiselabv.praesidium.admin.asset.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建 / 修改凭据请求（修改时 secret 可省略表示不轮换） */
public record CredentialRequest(
        @NotBlank(message = "不能为空") String name,
        @NotBlank(message = "不能为空") String type,
        String secret) {
}
