package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.SsoProvider;

/**
 * SSO 提供商配置仓储（领域层接口）。
 */
public interface SsoProviderRepository {

    List<SsoProvider> findAll();

    Optional<SsoProvider> findByProvider(String provider);

    Optional<SsoProvider> findById(Long id);

    SsoProvider save(SsoProvider provider);

    void delete(SsoProvider provider);
}
