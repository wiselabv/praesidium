package io.github.wiselabv.praesidium.admin.identity.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.identity.domain.model.RecoveryCode;

/**
 * MFA 恢复码仓储（领域层接口）。
 */
public interface RecoveryCodeRepository {

    List<RecoveryCode> findByUserId(Long userId);

    /** 用户未使用的恢复码（按创建时间倒序） */
    List<RecoveryCode> findUnusedByUserId(Long userId);

    Optional<RecoveryCode> findUnusedByUserIdAndHash(Long userId, String codeHash);

    long countUnusedByUserId(Long userId);

    RecoveryCode save(RecoveryCode code);

    void deleteAll(List<RecoveryCode> codes);
}
