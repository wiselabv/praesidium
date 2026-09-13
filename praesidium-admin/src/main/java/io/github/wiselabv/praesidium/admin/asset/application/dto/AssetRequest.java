package io.github.wiselabv.praesidium.admin.asset.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建 / 修改资产请求 */
public record AssetRequest(
        @NotBlank(message = "不能为空") String name,
        @NotBlank(message = "不能为空") String type,
        @NotBlank(message = "不能为空") String address,
        @NotBlank(message = "不能为空") String protocol,
        Integer port,
        String groupPath,
        String description) {
}
