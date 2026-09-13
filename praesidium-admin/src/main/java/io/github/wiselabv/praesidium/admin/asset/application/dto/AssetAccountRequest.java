package io.github.wiselabv.praesidium.admin.asset.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建 / 修改资产账号请求 */
public record AssetAccountRequest(
        @NotBlank(message = "不能为空") String name,
        @NotBlank(message = "不能为空") String type,
        boolean privileged,
        String source,
        Long credentialId,
        boolean enabled) {
}
