package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.UserThirdPartyRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserThirdParty;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第三方登录绑定仓储 JPA 实现（基础设施层）。
 */
@Repository
public class UserThirdPartyRepositoryJpa implements UserThirdPartyRepository {

    private final EntityManager entityManager;

    public UserThirdPartyRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserThirdParty> findByUserId(Long userId) {
        return entityManager.createQuery(
                        "select t from UserThirdParty t where t.userId = :userId order by t.boundAt desc",
                        UserThirdParty.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserThirdParty> findByUserIdAndProvider(Long userId, String provider) {
        return entityManager.createQuery(
                        "select t from UserThirdParty t where t.userId = :userId and t.provider = :provider",
                        UserThirdParty.class)
                .setParameter("userId", userId)
                .setParameter("provider", provider)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserThirdParty> findFirstByProvider(String provider) {
        return entityManager.createQuery(
                        "select t from UserThirdParty t where t.provider = :provider order by t.id",
                        UserThirdParty.class)
                .setParameter("provider", provider)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public UserThirdParty save(UserThirdParty binding) {
        if (binding.getId() == null) {
            entityManager.persist(binding);
            return binding;
        }
        return entityManager.merge(binding);
    }

    @Override
    @Transactional
    public void delete(UserThirdParty binding) {
        entityManager.remove(entityManager.contains(binding)
                ? binding : entityManager.merge(binding));
    }
}
