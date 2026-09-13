package io.github.wiselabv.praesidium.admin.audit.infrastructure;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.domain.AuditLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作审计日志仓储 JPA 实现（基础设施层）。
 */
@Repository
public class AuditLogRepositoryJpa implements AuditLogRepository {

    private final EntityManager entityManager;

    public AuditLogRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog log) {
        if (log.getId() == null) {
            entityManager.persist(log);
            return log;
        }
        return entityManager.merge(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findPage(String keyword, String risk, String result, int offset, int limit) {
        TypedQuery<AuditLog> query = entityManager.createQuery(
                "select l from AuditLog l where (:kw is null or lower(l.action) like :kw or lower(l.commandDetail) like :kw) "
                        + "and (:risk is null or l.risk = :risk) and (:result is null or l.result = :result) "
                        + "order by l.createdAt desc, l.id desc",
                AuditLog.class);
        bind(query, keyword, risk, result);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword, String risk, String result) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(l) from AuditLog l where (:kw is null or lower(l.action) like :kw or lower(l.commandDetail) like :kw) "
                        + "and (:risk is null or l.risk = :risk) and (:result is null or l.result = :result)",
                Long.class);
        bind(query, keyword, risk, result);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findRecent(int limit) {
        return entityManager.createQuery(
                        "select l from AuditLog l order by l.createdAt desc, l.id desc", AuditLog.class)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return entityManager.createQuery(
                        "select count(l) from AuditLog l where l.createdAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBlockedSince(Instant since) {
        return entityManager.createQuery(
                        "select count(l) from AuditLog l where l.result = 'blocked' and l.createdAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countGroupByRiskSince(Instant since) {
        return entityManager.createQuery(
                        "select l.risk, count(l) from AuditLog l where l.createdAt >= :since group by l.risk",
                        Object[].class)
                .setParameter("since", since)
                .getResultList();
    }

    private void bind(TypedQuery<?> query, String keyword, String risk, String result) {
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        query.setParameter("risk", risk == null || risk.isBlank() ? null : risk);
        query.setParameter("result", result == null || result.isBlank() ? null : result);
    }
}
