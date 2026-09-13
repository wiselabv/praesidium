package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 绑定短信验证请求：手机号 + 演示验证码 */
public record SmsBindRequest(
        @NotBlank(message = "不能为空") @Pattern(regexp = "\\+?[0-9]{6,15}", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "不能为空") String code) {
}
