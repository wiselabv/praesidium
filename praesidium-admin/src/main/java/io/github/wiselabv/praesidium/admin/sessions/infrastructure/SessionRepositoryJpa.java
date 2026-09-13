package io.github.wiselabv.praesidium.admin.sessions.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.sessions.domain.SessionRepository;
import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话仓储 JPA 实现（基础设施层）。
 */
@Repository
public class SessionRepositoryJpa implements SessionRepository {

    private final EntityManager entityManager;

    public SessionRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Session.class, id));
    }

    @Override
    @Transactional
    public Session save(Session session) {
        if (session.getId() == null) {
            entityManager.persist(session);
            return session;
        }
        return entityManager.merge(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findPage(String keyword, String status, int offset, int limit) {
        TypedQuery<Session> query = entityManager.createQuery(
                "select s from Session s where (:kw is null or lower(s.protocol) like :kw or lower(s.sourceIp) like :kw) "
                        + "and (:status is null or s.status = :status) order by s.startedAt desc",
                Session.class);
        bind(query, keyword, status);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword, String status) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(s) from Session s where (:kw is null or lower(s.protocol) like :kw or lower(s.sourceIp) like :kw) "
                        + "and (:status is null or s.status = :status)",
                Long.class);
        bind(query, keyword, status);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return entityManager.createQuery(
                        "select count(s) from Session s where s.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countStartedSince(Instant since) {
        return entityManager.createQuery(
                        "select count(s) from Session s where s.startedAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countDailySince(Instant since) {
        return entityManager.createQuery(
                        "select function('date', s.startedAt), count(s) from Session s "
                                + "where s.startedAt >= :since group by function('date', s.startedAt) "
                                + "order by function('date', s.startedAt)",
                        Object[].class)
                .setParameter("since", since)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findRecordings(String keyword, int offset, int limit) {
        TypedQuery<Session> query = entityManager.createQuery(
                "select s from Session s where s.status = 'ended' and s.recordingPath is not null "
                        + "and (:kw is null or lower(s.protocol) like :kw or lower(s.sourceIp) like :kw) "
                        + "order by s.endedAt desc, s.id desc",
                Session.class);
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecordings(String keyword) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(s) from Session s where s.status = 'ended' and s.recordingPath is not null "
                        + "and (:kw is null or lower(s.protocol) like :kw or lower(s.sourceIp) like :kw)",
                Long.class);
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findRecentByStatus(String status, int limit) {
        return entityManager.createQuery(
                        "select s from Session s where s.status = :status order by s.startedAt desc", Session.class)
                .setParameter("status", status)
                .setMaxResults(limit)
                .getResultList();
    }

    private void bind(TypedQuery<?> query, String keyword, String status) {
        query.setParameter("kw", keyword == null || keyword.isBlank()
                ? null : "%" + keyword.trim().toLowerCase() + "%");
        query.setParameter("status", status == null || status.isBlank() ? null : status);
    }
}
