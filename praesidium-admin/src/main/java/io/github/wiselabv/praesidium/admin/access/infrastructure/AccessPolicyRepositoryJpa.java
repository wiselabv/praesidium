package io.github.wiselabv.praesidium.admin.access.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.access.domain.AccessPolicyRepository;
import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 授权策略仓储 JPA 实现（基础设施层）。
 */
@Repository
public class AccessPolicyRepositoryJpa implements AccessPolicyRepository {

    private final EntityManager entityManager;

    public AccessPolicyRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessPolicy> findById(Long id) {
        return Optional.ofNullable(entityManager.find(AccessPolicy.class, id));
    }

    @Override
    @Transactional
    public AccessPolicy save(AccessPolicy policy) {
        if (policy.getId() == null) {
            entityManager.persist(policy);
            return policy;
        }
        return entityManager.merge(policy);
    }

    @Override
    @Transactional
    public void delete(AccessPolicy policy) {
        entityManager.remove(entityManager.contains(policy) ? policy : entityManager.merge(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessPolicy> findPage(String keyword, String status, int offset, int limit) {
        TypedQuery<AccessPolicy> query = entityManager.createQuery(
                "select p from AccessPolicy p where (:kw is null or lower(p.name) like :kw or lower(p.description) like :kw) "
                        + "and (:status is null or p.status = :status) order by p.id desc",
                AccessPolicy.class);
        bind(query, keyword, status);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword, String status) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(p) from AccessPolicy p where (:kw is null or lower(p.name) like :kw or lower(p.description) like :kw) "
                        + "and (:status is null or p.status = :status)",
                Long.class);
        bind(query, keyword, status);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessPolicy> findByStatus(String status) {
        return entityManager.createQuery(
                        "select p from AccessPolicy p where p.status = :status order by p.id", AccessPolicy.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return entityManager.createQuery("select count(p) from AccessPolicy p", Long.class).getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return entityManager.createQuery(
                        "select count(p) from AccessPolicy p where p.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    private void bind(TypedQuery<?> query, String keyword, String status) {
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        query.setParameter("status", status == null || status.isBlank() ? null : status);
    }
}
