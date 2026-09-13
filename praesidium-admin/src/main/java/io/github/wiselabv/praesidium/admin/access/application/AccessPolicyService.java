package io.github.wiselabv.praesidium.admin.access.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.access.application.dto.AccessPolicyRequest;
import io.github.wiselabv.praesidium.admin.access.application.dto.PolicyItem;
import io.github.wiselabv.praesidium.admin.access.domain.AccessPolicyRepository;
import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 授权策略应用服务：策略 CRUD + 审批/撤销。
 */
@Service
public class AccessPolicyService {

    private final AccessPolicyRepository policyRepository;
    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AccessPolicyService(AccessPolicyRepository policyRepository,
                               AssetRepository assetRepository,
                               AssetAccountRepository accountRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyItem> list(String keyword, String status, int page, int size) {
        List<AccessPolicy> policies = policyRepository.findPage(keyword, status, (page - 1) * size, size);
        long total = policyRepository.countPage(keyword, status);
        return PageResponse.of(toItems(policies), total, page, size);
    }

    @Transactional
    public PolicyItem create(AccessPolicyRequest request) {
        requireUser(request.userId());
        AccessPolicy policy = AccessPolicy.create(request.name().trim(), request.userId(),
                toJson(request.assetIds()), toJson(request.accountIds()), request.protocol(),
                request.validFrom(), request.validTo(), request.description());
        policyRepository.save(policy);
        return toItem(policy);
    }

    @Transactional
    public PolicyItem update(Long id, AccessPolicyRequest request) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        requireUser(request.userId());
        policy.update(request.name().trim(), request.userId(), toJson(request.assetIds()),
                toJson(request.accountIds()), request.protocol(), request.validFrom(),
                request.validTo(), request.description());
        policyRepository.save(policy);
        return toItem(policy);
    }

    @Transactional
    public void delete(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policyRepository.delete(policy);
    }

    /** 审批通过：pending → active */
    @Transactional
    public PolicyItem approve(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policy.approve();
        policyRepository.save(policy);
        return toItem(policy);
    }

    /** 撤销授权：→ revoked */
    @Transactional
    public PolicyItem revoke(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policy.revoke();
        policyRepository.save(policy);
        return toItem(policy);
    }

    /** 用户存在性校验（策略必须绑定真实用户） */
    private void requireUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.BAD_REQUEST, "授权用户不存在"));
    }

    private List<PolicyItem> toItems(List<AccessPolicy> policies) {
        // 批量取展示名，避免 N+1
        List<Long> userIds = policies.stream().map(AccessPolicy::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
        List<Long> assetIds = policies.stream()
                .flatMap(p -> parseIds(p.getAssetIds()).stream()).distinct().toList();
        Map<Long, String> assetNames = assetRepository.findByIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, Asset::getName));
        List<Long> accountIds = policies.stream()
                .flatMap(p -> parseIds(p.getAccountIds()).stream()).distinct().toList();
        Map<Long, String> accountNames = accountRepository.findByIds(accountIds).stream()
                .collect(Collectors.toMap(AssetAccount::getId, AssetAccount::getName));
        return policies.stream()
                .map(p -> toItem(p, userNames.get(p.getUserId()),
                        assetNames::get, accountNames::get))
                .toList();
    }

    private PolicyItem toItem(AccessPolicy policy) {
        return toItem(policy,
                userRepository.findById(policy.getUserId()).map(User::getDisplayName).orElse(null),
                id -> assetRepository.findById(id).map(Asset::getName).orElse(null),
                id -> accountRepository.findById(id).map(AssetAccount::getName).orElse(null));
    }

    private PolicyItem toItem(AccessPolicy policy, String userName,
                              Function<Long, String> assetName, Function<Long, String> accountName) {
        List<Long> assetIds = parseIds(policy.getAssetIds());
        List<Long> accountIds = parseIds(policy.getAccountIds());
        return PolicyItem.of(policy, userName, assetIds,
                join(assetIds, assetName), accountIds, join(accountIds, accountName));
    }

    private String join(List<Long> ids, Function<Long, String> nameResolver) {
        return ids.stream().map(nameResolver).filter(n -> n != null).collect(Collectors.joining("、"));
    }

    /** JSON 序列化 / 反序列化（JSONB 列以字符串形式存取） */
    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids == null ? List.of() : ids);
        } catch (Exception ex) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "资产/账号范围格式错误");
        }
    }

    private List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
