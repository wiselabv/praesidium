package io.github.wiselabv.praesidium.admin.audit.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.audit.application.dto.OperationLogItem;
import io.github.wiselabv.praesidium.admin.audit.domain.AuditLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.AuditLog;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作审计应用服务：日志分页查询（含最近动态，供总览使用）。
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        UserRepository userRepository,
                        AssetRepository assetRepository,
                        AssetAccountRepository accountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OperationLogItem> list(String keyword, String risk, String result,
                                               int page, int size) {
        List<AuditLog> logs = auditLogRepository.findPage(keyword, risk, result, (page - 1) * size, size);
        long total = auditLogRepository.countPage(keyword, risk, result);
        return PageResponse.of(toItems(logs), total, page, size);
    }

    /** 最近 N 条操作（总览页「最近动态」） */
    @Transactional(readOnly = true)
    public List<OperationLogItem> recent(int limit) {
        return toItems(auditLogRepository.findRecent(limit));
    }

    private List<OperationLogItem> toItems(List<AuditLog> logs) {
        List<Long> userIds = logs.stream().map(AuditLog::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        List<Long> assetIds = logs.stream().map(AuditLog::getAssetId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, String> assetNames = assetRepository.findByIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, Asset::getName));
        List<Long> accountIds = logs.stream().map(AuditLog::getAccountId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, String> accountNames = accountRepository.findByIds(accountIds).stream()
                .collect(Collectors.toMap(AssetAccount::getId, AssetAccount::getName));
        return logs.stream()
                .map(l -> OperationLogItem.of(l, userNames.get(l.getUserId()),
                        assetNames.get(l.getAssetId()), accountNames.get(l.getAccountId())))
                .toList();
    }
}
