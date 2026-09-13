package io.github.wiselabv.praesidium.admin.settings.infrastructure;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.settings.domain.SettingRepository;
import io.github.wiselabv.praesidium.admin.settings.domain.model.Setting;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置仓储 JPA 实现（基础设施层）。
 */
@Repository
public class SettingRepositoryJpa implements SettingRepository {

    private final EntityManager entityManager;

    public SettingRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Setting> findBySection(String section) {
        return entityManager.createQuery(
                        "select s from Setting s where s.section = :section", Setting.class)
                .setParameter("section", section)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Setting> findAll() {
        return entityManager.createQuery(
                        "select s from Setting s order by s.section", Setting.class)
                .getResultList();
    }

    @Override
    @Transactional
    public Setting save(Setting setting) {
        if (setting.getId() == null) {
            entityManager.persist(setting);
            return setting;
        }
        return entityManager.merge(setting);
    }
}
