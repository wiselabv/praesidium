package io.github.wiselabv.praesidium.admin.asset.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产仓储 JPA 实现（基础设施层，实现领域层接口——依赖倒置）。
 *
 * <p>与身份上下文一致：基于 EntityManager 手写持久化逻辑，查询语义显式可见。
 */
@Repository
public class AssetRepositoryJpa implements AssetRepository {

    private final EntityManager entityManager;

    public AssetRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Asset.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("select a from Asset a where a.id in :ids", Asset.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    @Transactional
    public Asset save(Asset asset) {
        if (asset.getId() == null) {
            entityManager.persist(asset);
            return asset;
        }
        return entityManager.merge(asset);
    }

    @Override
    @Transactional
    public void delete(Asset asset) {
        entityManager.remove(entityManager.contains(asset) ? asset : entityManager.merge(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return count("select count(a) from Asset a where a.name = :name",
                q -> q.setParameter("name", name)) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameExcludingId(String name, Long id) {
        return count("select count(a) from Asset a where a.name = :name and a.id <> :id",
                q -> q.setParameter("name", name).setParameter("id", id)) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findPage(String keyword, String type, int offset, int limit) {
        QuerySpec spec = pageQuery(keyword, type, "select a from Asset a");
        TypedQuery<Asset> query = entityManager.createQuery(spec.jpql, Asset.class);
        spec.bind(query);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String keyword, String type) {
        QuerySpec spec = pageQuery(keyword, type, "select count(a) from Asset a");
        TypedQuery<Long> query = entityManager.createQuery(spec.jpql, Long.class);
        spec.bind(query);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return entityManager.createQuery("select count(a) from Asset a", Long.class).getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countGroupByType() {
        return entityManager.createQuery(
                "select a.type, count(a) from Asset a group by a.type order by count(a) desc", Object[].class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countGroupByProtocol() {
        return entityManager.createQuery(
                "select a.protocol, count(a) from Asset a group by a.protocol order by count(a) desc", Object[].class)
                .getResultList();
    }

    /** 分页查询共用构造：keyword 模糊匹配名称/地址，type 精确过滤 */
    private QuerySpec pageQuery(String keyword, String type, String selectClause) {
        StringBuilder jpql = new StringBuilder(selectClause).append(" where 1=1");
        QuerySpec spec = new QuerySpec();
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" and (lower(a.name) like :kw or lower(a.address) like :kw)");
            spec.keyword = "%" + keyword.trim().toLowerCase() + "%";
        }
        if (type != null && !type.isBlank()) {
            jpql.append(" and a.type = :type");
            spec.type = type;
        }
        spec.jpql = jpql.toString();
        return spec;
    }

    private long count(String jpql, java.util.function.Consumer<TypedQuery<Long>> binder) {
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        binder.accept(query);
        return query.getSingleResult();
    }

    /** 动态查询参数容器（jpql + 可选参数） */
    private static final class QuerySpec {
        private String jpql;
        private String keyword;
        private String type;

        private void bind(TypedQuery<?> query) {
            if (keyword != null) {
                query.setParameter("kw", keyword);
            }
            if (type != null) {
                query.setParameter("type", type);
            }
        }
    }
}
