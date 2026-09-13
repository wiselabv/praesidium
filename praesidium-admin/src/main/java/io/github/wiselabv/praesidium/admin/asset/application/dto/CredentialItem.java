package io.github.wiselabv.praesidium.admin.asset.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;

/** 凭据列表项（仅脱敏值，绝不含明文） */
public record CredentialItem(
        Long id,
        String name,
        String type,
        String secretMasked,
        long boundAccounts,
        Instant createdAt,
        Instant updatedAt) {

    public static CredentialItem from(Credential credential, long boundAccounts) {
        return new CredentialItem(credential.getId(), credential.getName(), credential.getType(),
                credential.getSecretMasked(), boundAccounts, credential.getCreatedAt(), credential.getUpdatedAt());
    }
}
