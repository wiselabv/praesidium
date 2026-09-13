package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.RefreshToken;

/**
 * 刷新令牌仓储接口（领域层定义，基础设施层实现——依赖倒置）。
 */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);
}
