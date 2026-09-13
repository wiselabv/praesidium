package io.github.wiselabv.praesidium.admin.access.application.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 新建 / 修改授权策略请求 */
public record AccessPolicyRequest(
        @NotBlank(message = "不能为空") String name,
        @NotNull(message = "不能为空") Long userId,
        @NotNull(message = "不能为空") List<Long> assetIds,
        List<Long> accountIds,
        String protocol,
        Instant validFrom,
        Instant validTo,
        String description) {
}
