package io.github.wiselabv.praesidium.admin.asset.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.asset.domain.model.Credential;

/**
 * 凭据仓储（领域层接口）。
 */
public interface CredentialRepository {

    Optional<Credential> findById(Long id);

    List<Credential> findByIds(Collection<Long> ids);

    Credential save(Credential credential);

    void delete(Credential credential);

    /** 分页查询（keyword 模糊匹配名称） */
    List<Credential> findPage(String keyword, int offset, int limit);

    long countPage(String keyword);

    long countAll();
}
