package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.UserThirdParty;

/**
 * 第三方登录绑定仓储（领域层接口）。
 */
public interface UserThirdPartyRepository {

    List<UserThirdParty> findByUserId(Long userId);

    Optional<UserThirdParty> findByUserIdAndProvider(Long userId, String provider);

    /** 某提供商下的第一个绑定（SSO 演示回调按提供商定位用户） */
    Optional<UserThirdParty> findFirstByProvider(String provider);

    UserThirdParty save(UserThirdParty binding);

    void delete(UserThirdParty binding);
}
