package io.github.wiselabv.praesidium.admin.access.application.dto;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;

/** 授权策略列表项（id 数组 + 展示名冗余） */
public record PolicyItem(
        Long id,
        String name,
        Long userId,
        String userName,
        List<Long> assetIds,
        String assetNames,
        List<Long> accountIds,
        String accountNames,
        String protocol,
        Instant validFrom,
        Instant validTo,
        String status,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyItem of(AccessPolicy policy, String userName,
                                List<Long> assetIds, String assetNames,
                                List<Long> accountIds, String accountNames) {
        return new PolicyItem(policy.getId(), policy.getName(), policy.getUserId(), userName,
                assetIds, assetNames, accountIds, accountNames, policy.getProtocol(),
                policy.getValidFrom(), policy.getValidTo(), policy.getStatus(),
                policy.getDescription(), policy.getCreatedAt(), policy.getUpdatedAt());
    }
}
