package io.github.wiselabv.praesidium.admin.access.grpc;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 策略下发 gRPC 服务器：监听网关连接，随应用启停。
 *
 * <p>开关 {@code praesidium.grpc.enabled}（默认开启）；本地开发/测试可通过
 * 环境变量 {@code PRAESIDIUM_GRPC_ENABLED=false} 关闭。
 */
@Component
@ConditionalOnProperty(name = "praesidium.grpc.enabled", havingValue = "true", matchIfMissing = true)
public class PolicyGrpcServer {

    private static final Logger log = LoggerFactory.getLogger(PolicyGrpcServer.class);

    private final int port;
    private final PolicyGrpcService policyService;
    private Server server;

    public PolicyGrpcServer(@Value("${praesidium.grpc.port:9090}") int port,
                            PolicyGrpcService policyService) {
        this.port = port;
        this.policyService = policyService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = NettyServerBuilder.forPort(port)
                .addService(policyService)
                .build()
                .start();
        log.info("策略下发 gRPC 服务器已启动 port={}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
