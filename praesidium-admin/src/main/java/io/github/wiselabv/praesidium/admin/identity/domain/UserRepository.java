package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.User;

/**
 * 用户仓储接口（领域层定义，基础设施层实现——依赖倒置）。
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    List<User> findByIds(Collection<Long> ids);

    Optional<User> findByUsername(String username);

    User save(User user);

    boolean existsByUsername(String username);

    long countAll();

    /** 全量用户（id 升序）：授权策略表单的用户下拉数据源 */
    List<User> findAllSimple();
}
