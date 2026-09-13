package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.UserSecuritySettings;

/**
 * 用户 MFA 补充设置仓储（领域层接口）。
 */
public interface UserSecuritySettingsRepository {

    Optional<UserSecuritySettings> findByUserId(Long userId);

    UserSecuritySettings save(UserSecuritySettings settings);
}
