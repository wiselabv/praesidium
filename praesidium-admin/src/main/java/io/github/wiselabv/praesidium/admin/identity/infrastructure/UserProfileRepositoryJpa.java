package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.UserProfileRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserProfile;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人信息仓储 JPA 实现（基础设施层）。
 */
@Repository
public class UserProfileRepositoryJpa implements UserProfileRepository {

    private final EntityManager entityManager;

    public UserProfileRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(Long userId) {
        return Optional.ofNullable(entityManager.find(UserProfile.class, userId));
    }

    @Override
    @Transactional
    public UserProfile save(UserProfile profile) {
        return entityManager.merge(profile);
    }
}
