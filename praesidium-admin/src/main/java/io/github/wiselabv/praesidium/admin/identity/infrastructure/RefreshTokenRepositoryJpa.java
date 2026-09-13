package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.RefreshTokenRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.RefreshToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 刷新令牌仓储 JPA 实现（基础设施层，实现领域层接口——依赖倒置）。
 *
 * <p>基于 EntityManager 手写持久化逻辑，查询语义显式可见。
 */
@Repository
public class RefreshTokenRepositoryJpa implements RefreshTokenRepository {

    private final EntityManager entityManager;

    public RefreshTokenRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        TypedQuery<RefreshToken> query = entityManager.createQuery(
                "select t from RefreshToken t where t.tokenHash = :tokenHash", RefreshToken.class);
        query.setParameter("tokenHash", tokenHash);
        return query.getResultStream().findFirst();
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        if (token.getId() == null) {
            entityManager.persist(token);
            return token;
        }
        return entityManager.merge(token);
    }
}
