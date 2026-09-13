package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户仓储 JPA 实现（基础设施层，实现领域层接口——依赖倒置）。
 *
 * <p>基于 EntityManager 手写持久化逻辑，不引入 Spring Data 派生查询，
 * 查询语义显式可见。
 */
@Repository
public class UserRepositoryJpa implements UserRepository {

    private final EntityManager entityManager;

    public UserRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("select u from User u where u.id in :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        TypedQuery<User> query = entityManager.createQuery(
                "select u from User u where u.username = :username", User.class);
        query.setParameter("username", username);
        return query.getResultStream().findFirst();
    }

    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(u) from User u where u.username = :username", Long.class);
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return entityManager.createQuery("select count(u) from User u", Long.class)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllSimple() {
        return entityManager.createQuery("select u from User u order by u.id", User.class)
                .getResultList();
    }
}
