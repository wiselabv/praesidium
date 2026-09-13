package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.RecoveryCodeRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.RecoveryCode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA 恢复码仓储 JPA 实现（基础设施层）。
 */
@Repository
public class RecoveryCodeRepositoryJpa implements RecoveryCodeRepository {

    private final EntityManager entityManager;

    public RecoveryCodeRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoveryCode> findByUserId(Long userId) {
        return entityManager.createQuery(
                        "select r from RecoveryCode r where r.userId = :userId order by r.createdAt desc",
                        RecoveryCode.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoveryCode> findUnusedByUserId(Long userId) {
        return entityManager.createQuery(
                        "select r from RecoveryCode r where r.userId = :userId and r.used = false "
                                + "order by r.createdAt desc",
                        RecoveryCode.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecoveryCode> findUnusedByUserIdAndHash(Long userId, String codeHash) {
        return entityManager.createQuery(
                        "select r from RecoveryCode r where r.userId = :userId and r.codeHash = :hash and r.used = false",
                        RecoveryCode.class)
                .setParameter("userId", userId)
                .setParameter("hash", codeHash)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnusedByUserId(Long userId) {
        return entityManager.createQuery(
                        "select count(r) from RecoveryCode r where r.userId = :userId and r.used = false",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    @Transactional
    public RecoveryCode save(RecoveryCode code) {
        if (code.getId() == null) {
            entityManager.persist(code);
            return code;
        }
        return entityManager.merge(code);
    }

    @Override
    @Transactional
    public void deleteAll(List<RecoveryCode> codes) {
        for (RecoveryCode code : codes) {
            entityManager.remove(entityManager.contains(code)
                    ? code : entityManager.merge(code));
        }
    }
}
