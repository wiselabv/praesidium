package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.UserProfile;

/**
 * 个人信息仓储（领域层接口）。
 */
public interface UserProfileRepository {

    Optional<UserProfile> findByUserId(Long userId);

    UserProfile save(UserProfile profile);
}
