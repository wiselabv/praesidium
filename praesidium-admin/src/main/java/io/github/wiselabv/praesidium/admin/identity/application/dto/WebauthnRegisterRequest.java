package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 注册 WebAuthn 设备请求（演示：后端生成凭据元数据，真实场景由前端 attestation 提供） */
public record WebauthnRegisterRequest(
        @NotBlank(message = "不能为空") @Size(max = 64) String name) {
}
