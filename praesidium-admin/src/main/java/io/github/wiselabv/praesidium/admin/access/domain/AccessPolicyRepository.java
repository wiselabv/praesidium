package io.github.wiselabv.praesidium.admin.access.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;

/**
 * 授权策略仓储（领域层接口）。
 */
public interface AccessPolicyRepository {

    Optional<AccessPolicy> findById(Long id);

    AccessPolicy save(AccessPolicy policy);

    void delete(AccessPolicy policy);

    /** 分页查询（keyword 模糊匹配名称/描述，status 精确过滤） */
    List<AccessPolicy> findPage(String keyword, String status, int offset, int limit);

    long countPage(String keyword, String status);

    /** 按状态全量查询（策略下发全量兜底用） */
    List<AccessPolicy> findByStatus(String status);

    long countAll();

    long countByStatus(String status);
}
