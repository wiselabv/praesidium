package io.github.wiselabv.praesidium.admin.asset.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.asset.domain.CredentialRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 凭据仓储 JPA 实现（基础设施层）。
 */
@Repository
public class CredentialRepositoryJpa implements CredentialRepository {

    private final EntityManager entityManager;

    public CredentialRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Credential> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Credential.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Credential> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("select c from Credential c where c.id in :ids", Credential.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    @Transactional
    public Credential save(Credential credential) {
        if (credential.getId() == null) {
            entityManager.persist(credential);
            return credential;
        }
        return entityManager.merge(credential);
    }

    @Override
    @Transactional
    public void delete(Credential credential) {
        entityManager.remove(entityManager.contains(credential) ? credential : entityManager.merge(credential));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Credential> findPage(String keyword, int offset, int limit) {
        TypedQuery<Credential> query = entityManager.createQuery(
                "select c from Credential c where (:kw is null or lower(c.name) like :kw) order by c.id desc",
                Credential.class);
        bindKeyword(query, keyword);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(c) from Credential c where (:kw is null or lower(c.name) like :kw)", Long.class);
        bindKeyword(query, keyword);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return entityManager.createQuery("select count(c) from Credential c", Long.class).getSingleResult();
    }

    private void bindKeyword(TypedQuery<?> query, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            query.setParameter("kw", null);
        } else {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }
    }
}
