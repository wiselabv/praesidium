package io.github.wiselabv.praesidium.admin.asset.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;

/**
 * 资产仓储（领域层接口，依赖倒置，JPA 实现见 infrastructure）。
 */
public interface AssetRepository {

    Optional<Asset> findById(Long id);

    List<Asset> findByIds(Collection<Long> ids);

    Asset save(Asset asset);

    void delete(Asset asset);

    boolean existsByName(String name);

    boolean existsByNameExcludingId(String name, Long id);

    /** 分页查询（keyword 模糊匹配名称/地址，type 精确过滤），id 倒序 */
    List<Asset> findPage(String keyword, String type, int offset, int limit);

    long countPage(String keyword, String type);

    long countAll();

    /** 按类型统计：返回 [type, count] 行（总览分布图用） */
    List<Object[]> countGroupByType();

    /** 按协议统计：返回 [protocol, count] 行（总览协议分布用） */
    List<Object[]> countGroupByProtocol();
}
