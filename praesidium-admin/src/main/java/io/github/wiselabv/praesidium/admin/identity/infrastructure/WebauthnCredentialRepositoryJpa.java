package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.WebauthnCredentialRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.WebauthnCredential;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebAuthn 凭据仓储 JPA 实现（基础设施层）。
 */
@Repository
public class WebauthnCredentialRepositoryJpa implements WebauthnCredentialRepository {

    private final EntityManager entityManager;

    public WebauthnCredentialRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebauthnCredential> findByUserId(Long userId) {
        return entityManager.createQuery(
                        "select w from WebauthnCredential w where w.userId = :userId order by w.addedAt desc",
                        WebauthnCredential.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebauthnCredential> findById(Long id) {
        return Optional.ofNullable(entityManager.find(WebauthnCredential.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        return entityManager.createQuery(
                        "select count(w) from WebauthnCredential w where w.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    @Transactional
    public WebauthnCredential save(WebauthnCredential credential) {
        if (credential.getId() == null) {
            entityManager.persist(credential);
            return credential;
        }
        return entityManager.merge(credential);
    }

    @Override
    @Transactional
    public void delete(WebauthnCredential credential) {
        entityManager.remove(entityManager.contains(credential)
                ? credential : entityManager.merge(credential));
    }
}
