package io.github.wiselabv.praesidium.admin.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新个人信息请求（displayName/email 在 users 表，其余在 user_profiles） */
public record UpdateProfileRequest(
        @NotBlank(message = "显示名称不能为空") @Size(max = 64) String displayName,
        @Email(message = "邮箱格式不正确") String email,
        @Size(max = 255) String avatar,
        @Size(max = 20) String phone,
        @Size(max = 64) String department,
        @Size(max = 64) String title,
        @Size(max = 512) String bio) {
}
