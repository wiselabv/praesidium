package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 发送验证码请求：channel=sms（手机号）/ email（邮箱地址）；演示模式验证码固定 123456 */
public record SendCodeRequest(
        @NotBlank(message = "不能为空") String channel,
        @NotBlank(message = "不能为空") String target) {
}
