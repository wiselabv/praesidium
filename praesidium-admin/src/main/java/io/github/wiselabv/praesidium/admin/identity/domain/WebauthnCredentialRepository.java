package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.WebauthnCredential;

/**
 * WebAuthn 凭据仓储（领域层接口）。
 */
public interface WebauthnCredentialRepository {

    List<WebauthnCredential> findByUserId(Long userId);

    Optional<WebauthnCredential> findById(Long id);

    long countByUserId(Long userId);

    WebauthnCredential save(WebauthnCredential credential);

    void delete(WebauthnCredential credential);
}
