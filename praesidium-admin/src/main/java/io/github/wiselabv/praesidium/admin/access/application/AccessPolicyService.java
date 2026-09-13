package io.github.wiselabv.praesidium.admin.access.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.access.application.dto.AccessPolicyRequest;
import io.github.wiselabv.praesidium.admin.access.application.dto.PolicyItem;
import io.github.wiselabv.praesidium.admin.access.domain.AccessPolicyRepository;
import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;
import io.github.wiselabv.praesidium.admin.access.grpc.PolicyGrpcRegistry;
import io.github.wiselabv.praesidium.admin.access.grpc.PolicyGrpcService;
import io.github.wiselabv.praesidium.admin.access.grpc.PolicyProto;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 授权策略应用服务：策略 CRUD + 审批/撤销。
 *
 * <p>策略变更（生效范围变化）在事务提交后经 {@link PolicyGrpcRegistry} 广播给
 * Rust 网关，保证内存策略表与数据库最终一致。
 */
@Service
public class AccessPolicyService {

    /**
     *
     */
    private final AccessPolicyRepository policyRepository;
    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PolicyGrpcRegistry policyRegistry;
    private final PolicyGrpcService policyGrpcService;

    public AccessPolicyService(AccessPolicyRepository policyRepository,
                               AssetRepository assetRepository,
                               AssetAccountRepository accountRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper,
                               PolicyGrpcRegistry policyRegistry,
                               PolicyGrpcService policyGrpcService) {
        this.policyRepository = policyRepository;
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.policyRegistry = policyRegistry;
        this.policyGrpcService = policyGrpcService;
    }

    /**
     * 分页
     * @param keyword 关键字
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 授权策略分页列表
     */
    @Transactional(readOnly = true)
    public PageResponse<PolicyItem> list(String keyword, String status, int page, int size) {
        List<AccessPolicy> policies = policyRepository.findPage(keyword, status, (page - 1) * size, size);
        long total = policyRepository.countPage(keyword, status);
        return PageResponse.of(toItems(policies), total, page, size);
    }

    /**
     * 创建授权策略
     * @param request
     * @return
     */
    @Transactional
    public PolicyItem create(AccessPolicyRequest request) {
        requireUser(request.userId());
        AccessPolicy policy = AccessPolicy.create(request.name().trim(), request.userId(),
                toJson(request.assetIds()), toJson(request.accountIds()), request.protocol(),
                request.validFrom(), request.validTo(), request.description());
        policyRepository.save(policy);
        // pending 状态不推送（Rust 只认 active），审批通过时再广播
        return toItem(policy);
    }

    /**
     * 更新授权策略
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public PolicyItem update(Long id, AccessPolicyRequest request) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        requireUser(request.userId());
        policy.update(request.name().trim(), request.userId(), toJson(request.assetIds()),
                toJson(request.accountIds()), request.protocol(), request.validFrom(),
                request.validTo(), request.description());
        policyRepository.save(policy);
        afterCommit(() -> policyRegistry.broadcast(upsertEvent(policy)));
        return toItem(policy);
    }

    /**
     * 删除授权策略
     * @param id
     */
    @Transactional
    public void delete(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policyRepository.delete(policy);
        afterCommit(() -> policyRegistry.broadcast(PolicyProto.PolicyEvent.newBuilder()
                .setAction("remove")
                .setPolicy(policyGrpcService.toProto(policy))
                .build()));
    }

    /**
     * 审批通过授权策略
     * @param id
     * @return
     */
    /** 审批通过：pending → active */
    @Transactional
    public PolicyItem approve(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policy.approve();
        policyRepository.save(policy);
        afterCommit(() -> policyRegistry.broadcast(upsertEvent(policy)));
        return toItem(policy);
    }

    /**
     * 撤销授权策略
     * @param id
     * @return
     */
    /** 撤销授权：→ revoked */
    @Transactional
    public PolicyItem revoke(Long id) {
        AccessPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.POLICY_NOT_FOUND, "授权策略不存在"));
        policy.revoke();
        policyRepository.save(policy);
        // revoked 也推 upsert：Rust 端策略状态翻转，立即拒绝后续建连
        afterCommit(() -> policyRegistry.broadcast(upsertEvent(policy)));
        return toItem(policy);
    }

    /** upsert 事件（策略整体替换） */
    private PolicyProto.PolicyEvent upsertEvent(AccessPolicy policy) {
        return PolicyProto.PolicyEvent.newBuilder()
                .setAction("upsert")
                .setPolicy(policyGrpcService.toProto(policy))
                .build();
    }

    /** 事务提交成功后执行（回滚则静默跳过，避免下发脏策略） */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * 用户存在性校验（策略必须绑定真实用户）
     * @param userId
     */
    /** 用户存在性校验（策略必须绑定真实用户） */
    private void requireUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.BAD_REQUEST, "授权用户不存在"));
    }

    /**
     * 转换授权策略列表
     * @param policies
     * @return
     */
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

    /**
     * 转换授权策略
     * @param policy
     * @return
     */
    private PolicyItem toItem(AccessPolicy policy) {
        return toItem(policy,
                userRepository.findById(policy.getUserId()).map(User::getDisplayName).orElse(null),
                id -> assetRepository.findById(id).map(Asset::getName).orElse(null),
                id -> accountRepository.findById(id).map(AssetAccount::getName).orElse(null));
    }

    /**
     * 转换授权策略
     * @param policy
     * @param userName
     * @param assetName
     * @param accountName
     * @return
     */
    private PolicyItem toItem(AccessPolicy policy, String userName,
                              Function<Long, String> assetName, Function<Long, String> accountName) {
        List<Long> assetIds = parseIds(policy.getAssetIds());
        List<Long> accountIds = parseIds(policy.getAccountIds());
        return PolicyItem.of(policy, userName, assetIds,
                join(assetIds, assetName), accountIds, join(accountIds, accountName));
    }

    /**
     * 按名称连接
     * @param ids
     * @param nameResolver
     * @return
     */
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
