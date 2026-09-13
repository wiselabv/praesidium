package io.github.wiselabv.praesidium.admin.identity.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.identity.domain.model.WebauthnCredential;

/** WebAuthn 设备条目 */
public record WebauthnItem(Long id, String name, String credentialId, long signCount, Instant addedAt) {

    public static WebauthnItem from(WebauthnCredential credential) {
        return new WebauthnItem(credential.getId(), credential.getName(),
                credential.getCredentialId(), credential.getSignCount(), credential.getAddedAt());
    }
}
