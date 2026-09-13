package io.github.wiselabv.praesidium.admin.asset.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;

/** 资产账号列表项（冗余资产名与凭据名，便于列表直显） */
public record AssetAccountItem(
        Long id,
        Long assetId,
        String assetName,
        String name,
        String type,
        boolean privileged,
        String source,
        Long credentialId,
        String credentialName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetAccountItem from(AssetAccount account, String assetName, String credentialName) {
        return new AssetAccountItem(account.getId(), account.getAssetId(), assetName, account.getName(),
                account.getType(), account.isPrivileged(), account.getSource(), account.getCredentialId(),
                credentialName, account.isEnabled(), account.getCreatedAt(), account.getUpdatedAt());
    }
}
