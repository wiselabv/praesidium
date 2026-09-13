package io.github.wiselabv.praesidium.admin.audit.infrastructure;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.domain.LoginLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.LoginLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录日志仓储 JPA 实现（基础设施层）。
 */
@Repository
public class LoginLogRepositoryJpa implements LoginLogRepository {

    private final EntityManager entityManager;

    public LoginLogRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public LoginLog save(LoginLog log) {
        if (log.getId() == null) {
            entityManager.persist(log);
            return log;
        }
        return entityManager.merge(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginLog> findPage(String keyword, String result, int offset, int limit) {
        TypedQuery<LoginLog> query = entityManager.createQuery(
                "select l from LoginLog l where (:kw is null or lower(l.username) like :kw or lower(l.sourceIp) like :kw) "
                        + "and (:result is null or l.result = :result) order by l.createdAt desc, l.id desc",
                LoginLog.class);
        bind(query, keyword, result);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword, String result) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(l) from LoginLog l where (:kw is null or lower(l.username) like :kw or lower(l.sourceIp) like :kw) "
                        + "and (:result is null or l.result = :result)",
                Long.class);
        bind(query, keyword, result);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return entityManager.createQuery(
                        "select count(l) from LoginLog l where l.createdAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedSince(Instant since) {
        return entityManager.createQuery(
                        "select count(l) from LoginLog l where l.createdAt >= :since and l.result = 'failed'",
                        Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countDailySince(Instant since) {
        return entityManager.createQuery(
                        "select function('date', l.createdAt), l.result, count(l) from LoginLog l "
                                + "where l.createdAt >= :since "
                                + "group by function('date', l.createdAt), l.result",
                        Object[].class)
                .setParameter("since", since)
                .getResultList();
    }

    private void bind(TypedQuery<?> query, String keyword, String result) {
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        query.setParameter("result", result == null || result.isBlank() ? null : result);
    }
}
