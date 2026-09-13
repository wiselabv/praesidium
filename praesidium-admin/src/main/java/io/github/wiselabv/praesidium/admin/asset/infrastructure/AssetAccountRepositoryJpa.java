package io.github.wiselabv.praesidium.admin.asset.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产账号仓储 JPA 实现（基础设施层）。
 */
@Repository
public class AssetAccountRepositoryJpa implements AssetAccountRepository {

    private final EntityManager entityManager;

    public AssetAccountRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetAccount> findById(Long id) {
        return Optional.ofNullable(entityManager.find(AssetAccount.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAccount> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("select a from AssetAccount a where a.id in :ids", AssetAccount.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    @Transactional
    public AssetAccount save(AssetAccount account) {
        if (account.getId() == null) {
            entityManager.persist(account);
            return account;
        }
        return entityManager.merge(account);
    }

    @Override
    @Transactional
    public void delete(AssetAccount account) {
        entityManager.remove(entityManager.contains(account) ? account : entityManager.merge(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAccount> findByAssetId(Long assetId) {
        return entityManager.createQuery(
                        "select a from AssetAccount a where a.assetId = :assetId order by a.id", AssetAccount.class)
                .setParameter("assetId", assetId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAccount> findPage(String keyword, int offset, int limit) {
        TypedQuery<AssetAccount> query = entityManager.createQuery(
                "select a from AssetAccount a where (:kw is null or lower(a.name) like :kw) order by a.id desc",
                AssetAccount.class);
        bindKeyword(query, keyword);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(a) from AssetAccount a where (:kw is null or lower(a.name) like :kw)", Long.class);
        bindKeyword(query, keyword);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAssetAndName(Long assetId, String name) {
        return countByName(assetId, name, null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAssetAndNameExcludingId(Long assetId, String name, Long id) {
        return countByName(assetId, name, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countGroupByAsset(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery(
                        "select a.assetId, count(a) from AssetAccount a where a.assetId in :ids group by a.assetId",
                        Object[].class)
                .setParameter("ids", assetIds)
                .getResultStream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countGroupByCredential(Collection<Long> credentialIds) {
        if (credentialIds == null || credentialIds.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery(
                        "select a.credentialId, count(a) from AssetAccount a where a.credentialId in :ids group by a.credentialId",
                        Object[].class)
                .setParameter("ids", credentialIds)
                .getResultStream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCredentialId(Long credentialId) {
        return entityManager.createQuery(
                        "select count(a) from AssetAccount a where a.credentialId = :credentialId", Long.class)
                .setParameter("credentialId", credentialId)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return entityManager.createQuery("select count(a) from AssetAccount a", Long.class).getSingleResult();
    }

    private boolean countByName(Long assetId, String name, Long excludingId) {
        String jpql = excludingId == null
                ? "select count(a) from AssetAccount a where a.assetId = :assetId and a.name = :name"
                : "select count(a) from AssetAccount a where a.assetId = :assetId and a.name = :name and a.id <> :id";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("assetId", assetId)
                .setParameter("name", name);
        if (excludingId != null) {
            query.setParameter("id", excludingId);
        }
        return query.getSingleResult() > 0;
    }

    private void bindKeyword(TypedQuery<?> query, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            query.setParameter("kw", null);
        } else {
            query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
        }
    }
}
