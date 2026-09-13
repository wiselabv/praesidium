package io.github.wiselabv.praesidium.admin.asset.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;

/** 资产列表项（含账号数，不含敏感信息） */
public record AssetItem(
        Long id,
        String name,
        String type,
        String address,
        String protocol,
        Integer port,
        String groupPath,
        String description,
        String status,
        long accountCount,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetItem from(Asset asset, long accountCount) {
        return new AssetItem(asset.getId(), asset.getName(), asset.getType(), asset.getAddress(),
                asset.getProtocol(), asset.getPort(), asset.getGroupPath(), asset.getDescription(),
                asset.getStatus(), accountCount, asset.getCreatedAt(), asset.getUpdatedAt());
    }
}
