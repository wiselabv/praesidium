package io.github.wiselabv.praesidium.admin.access.grpc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 策略推送注册表：维护与 Rust 网关的长连接（StreamObserver）与事件版本号。
 *
 * <p>版本号单调递增：连接建立时按当前版本做全量兜底（upsert 幂等），
 * 之后业务变更通过 {@link #broadcast} 增量下发。
 */
@Component
public class PolicyGrpcRegistry {

    private static final Logger log = LoggerFactory.getLogger(PolicyGrpcRegistry.class);

    private final List<StreamObserver<PolicyProto.PolicyEvent>> observers =
            new CopyOnWriteArrayList<>();
    private final AtomicLong version = new AtomicLong(0);

    /** 注册一个网关连接；返回当前版本号（供全量兜底） */
    public long register(StreamObserver<PolicyProto.PolicyEvent> observer) {
        observers.add(observer);
        log.info("Rust 网关策略流接入，当前连接数={}", observers.size());
        return version.get();
    }

    /** 连接结束注销 */
    public void unregister(StreamObserver<PolicyProto.PolicyEvent> observer) {
        observers.remove(observer);
        log.info("Rust 网关策略流断开，当前连接数={}", observers.size());
    }

    /** 广播一条策略变更（版本号自增）；观察者异常自动剔除 */
    public void broadcast(PolicyProto.PolicyEvent event) {
        PolicyProto.PolicyEvent stamped = stamp(event);
        for (StreamObserver<PolicyProto.PolicyEvent> observer : observers) {
            try {
                observer.onNext(stamped);
            } catch (Exception ex) {
                log.warn("策略下发失败，剔除连接: {}", ex.getMessage());
                observers.remove(observer);
            }
        }
    }

    /** 只推给单个连接（全量兜底用，避免重复下发） */
    public void pushTo(StreamObserver<PolicyProto.PolicyEvent> observer, PolicyProto.PolicyEvent event) {
        try {
            observer.onNext(stamp(event));
        } catch (Exception ex) {
            log.warn("策略下发失败，剔除连接: {}", ex.getMessage());
            observers.remove(observer);
        }
    }

    /** 版本号自增并回填版本字段 */
    private PolicyProto.PolicyEvent stamp(PolicyProto.PolicyEvent event) {
        return event.toBuilder().setVersion(version.incrementAndGet()).build();
    }

    /** 当前版本号（全量兜底后同步给网关） */
    public long currentVersion() {
        return version.get();
    }
}
