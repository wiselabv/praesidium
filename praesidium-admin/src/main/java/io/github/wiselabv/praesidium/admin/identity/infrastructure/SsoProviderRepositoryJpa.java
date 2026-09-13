package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.SsoProviderRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.SsoProvider;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * SSO 提供商配置仓储 JPA 实现（基础设施层）。
 */
@Repository
public class SsoProviderRepositoryJpa implements SsoProviderRepository {

    private final EntityManager entityManager;

    public SsoProviderRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SsoProvider> findAll() {
        return entityManager.createQuery(
                        "select p from SsoProvider p order by p.provider", SsoProvider.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SsoProvider> findByProvider(String provider) {
        return entityManager.createQuery(
                        "select p from SsoProvider p where p.provider = :provider", SsoProvider.class)
                .setParameter("provider", provider)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SsoProvider> findById(Long id) {
        return Optional.ofNullable(entityManager.find(SsoProvider.class, id));
    }

    @Override
    @Transactional
    public SsoProvider save(SsoProvider provider) {
        if (provider.getId() == null) {
            entityManager.persist(provider);
            return provider;
        }
        return entityManager.merge(provider);
    }

    @Override
    @Transactional
    public void delete(SsoProvider provider) {
        entityManager.remove(entityManager.contains(provider)
                ? provider : entityManager.merge(provider));
    }
}
