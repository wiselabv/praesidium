package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.UserSecuritySettingsRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserSecuritySettings;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户 MFA 补充设置仓储 JPA 实现（基础设施层）。
 */
@Repository
public class UserSecuritySettingsRepositoryJpa implements UserSecuritySettingsRepository {

    private final EntityManager entityManager;

    public UserSecuritySettingsRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSecuritySettings> findByUserId(Long userId) {
        return Optional.ofNullable(entityManager.find(UserSecuritySettings.class, userId));
    }

    @Override
    @Transactional
    public UserSecuritySettings save(UserSecuritySettings settings) {
        return entityManager.merge(settings);
    }
}
