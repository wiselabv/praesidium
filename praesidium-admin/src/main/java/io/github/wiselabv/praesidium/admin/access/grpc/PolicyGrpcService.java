package io.github.wiselabv.praesidium.admin.access.grpc;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.github.wiselabv.praesidium.admin.access.domain.AccessPolicyRepository;
import io.github.wiselabv.praesidium.admin.access.domain.model.AccessPolicy;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 策略流 gRPC 服务实现：Rust 网关连接后先全量兜底（幂等 upsert），
 * 再维持长连接等待增量事件。
 */
@Service
public class PolicyGrpcService extends PolicyServiceGrpc.PolicyServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PolicyGrpcService.class);

    private final PolicyGrpcRegistry registry;
    private final AccessPolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    public PolicyGrpcService(PolicyGrpcRegistry registry,
                             AccessPolicyRepository policyRepository,
                             ObjectMapper objectMapper) {
        this.registry = registry;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void streamPolicies(PolicyProto.StreamRequest request,
                               StreamObserver<PolicyProto.PolicyEvent> responseObserver) {
        long knownVersion = request.getSinceVersion();
        // 长连接维持：先挂 close 回调，再注册；方法返回即断流，必须阻塞到客户端断开
        ServerCallStreamObserver<PolicyProto.PolicyEvent> serverObserver =
                (ServerCallStreamObserver<PolicyProto.PolicyEvent>) responseObserver;
        CountDownLatch closed = new CountDownLatch(1);
        serverObserver.setOnCloseHandler(closed::countDown);
        registry.register(responseObserver);
        try {
            // 全量兜底：网关本地版本落后（含 0 = 首次连接）时重推全部 active 策略
            if (knownVersion < registry.currentVersion() || knownVersion == 0) {
                pushSnapshot(responseObserver);
            }
            // 保持连接：等待客户端断开（onClose 回调触发 latch），期间增量事件由 Registry 广播
            log.info("策略流就绪 sinceVersion={}", knownVersion);
            closed.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("全量兜底失败: {}", ex.getMessage());
            responseObserver.onError(ex);
        } finally {
            registry.unregister(responseObserver);
        }
    }

    /** 全量推送 active 策略（每条约携带自增版本，upsert 幂等） */
    private void pushSnapshot(StreamObserver<PolicyProto.PolicyEvent> responseObserver) {
        List<AccessPolicy> policies = policyRepository.findByStatus("active");
        for (AccessPolicy policy : policies) {
            registry.pushTo(responseObserver, PolicyProto.PolicyEvent.newBuilder()
                    .setAction("upsert")
                    .setPolicy(toProto(policy))
                    .build());
        }
        log.info("策略全量推送完成 count={}", policies.size());
    }

    /** 实体 → proto（日期转 ISO 字符串，与 Rust 端解析约定一致） */
    public PolicyProto.Policy toProto(AccessPolicy policy) {
        PolicyProto.Policy.Builder builder = PolicyProto.Policy.newBuilder()
                .setId(policy.getId())
                .setName(policy.getName() == null ? "" : policy.getName())
                .setUserId(policy.getUserId())
                .setProtocol(policy.getProtocol() == null ? "" : policy.getProtocol())
                .setStatus(policy.getStatus() == null ? "" : policy.getStatus());
        for (Long id : parseIds(policy.getAssetIds())) {
            builder.addAssetIds(id);
        }
        for (Long id : parseIds(policy.getAccountIds())) {
            builder.addAccountIds(id);
        }
        if (policy.getValidFrom() != null) {
            builder.setValidFrom(policy.getValidFrom().atZone(ZoneOffset.UTC).toLocalDate().toString());
        }
        if (policy.getValidTo() != null) {
            builder.setValidTo(policy.getValidTo().atZone(ZoneOffset.UTC).toLocalDate().toString());
        }
        return builder.build();
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
