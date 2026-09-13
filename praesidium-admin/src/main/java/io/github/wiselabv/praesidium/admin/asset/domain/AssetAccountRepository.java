package io.github.wiselabv.praesidium.admin.asset.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;

/**
 * 资产账号仓储（领域层接口）。
 */
public interface AssetAccountRepository {

    Optional<AssetAccount> findById(Long id);

    List<AssetAccount> findByIds(Collection<Long> ids);

    AssetAccount save(AssetAccount account);

    void delete(AssetAccount account);

    List<AssetAccount> findByAssetId(Long assetId);

    /** 全局账号分页（keyword 模糊匹配名称） */
    List<AssetAccount> findPage(String keyword, int offset, int limit);

    long countPage(String keyword);

    boolean existsByAssetAndName(Long assetId, String name);

    boolean existsByAssetAndNameExcludingId(Long assetId, String name, Long id);

    /** 按资产统计账号数：assetId -> count（仅统计入参范围内的资产） */
    Map<Long, Long> countGroupByAsset(Collection<Long> assetIds);

    /** 按凭据统计引用数：credentialId -> count */
    Map<Long, Long> countGroupByCredential(Collection<Long> credentialIds);

    long countByCredentialId(Long credentialId);

    long countAll();
}
