package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** MFA 验证请求 */
public record LoginMfaRequest(
        @NotBlank(message = "不能为空") String sessionToken,
        @NotBlank(message = "不能为空")
        @Pattern(regexp = "\\d{6}", message = "须为 6 位数字") String code) {
}
